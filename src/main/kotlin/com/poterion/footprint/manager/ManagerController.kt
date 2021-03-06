/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.footprint.manager

import com.poterion.footprint.manager.data.Device
import com.poterion.footprint.manager.data.MediaItem
import com.poterion.footprint.manager.data.MetadataTag
import com.poterion.footprint.manager.data.Notification
import com.poterion.footprint.manager.data.Setting
import com.poterion.footprint.manager.data.UriItem
import com.poterion.footprint.manager.enums.DeviceType
import com.poterion.footprint.manager.model.MultiProgress
import com.poterion.footprint.manager.model.Progress
import com.poterion.footprint.manager.model.ThumbnailBin
import com.poterion.footprint.manager.model.VirtualItem
import com.poterion.footprint.manager.ui.AddSambaShareDialog
import com.poterion.footprint.manager.ui.ProgressDialog
import com.poterion.footprint.manager.ui.SettingsController
import com.poterion.footprint.manager.ui.helper.MetadataExtractorHelper
import com.poterion.footprint.manager.ui.helper.PreviewLoaderHelper
import com.poterion.footprint.manager.utils.Database
import com.poterion.footprint.manager.utils.Notifications
import com.poterion.footprint.manager.utils.addAll
import com.poterion.footprint.manager.utils.dataTreeComparator
import com.poterion.footprint.manager.utils.device
import com.poterion.footprint.manager.utils.displayName
import com.poterion.footprint.manager.utils.formattedLocation
import com.poterion.footprint.manager.utils.formattedResolution
import com.poterion.footprint.manager.utils.icon
import com.poterion.footprint.manager.utils.metadata
import com.poterion.footprint.manager.utils.toIcon
import com.poterion.footprint.manager.utils.toUriOrNull
import com.poterion.footprint.manager.workers.DuplicateScanner
import com.poterion.footprint.manager.workers.GenerateTreeWorker
import com.poterion.footprint.manager.workers.ImageLoader
import com.poterion.footprint.manager.workers.ScanWorker
import com.poterion.utils.javafx.cell
import com.poterion.utils.javafx.expandTree
import com.poterion.utils.javafx.find
import com.poterion.utils.javafx.findAll
import com.poterion.utils.javafx.toImage
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.intermediate
import com.poterion.utils.kotlin.toFormattedDuration
import com.poterion.utils.kotlin.uriDecode
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.ProgressBar
import javafx.scene.control.ScrollPane
import javafx.scene.control.SelectionMode
import javafx.scene.control.Slider
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.Duration
import jcifs.smb.SmbFile
import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager
import net.samuelcampos.usbdrivedetector.USBStorageDevice
import net.samuelcampos.usbdrivedetector.events.DeviceEventType
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ManagerController {

	companion object {
		fun get(stage: Stage, initialScan: Boolean): Parent {
			val fxmlLoader = FXMLLoader()
			val root =
					fxmlLoader.load<Pane>(ManagerController::class.java.getResource("/com/poterion/footprint/manager/main.fxml").openStream())
			val controller = fxmlLoader.getController<ManagerController>() as ManagerController
			controller.stage = stage
			controller.start(initialScan)
			return root
		}
	}

	@FXML private lateinit var buttonAddFolder: Button
	@FXML private lateinit var buttonAddSharedFolder: Button
	@FXML private lateinit var buttonSynchronize: Button
	@FXML private lateinit var buttonFindDuplicates: Button
	@FXML private lateinit var buttonInclude: Button
	@FXML private lateinit var buttonExclude: Button
	@FXML private lateinit var buttonRemove: Button
	@FXML private lateinit var buttonSettings: Button

	@FXML private lateinit var tableData: TreeTableView<UriItem>
	@FXML private lateinit var columnDataName: TreeTableColumn<UriItem, String>
	@FXML private lateinit var columnDataDates: TreeTableColumn<UriItem, Void>
	@FXML private lateinit var columnDataCreationDate: TreeTableColumn<UriItem, Instant>
	@FXML private lateinit var columnDataModificationDate: TreeTableColumn<UriItem, Instant>
	@FXML private lateinit var columnDataResolution: TreeTableColumn<UriItem, String>
	@FXML private lateinit var columnDataLocation: TreeTableColumn<UriItem, String>

	@FXML private lateinit var tabPane: TabPane
	@FXML private lateinit var tabThumbnails: Tab
	@FXML private lateinit var tabMap: Tab
	@FXML private lateinit var tabFaces: Tab
	@FXML private lateinit var tabPhoto: Tab
	@FXML private lateinit var tabNotifications: Tab

	@FXML private lateinit var scrollPaneThumbnails: ScrollPane
	@FXML private lateinit var vboxThumbnails: VBox
	@FXML private lateinit var labelThumbnailSize: Label
	@FXML private lateinit var sliderThumbnailSize: Slider

	@FXML private lateinit var stackPanePreview: StackPane
	@FXML private lateinit var mediaView: MediaView
	@FXML private lateinit var imageView: ImageView
	@FXML private lateinit var hboxPreviewControls: HBox
	@FXML private lateinit var buttonPreviewPlayPause: Button
	@FXML private lateinit var sliderPreviewPosition: Slider
	@FXML private lateinit var labelPreviewStart: Label
	@FXML private lateinit var labelPreviewCurrent: Label
	@FXML private lateinit var labelPreviewEnd: Label

	@FXML private lateinit var tableMetadata: TreeTableView<MetadataTag>
	@FXML private lateinit var columnMetadataName: TreeTableColumn<MetadataTag, String>
	@FXML private lateinit var columnMetadataTagType: TreeTableColumn<MetadataTag, Int>
	@FXML private lateinit var columnMetadataValue: TreeTableColumn<MetadataTag, String>
	@FXML private lateinit var columnMetadataDescription: TreeTableColumn<MetadataTag, String>
	@FXML private lateinit var columnMetadataValueType: TreeTableColumn<MetadataTag, String>
	@FXML private lateinit var progressbar: ProgressBar
	@FXML private lateinit var labelStatus: Label

	@FXML private lateinit var treeViewNotifications: TreeView<Notification>

	private lateinit var stage: Stage

	private val driveDetector = USBDeviceDetectorManager()
	private val directoryChooser = DirectoryChooser()
	private val uiUpdateExecutor = Executors.newFixedThreadPool(5)
	private var scanExecutor = Executors.newFixedThreadPool(5)
	private var treeUpdateExecutor = Executors.newSingleThreadExecutor()
	private var thumbnailLoaderExecutor: ExecutorService? = null
	private lateinit var multiProgress: MultiProgress

	private var progressDialog: ProgressDialog? = null
	private var scansInProgress = mutableMapOf<String, Boolean>()

	private var mediaPlayer: MediaPlayer? = null

	private val selectedMediaProperty = SimpleObjectProperty<MediaItem>()
	private val selectedThumbnails = FXCollections.observableSet<MediaItem>()

	private val loadingImage = ManagerController::class.java
		.getResourceAsStream("/com/poterion/footprint/manager/images/loading.jpg")
		.toImage()

	private lateinit var previewLoaderHelper: PreviewLoaderHelper

	@FXML
	fun initialize() {
		multiProgress = MultiProgress(progressbar)
		previewLoaderHelper = PreviewLoaderHelper(stackPanePreview, mediaView, imageView, hboxPreviewControls,
												  buttonPreviewPlayPause, sliderPreviewPosition, labelPreviewStart,
												  labelPreviewCurrent, labelPreviewEnd)
		buttonRemove.isDisable = true
		buttonFindDuplicates.isDisable = Database.list(Device::class).none { it.isPrimary }

		tableData.root = TreeItem(VirtualItem.ROOT)
		tableData.selectionModel.selectionMode = SelectionMode.MULTIPLE
		tableData.selectionModel.selectedItemProperty().addListener { _, _, selected ->
			buttonRemove.isDisable = tableData.selectionModel.selectedItems
				.map { it?.value }
				.filterIsInstance<Device>()
				.isEmpty()


			when (selected?.value) {
				is Device -> {
					//selectedMediaProperty.set(null)
					showThumbnails(selected)
				}
				is VirtualItem -> {
					//selectedMediaProperty.set(null)
					showThumbnails(selected)
				}
				is MediaItem -> {
					//showThumbnails(null)
					selected.value
						.let { it as? MediaItem }
						?.takeIf { it.id != selectedMediaProperty.get()?.id }
						?.also { selectedMediaProperty.set(it) }
				}
				null -> selectedMediaProperty.set(null)
			}
		}

//		val columnDataNameWidth = Database.list(Setting::class).find { it.name == Setting.COLUMN_DATA_NAME_WIDTH }
//			?: Setting(name = Setting.COLUMN_DATA_NAME_WIDTH, value = "${100.0}")
//		columnDataName.prefWidth = columnDataNameWidth.value?.toDoubleOrNull() ?: 100.0
//		columnDataName.widthProperty().addListener { _, _, v -> Database.save(columnDataNameWidth.apply { value = "${v}" }) }
		columnDataName.cell("name") { item, value, empty ->
			text = value?.takeUnless { empty }?.removeSuffix("/")?.uriDecode()
			graphic = item?.takeUnless { empty }?.icon()?.toImageView()
//			graphic = StackPane().takeUnless { empty }?.also { stackPane ->
//				minWidth = 16.0
//				minHeight = 16.0
//				prefWidth = 16.0
//				prefHeight = 16.0
//				maxWidth = 16.0
//				maxHeight = 16.0
//				item?.takeUnless { empty }?.icon()?.toImageView()?.also { stackPane.children.add(it) }
//				if (item is Device && item.isPrimary) Icons.STAR
//					?.takeUnless { empty }
//					?.toImageView(8, 8)
//					?.also { StackPane.setAlignment(it, Pos.TOP_LEFT) }
//					?.also { stackPane.children.add(it) }
//
//				when {
//					item is Device && !item.isAvailable -> Icons.UNAVAILABLE
////					item is Device -> item.mediaItems.flatMap { it.problems }.toSet().minBy { it.ordinal }?.icon
////					item is VirtualItem -> item.mediaItems.flatMap { it.problems }.toSet().minBy { it.ordinal }?.icon
////					item is MediaItem -> item.problems.minBy { it.ordinal }?.icon
//					else -> null
//				}?.takeUnless { empty }
//					?.toImageView(8, 8)
//					?.also { StackPane.setAlignment(it, Pos.BOTTOM_RIGHT) }
//					?.also { stackPane.children.add(it) }
//			}

//			tooltip = item.takeUnless { empty }?.getFormattedProblemMessage()?.let { Tooltip(it) }

			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnDataCreationDate.cell { param -> param?.value?.value?.let { it as? MediaItem }?.createdAt }
		columnDataModificationDate.cell { param -> param?.value?.value?.let { it as? MediaItem }?.updatedAt }
		columnDataResolution.cell { param -> param?.value?.value?.let { it as? MediaItem }?.formattedResolution }
		columnDataLocation.cell { param -> param?.value?.value?.let { it as? MediaItem }?.formattedLocation }

		tableMetadata.root = TreeItem(null)
		val columnMetadataNameWidth =
				Database.list(Setting::class).find { it.name == Setting.COLUMN_METADATA_NAME_WIDTH }
					?: Setting(name = Setting.COLUMN_METADATA_NAME_WIDTH, value = "${200.0}")
		columnDataName.prefWidth = columnMetadataNameWidth.value?.toDoubleOrNull() ?: 200.0
		columnDataName.widthProperty()
			.addListener { _, _, v -> Database.save(columnMetadataNameWidth.apply { value = "${v}" }) }
		columnMetadataName.cell("name")
		columnMetadataTagType.cell { param -> param?.value?.value?.takeIf { it.id != null }?.tagType }

//		val columnMetadataValueWidth = Database.list(Setting::class).find { it.name == Setting.COLUMN_METADATA_VALUE_WIDTH }
//			?: Setting(name = Setting.COLUMN_METADATA_VALUE_WIDTH, value = "${200.0}")
//		columnDataName.prefWidth = columnMetadataValueWidth.value?.toDoubleOrNull() ?: 200.0
//		columnDataName.widthProperty().addListener { _, _, v -> Database.save(columnMetadataValueWidth.apply { value = "${v}" }) }
		columnMetadataValue.cell("raw")
		columnMetadataDescription.cell("description")
		columnMetadataValueType.cell { param -> param?.value?.value?.valueType?.displayName }

		vboxThumbnails.children.clear()
		sliderThumbnailSize.valueProperty()
			.addListener { _, _, value -> labelThumbnailSize.text = "${value.toInt()}px" }
		sliderThumbnailSize.value = 150.0

		imageView.fitWidthProperty().bind(tabPane.widthProperty())
		imageView.fitHeightProperty().bind(tabPane.widthProperty())
		mediaView.fitWidthProperty().bind(tabPane.widthProperty())
		mediaView.fitHeightProperty().bind(tabPane.widthProperty())

		treeViewNotifications.root = TreeItem(Notification.ROOT)

		treeViewNotifications.cell { _, value, empty ->
			graphic = value?.takeUnless { empty }?.toIcon()?.toImageView()
			text = value?.takeUnless { empty }?.displayName
			tooltip = value?.takeUnless { empty }?.value?.let { Tooltip(it) }
			contextMenu = value?.takeUnless { empty }?.createContextMenu()
		}
		tabPane.selectionModel.select(tabNotifications)

		selectedMediaProperty.addListener { _, _, item -> showItem(item) }
	}

	private fun start(initialScan: Boolean) {
		stage.setOnShown {
			Notifications.subject.sample(2, TimeUnit.SECONDS).subscribe { notifications ->
				Platform.runLater {
					treeViewNotifications.root.children.clear()
					treeViewNotifications.addAll(notifications)
				}
			}

			progressDialog = ProgressDialog("Refreshing media collection")
			progressDialog?.setOnShown { updateDataTree() }
			progressDialog?.show()

			if (initialScan) Database.list(Device::class)
				.filter { it.type == DeviceType.LOCAL }
				.mapNotNull { it.toUriOrNull() }
				.forEach { it.scan() }

			//driveDetector.removableDevices.forEach(::mount)
			driveDetector.addDriveListener { event ->
				Platform.runLater {
					when (event.eventType!!) {
						DeviceEventType.CONNECTED -> mount(event.storageDevice)
						DeviceEventType.REMOVED -> umount(event.storageDevice)
					}
				}
			}
		}
	}

	@FXML
	fun onAddFolder() {
		directoryChooser
			.apply { title = "Select directory to add to library" }
			.showDialog(stage)
			?.let { it.absoluteFile.toPath().toUri().toString() to it.name }
			?.takeIf { (uri, _) -> Database.list(Device::class).none { it.uri == uri } }
			?.takeIf { (uri, _) -> Database.list(Device::class).none { it.uri.startsWith(uri) } }
			?.takeIf { (uri, _) -> Database.list(Device::class).none { uri.startsWith(it.uri) } }
			?.let { (uri, name) -> Device(name = name, uri = uri) }
			?.also { Database.save(it) }
			?.also { tableData.root.children.add(TreeItem(it)) }
			?.also { tableData.root.children.sortWith(dataTreeComparator) }
			?.toUriOrNull()
			?.also { it.scan() }
	}

	@FXML
	fun onAddSharedFolder() {
		AddSambaShareDialog()
			.showAndWait()
			.orElse(null)
			?.also { Database.save(it) }
			?.also { tableData.root.children.add(TreeItem(it)) }
			?.also { tableData.root.children.sortWith(dataTreeComparator) }
			?.toUriOrNull()
			?.also { it.scan() }
	}

	@FXML
	fun onSynchronize() {
		tableData.selectionModel.selectedItems
			.map { it.value }
			.filter { it is Device || it is VirtualItem }
			.mapNotNull { it.toUriOrNull() }
			.forEach { it.scan(true) }
	}

	@FXML
	fun onFindDuplicates() {
		val primaryDeviceUri = Database.list(Device::class).find { it.isPrimary }?.toUriOrNull()
		if (primaryDeviceUri != null) DuplicateScanner()
			.onStart {
				multiProgress.update("find-duplicates", Progress.INDETERMINATE)
				labelStatus.text = "Scanning media ..."
				buttonFindDuplicates.isDisable = true
			}
			.onUpdate { (progress, count) ->
				multiProgress.update("find-duplicates", progress)
				labelStatus.text = when {
					multiProgress.indeterminate -> "Scanning for duplicates..."
					else -> "(${multiProgress.progress}/${multiProgress.total}) " + (if (count == 0)
						"Scanning for duplicates..." else "Found ${count} duplicates")
				}
			}
			.onSuccess { count ->
				multiProgress.update("find-duplicates", null)
				labelStatus.text = "Scanning for duplicates finished${count?.let { ", found ${it} duplicates" }}"
			}
			.onCancel {
				multiProgress.update("find-duplicates", null)
				labelStatus.text = "Scanning for duplicates canceled"
			}
			.onError {
				multiProgress.update("find-duplicates", null)
				labelStatus.text = "Error while scanning for duplicates!"
			}
			.onFinished {
				buttonFindDuplicates.isDisable = false
				scansInProgress.remove("find-duplicates")
				tableData.refresh()
			}
			.takeUnless { scansInProgress.containsKey("find-duplicates") }
			?.also { scansInProgress["find-duplicates"] = true }
			?.also { scanExecutor.submit(it) }
	}

	@FXML
	fun onInclude() {
	}

	@FXML
	fun onExclude() {
	}

	@FXML
	fun onRemove() {
		val toDelete = tableData.selectionModel.selectedItems
			.mapNotNull { it.value }
			.filterIsInstance<Device>()
		if (toDelete.isNotEmpty()) {
			val name = if (toDelete.size == 1) toDelete.first().name else "${toDelete.size} selected items"
			val result = Alert(Alert.AlertType.CONFIRMATION)
				.apply {
					title = "Remove confirmation"
					headerText = "Do you want to remove ${name} from collection?\n" +
							"(Files on the drive will not be touched)"
					buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
					isResizable = true
					//modality = Modality.APPLICATION_MODAL
					//initOwner(stage.owner)
				}
				.showAndWait()
				?.orElse(ButtonType.NO)
			if (result == ButtonType.YES) {
				Database.deleteAll(toDelete)
				tableData.root
					.findAll { item -> toDelete.map { it.id }.contains(item.id) }
					.forEach { it.parent.children.remove(it) }
			}
		}
	}

	@FXML
	fun onSettings() {
		buttonSettings.isDisable = true
		SettingsController.showAndWait()
		buttonSettings.isDisable = false
	}

	@FXML
	fun onPlayPause() {
		when (mediaPlayer?.status) {
			MediaPlayer.Status.PLAYING -> mediaPlayer?.pause()
			MediaPlayer.Status.PAUSED -> mediaPlayer?.play()
			else -> {
				mediaPlayer?.seek(Duration.ZERO)
				mediaPlayer?.play()
			}
		}
	}

	private fun showThumbnails(container: TreeItem<UriItem>) {
		tabPhoto.isDisable = true
		tabThumbnails.isDisable = false
		tabPane.selectionModel.select(tabThumbnails)

		val currentFlowPane = FlowPane().apply {
			minWidth = Region.USE_PREF_SIZE
			minHeight = Region.USE_PREF_SIZE
			prefWidth = Region.USE_COMPUTED_SIZE
			prefHeight = Region.USE_COMPUTED_SIZE
			maxWidth = Double.MAX_VALUE
			maxHeight = Double.MAX_VALUE
		}
		if (vboxThumbnails.children.isEmpty()) vboxThumbnails.children.add(0, currentFlowPane)
		else vboxThumbnails.children[0] = currentFlowPane


		uiUpdateExecutor.submit {
			val imageThumbnailsToLoad = mutableListOf<Pair<Node, MediaItem>>()
			val items = container
				.children
				.map { it.value }
				.filterIsInstance<MediaItem>()
				.sortedBy { it.name }
			for (item in items) {
				val bin = ThumbnailBin(
						mediaItem = item,
						background = Rectangle().apply {
							widthProperty().bind(sliderThumbnailSize.valueProperty().add(15.0))
							heightProperty().bind(sliderThumbnailSize.valueProperty().add(15.0))
							fill = Color.DODGERBLUE
							strokeWidth = 0.0
							isVisible = false
						},
						checkBox = CheckBox("").apply {
							selectedProperty().addListener { _, _, selected ->
								val bin = (parent.userData as ThumbnailBin)
								bin.background.isVisible = selected
								if (selected) selectedThumbnails.add(bin.mediaItem)
								else selectedThumbnails.remove(bin.mediaItem)
							}
							isVisible = false
							StackPane.setAlignment(this, Pos.TOP_LEFT)
							StackPane.setMargin(this, Insets(5.0))
						})

				val stackPane = StackPane(
						bin.background,
						StackPane(
								Pane()
									.apply { style = "-fx-background-color: white; -fx-border-color: black;" },
								ImageView(loadingImage)
									.apply {
										fitWidthProperty().bind(sliderThumbnailSize.valueProperty())
										fitHeightProperty().bind(sliderThumbnailSize.valueProperty())
										isPreserveRatio = true
										StackPane.setMargin(this, Insets(5.0))
										imageThumbnailsToLoad.add(this to item)
										setOnMouseClicked { event ->
											if (event.button == MouseButton.PRIMARY) (parent.parent.userData as ThumbnailBin).also {
												if (event.clickCount == 2) {
													selectedMediaProperty.set(it.mediaItem)
												} else {
													it.checkBox.isSelected = !it.checkBox.isSelected
												}
											}
										}
									},
								ImageView(item.icon()?.toImage(32, 32))
									.apply { StackPane.setAlignment(this, Pos.BOTTOM_RIGHT) })
							.apply {
								minWidth = Region.USE_PREF_SIZE
								minHeight = Region.USE_PREF_SIZE
								prefWidth = Region.USE_COMPUTED_SIZE
								prefHeight = Region.USE_COMPUTED_SIZE
								maxWidth = Region.USE_PREF_SIZE
								maxHeight = Region.USE_PREF_SIZE
							},
						bin.checkBox)
					.apply { userData = bin }
				Platform.runLater { currentFlowPane.children.add(stackPane) }
			}

			imageThumbnailsToLoad.loadThumbnailsInBackground(500)
		}
	}

	private fun showItem(item: MediaItem?) {
		tabThumbnails.isDisable = true
		tabPhoto.isDisable = item == null
		if (item == null) {
			tabPane.selectionModel.select(tabNotifications)
		} else {
			tabPane.selectionModel.select(tabPhoto)

			mediaPlayer?.stop()
			if (item.imageFormat != null) {
				previewLoaderHelper.loadPreviewInBackground(item) { mediaPlayer = it }
			} else if (item.videoFormat != null) {
				previewLoaderHelper.loadPreviewInBackground(item) { mediaPlayer = it }
				//mediaView.isVisible = true
				//imageView.isVisible = false
			} else {
				imageView.image = null
				mediaPlayer = null
				if (stackPanePreview.children.contains(imageView)) stackPanePreview.children.remove(imageView)
				if (stackPanePreview.children.contains(mediaView)) stackPanePreview.children.remove(mediaView)
				if (stackPanePreview.children.contains(hboxPreviewControls)) stackPanePreview.children.remove(
						hboxPreviewControls)
			}

			val metadataItems = item
				.metadata
				.sortedBy { it.directory }
				.groupBy { it.directory }
				.mapValues { (_, value) -> value.sortedBy { it.name } }
				.mapValues { (_, value) -> value.map { TreeItem(it) } }
				.mapKeys { (directory, _) -> TreeItem(MetadataTag(id = null, name = directory)) }
				.mapKeys { (directory, value) -> directory.also { it.children.addAll(value) } }
				.keys
				.sortedBy { it.value.name }
			tableMetadata.root.children.setAll(metadataItems)
			tableMetadata.root.expandTree()
			MetadataExtractorHelper.loadMetadataInBackground(item, tableMetadata)
		}
	}

	private fun mount(drive: USBStorageDevice) {
		var device = Database.get(Device::class, drive.uuid)

		if (device == null) {
			val result = Alert(Alert.AlertType.CONFIRMATION)
				.apply {
					title = "New removable drive detected"
					headerText = "New ${drive.systemDisplayName} drive detected."
					contentText = "Scan new device?"
					isResizable = true
					buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
					//modality = Modality.APPLICATION_MODAL
					//initOwner(stage.owner)
				}
				.showAndWait()
				?.orElse(ButtonType.NO)
			if (result == ButtonType.YES) {
				device = Device(id = drive.uuid, type = DeviceType.REMOVABLE)
			}
		}

		device
			?.also { it.name = drive.deviceName }
			?.also { it.uri = drive.rootDirectory.absoluteFile.toPath().toUri().toString() }
			?.also { Database.save(it) }
			?.also { dev ->
				if (tableData.root.children.none { it.value.id == dev.id }) {
					tableData.root.children.add(TreeItem(dev))
					tableData.root.children.sortWith(dataTreeComparator)
				}
			}
			?.toUriOrNull()
			?.also { it.scan() }
	}

	private fun umount(drive: USBStorageDevice) {
		tableData.root.children
			.find { drive.uuid.equals(it.value.id, true) }
			?.also { it.children.clear() }
	}

	private var updatingWholeTreeInProgress = false

	private fun updateDataTree(vararg devices: Device?) {
		if (!updatingWholeTreeInProgress) {
			updatingWholeTreeInProgress = devices.filterNotNull().isEmpty()
			GenerateTreeWorker(devices.filterNotNull())
				.onStart {
					if (progressDialog == null && devices.filterNotNull().isEmpty()) {
						progressDialog = ProgressDialog("Refreshing media collection")
						progressDialog?.show()
					}
				}
				.onUpdate { rootItem ->
					if (tableData.root.children.none { it.value.id == rootItem.value.id }) {
						tableData.root.children.add(rootItem)
						tableData.root.children.sortWith(dataTreeComparator)
					}
				}
				.onSuccess { firstLevelItems ->
					if (devices.filterNotNull().isEmpty() && firstLevelItems != null) {
						tableData.root.children.setAll(firstLevelItems)
					} else if (firstLevelItems != null) {
						tableData.root.children.removeIf { root -> firstLevelItems.any { it.value.id == root.value.id } }
						for (firstLevelItem in firstLevelItems) {
							tableData.root.children.add(firstLevelItem)
							tableData.root.children.sortWith(dataTreeComparator)
						}
					}
				}
				.onFinished {
					if (devices.filterNotNull().isEmpty()) {
						updatingWholeTreeInProgress = false
						progressDialog?.dismiss()
						progressDialog = null
					}
				}
				.also {
					treeUpdateExecutor.shutdownNow()
					treeUpdateExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)
					treeUpdateExecutor = Executors.newSingleThreadExecutor()
					treeUpdateExecutor.submit(it)
				}
		}
	}

	private fun URI.scan(force: Boolean = false) = ScanWorker(this to force)
		.onStart {
			multiProgress.update(toString(), Progress.INDETERMINATE)
			labelStatus.text = "Scanning media ..."
		}
		.onUpdate { (progress, info) ->
			multiProgress.update(toString(), progress)

			val additional = when (info) {
				is String -> info
				is SmbFile -> "Scanning ${info.path.uriDecode().removeSuffix("/")} ..."
				is File -> "Scanning ${info.absolutePath.uriDecode().removePrefix("file://").removeSuffix("/")} ..."
				is MediaItem -> "Scanning ${info.uri.uriDecode().removePrefix("file://").removeSuffix("/")} ..."
				else -> "Scanning media ..."
			}
			val prefix = when {
				multiProgress.indeterminate -> ""
				else -> "${(multiProgress.value * 100).toInt()}% (${multiProgress.progress}/${multiProgress.total}) "
			}

			labelStatus.text = "${prefix}${additional}"

			//if (!updatingWholeTreeInProgress) {
			//	if (info is Collection<*>) info
			//		.filterIsInstance<MediaItem>()
			//		.takeIf { it.isNotEmpty() }
			//		?.let { UpdateTreeWorker(tableData to it) }
			//		?.also { treeUpdateExecutor.submit(it) }
			//	if (info is MediaItem) UpdateTreeWorker(tableData to listOf(info))
			//		.also { treeUpdateExecutor.submit(it) }
			//}
		}
		.onSuccess {
			multiProgress.update(toString(), null)
			labelStatus.text = "Scanning media finished"
		}
		.onCancel {
			multiProgress.update(toString(), null)
			labelStatus.text = "Scanning media canceled"
		}
		.onError {
			multiProgress.update(toString(), null)
			labelStatus.text = "Error scanning media!"
		}
		.onFinished {
			scansInProgress.remove(toString())
			updateDataTree(device)
		}
		.takeUnless { scansInProgress.containsKey(toString()) }
		?.also { scansInProgress[toString()] = true }
		?.also { scanExecutor.submit(it) }

	private fun Collection<Pair<Node, MediaItem>>.loadThumbnailsInBackground(bbox: Int) {
		thumbnailLoaderExecutor?.shutdownNow()
		thumbnailLoaderExecutor?.awaitTermination(500, TimeUnit.MILLISECONDS)
		thumbnailLoaderExecutor = Executors.newSingleThreadExecutor()

		val imageLoader = ImageLoader(bbox to this)
			.onUpdate { (node, image) -> (node as? ImageView)?.image = image }
			.onCancel { }
			.onError { }

		thumbnailLoaderExecutor?.submit(imageLoader)
	}

	private fun createContextMenuItem(title: String, icon: Icons, action: () -> Unit): MenuItem =
			MenuItem(title, icon.toImageView()).apply { setOnAction { action() } }

	private fun UriItem.createContextMenu(): ContextMenu? {
		val uri = toUriOrNull()
		val menuItems = mutableListOf<MenuItem>()
		if (uri != null) {
			menuItems.add(createContextMenuItem("Scan", Icons.SCAN) { uri.scan(force = false) })
			menuItems.add(createContextMenuItem("Rescan all", Icons.SCAN) {
				val result = Alert(Alert.AlertType.CONFIRMATION)
					.apply {
						title = "Rescan confirmation"
						headerText = "Do you want to rescan ${name}?"
						buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
						isResizable = true
					}
					.showAndWait()
					?.orElse(ButtonType.NO)
				if (result == ButtonType.YES) uri.scan(force = true)
			})
		}
		if (this is Device && !isPrimary) {
			menuItems.add(createContextMenuItem("Set as primary", Icons.PRIMARY) {
				val result = Alert(Alert.AlertType.CONFIRMATION)
					.apply {
						title = "Primary selection confirmation"
						headerText = "Do you want to set ${name} as your primary storage?"
						buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
						isResizable = true
					}
					.showAndWait()
					?.orElse(ButtonType.NO)
				if (result == ButtonType.YES) {
					val devices = Database.list(Device::class).intermediate { it.isPrimary = id == it.id }
					Database.saveAll(devices)
					buttonFindDuplicates.isDisable = false
					tableData.refresh()
				}
			})
		}
		if (this is Device && type == DeviceType.SMB) {
			menuItems.add(createContextMenuItem("Properties", Icons.SETTINGS) {
				AddSambaShareDialog(this).showAndWait().orElse(null)?.also { Database.save(it) }
			})
		}
		if (this is Device && !isPrimary) {
			menuItems.add(createContextMenuItem("Remove device", Icons.TRASH) {
				val result = Alert(Alert.AlertType.CONFIRMATION)
					.apply {
						title = "Removal confirmation"
						headerText = "Do you want to remove ${name} from your collection?"
						buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
						isResizable = true
					}
					.showAndWait()
					?.orElse(ButtonType.NO)
				if (result == ButtonType.YES) {
					Database.delete(this)
					tableData.root.children.sortWith(dataTreeComparator)
					tableData.refresh()
				}
			})
		}
		return menuItems.takeIf { it.isNotEmpty() }?.toTypedArray()?.let { ContextMenu(*it) }
	}

	private fun Notification.createContextMenu(): ContextMenu? {
		val menuItems = mutableListOf<MenuItem>()
		menuItems.add(createContextMenuItem("Dismiss", Icons.TRASH) {
			val treeItem = treeViewNotifications.root.find { it == this@createContextMenu }
			if (treeItem != null) {
				treeItem.parent.children.remove(treeItem)
				Database.delete(this)
			}
		})
		return menuItems.takeIf { it.isNotEmpty() }?.toTypedArray()?.let { ContextMenu(*it) }
	}
}

