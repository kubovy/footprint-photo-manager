package com.poterion.footprint.manager.utils

import com.poterion.footprint.manager.Main
import com.poterion.footprint.manager.data.*
import org.flywaydb.core.Flyway
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.SessionFactoryObserver
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.service.ServiceRegistry
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Database {
	private val LOGGER = LoggerFactory.getLogger(Database::class.java)
	private val dbFile: String = Main.CONFIG_PATH.resolve("storage").toString()
	private val sessionFactory: SessionFactory
	private val serviceRegistry: ServiceRegistry
	private val types: Collection<KClass<out BaseItem>> = listOf(
			Device::class, MediaItem::class, MetadataTag::class, Notification::class, Setting::class)
	private val cache: MutableMap<KClass<*>, MutableMap<String, CacheableItem>> = mutableMapOf()
	private val connectionString = "jdbc:h2:${dbFile}"
	private val onSaveListeners = mutableListOf<(BaseItem) -> Unit>()
	private val lock = ReentrantReadWriteLock()

	private val deviceMediaItemCache = mutableMapOf<String, Collection<MediaItem>>()
	private val mediaItemRelevantMetadataCache = mutableMapOf<String, Collection<MetadataTag>>()

	init {
		try {
			Class.forName("org.h2.Driver")
			Flyway.configure()
				.dataSource(connectionString, "sa", null)
				.baselineVersion("4")
				.baselineOnMigrate(true)
				.load()
				//.baseline()
				.migrate()

			val config = getConfiguration()
			serviceRegistry = StandardServiceRegistryBuilder().applySettings(config.properties).build()
			config.sessionFactoryObserver = object : SessionFactoryObserver {
				override fun sessionFactoryCreated(factory: SessionFactory) {
				}

				override fun sessionFactoryClosed(factory: SessionFactory) {
					StandardServiceRegistryBuilder.destroy(serviceRegistry)
				}
			}
			sessionFactory = config.buildSessionFactory(serviceRegistry)

			lock.read {
				types.filter { it.isCacheable() }.mapNotNull { it as? KClass<CacheableItem> }.forEach { type ->
					cache[type] = listFromDB(type).map { it.id!! to it }.toMap().toMutableMap()
				}
			}
//			lock.write {
//				for (mediaItem in list(MediaItem::class)) {
//					val irrelevant = mediaItem.metadata.filterNot { it.isRelevant() }
//					deleteAll(irrelevant)
//				}
//			}
		} catch (t: Throwable) {
			LOGGER.error("Initial SessionFactory creation failed: ${t}", t)
			throw ExceptionInInitializerError(t)
		}
	}

	private fun openSession(): Session {
		return sessionFactory.withOptions()
			.jdbcTimeZone(TimeZone.getTimeZone("UTC"))
			.openSession()
	}

	private fun <T : BaseItem> KClass<T>.isCacheable() = CacheableItem::class.java.isAssignableFrom(java)

	fun <T : CacheableItem> list(type: KClass<T>, includingDeleted: Boolean = false): Collection<T> = lock.read {
		(cache[type]?.values as? Collection<T> ?: listFromDB(type))
			.filter { includingDeleted || it !is MediaItem || it.deletedAt == null }
	}

	private fun <T : BaseItem> listFromDB(type: KClass<T>): Collection<T> = openSession().use { session ->
		val builder = session.criteriaBuilder
		val criteria = builder.createQuery<T>(type.java)
		criteria.from(type.java)
		return session.createQuery(criteria).resultList
	}

	fun <T : BaseItem> get(type: KClass<T>, id: String): T? = lock.read {
		cache[type]?.get(id) as? T ?: getFromDB(type, id)
	}

	private fun <T : BaseItem> getFromDB(type: KClass<T>, id: String): T? = openSession().use { session ->
		session.get(type.java, id)
			?.let { it as? T }
			?.also { session.evict(it) }
	}

	fun <T : BaseItem> find(type: KClass<T>,
							restrictions: (CriteriaBuilder, Root<T>) -> Collection<Predicate>): Collection<T> =
		openSession().use { session ->
			val builder = session.criteriaBuilder
			val criteria = builder.createQuery<T>(type.java)
			val root = criteria.from(type.java) as Root<T>
			criteria.where(*restrictions(builder, root).toTypedArray())
			return session.createQuery(criteria).resultList
		}

	fun <T : BaseItem> save(entity: T) = openSession().use { session ->
		session.beginTransaction()
		var success = false
		try {
			//LOGGER.info("Saving: ${entity}")
			session.saveOrUpdate(entity)
			success = true
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}
		session.transaction.commit()
		if (success) {
			if (entity is CacheableItem) {
				lock.write { cache.getOrPut(entity::class) { mutableMapOf() }[entity.id!!] = entity }
			}
			onSaveListeners.forEach { it(entity) }
		}
	}

	fun <T : BaseItem> saveAll(entities: Iterable<T>) = openSession().use { session ->
		session.beginTransaction()
		val successful = mutableListOf<T>()
		for (entity in entities) try {
			//LOGGER.info("Saving: ${entity}")
			session.saveOrUpdate(entity)
			successful.add(entity)
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}
		session.transaction.commit()
		for (entity in successful) {
			if (entity is CacheableItem) {
				lock.write { cache.getOrPut(entity::class) { mutableMapOf() }[entity.id!!] = entity }
			}
			onSaveListeners.forEach { it(entity) }
		}
	}

	fun <T : BaseItem> delete(entity: T) = openSession().use { session ->
		if (entity is MediaItem) entity.removeCache()
		session.beginTransaction()
		try {
			session.deleteInternal(entity)
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}
		session.transaction.commit()
	}

	fun <T : BaseItem> deleteAll(entities: Iterable<T>) = openSession().use { session ->
		entities.filterIsInstance<MediaItem>().forEach { it.removeCache() }
		session.beginTransaction()
		try {
			entities.forEach { session.deleteInternal(it) }
		} catch (t: Throwable) {
			LOGGER.error(t.message, t)
		}
		session.transaction.commit()
	}

	private fun <T : BaseItem> Session.deleteInternal(entity: T) {
		if (entity is Device) entity.mediaItems.forEach { deleteInternal(it) }
		if (entity is MediaItem) entity.metadata.forEach { deleteInternal(it) }
		if (entity is UriItem) lock.read {
			list(Setting::class)
				.filter { it.name == Setting.EXPANDED && it.value == entity.uri }
				.forEach { deleteInternal(it) }
		}
		remove(entity)
		if (entity is CacheableItem) lock.write { cache[entity::class]?.remove(entity.id) }
	}

	fun addOnSaveListener(listener: (BaseItem) -> Unit) {
		onSaveListeners.add(listener)
	}

	fun removeOnSaveListener(listener: (BaseItem) -> Unit) {
		onSaveListeners.remove(listener)
	}

	//private fun getDevicePhotosFromDB(device: Device): Collection<MediaItem> = openSession().use { session ->
	//	val builder = session.criteriaBuilder
	//	val criteria = builder.createQuery<MediaItem>(MediaItem::class.java)
	//	val root = criteria.from(MediaItem::class.java)!!
	//	val deviceColumn: Path<MediaItem> = root.get("device")
	//	criteria.where(builder.equal(deviceColumn, this))
	//	return session.createQuery(criteria).resultList
	//}

	//private fun getPhotoMetadataFromDB(photo: MediaItem): Collection<MetadataTag> = openSession().use { session ->
	//	val builder = session.criteriaBuilder
	//	val criteria = builder.createQuery<MetadataTag>(MetadataTag::class.java)
	//	val root = criteria.from(MetadataTag::class.java)!!
	//	val photoColumn: Path<MetadataTag> = root.get("photo")
	//	criteria.where(builder.equal(photoColumn, this))
	//	return session.createQuery(criteria).resultList
	//}

	private fun getConfiguration() = Configuration().apply {
		File(dbFile).parentFile.takeIf { !it.exists() }?.mkdirs()
		types.forEach { addAnnotatedClass(it.java) }
		setProperty("hibernate.connection.driver_class", "org.h2.Driver")
		setProperty("hibernate.connection.url", connectionString)
		setProperty("hibernate.connection.username", "sa")
		setProperty("hibernate.connection.password", "")
		setProperty("hibernate.show_sql", "false")
		setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
		setProperty("hibernate.hbm2ddl.auto", "")
		setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false")
		setProperty("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider")
		setProperty("hibernate.current_session_context_class", "thread")
	}
}