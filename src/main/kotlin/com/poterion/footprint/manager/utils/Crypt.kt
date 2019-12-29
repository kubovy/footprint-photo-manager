package com.poterion.footprint.manager.utils

import java.nio.charset.Charset
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */

private const val SALT = "p2KiPkUUqArHHImPK5hvs4GhowWg5zalrRVn7HA8wM81cmVNOkBYD0xKYLc9udc6gXAOhX5xxdnORB1X"
private const val DELIMITER = ":"
private var passwordCache: String = ""

fun setPasswordForEncryption(password: String) {
	passwordCache = password
}

fun createSecretKey(password: String, iterationCount: Int = 1_000_000, keyLength: Int = 128): SecretKeySpec =
	createSecretKey(password.toCharArray(), iterationCount, keyLength)

fun createSecretKey(password: CharArray, iterationCount: Int = 1_000_000, keyLength: Int = 128): SecretKeySpec {
	val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
	val keySpec = PBEKeySpec(password, SALT.toByteArray(), iterationCount, keyLength)
	val keyTmp = keyFactory.generateSecret(keySpec)
	return SecretKeySpec(keyTmp.encoded, "AES")
}

fun encrypt(message: String, password: String? = null, iterationCount: Int = 1_000_000, keyLength: Int = 128) =
	encrypt(message, createSecretKey(password ?: passwordCache, iterationCount, keyLength))

fun String.encrypt(password: String? = null): String = encrypt(this, password)

fun encrypt(message: String, key: SecretKeySpec): String {
	val pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
		init(Cipher.ENCRYPT_MODE, key)
	}
	val ivParameterSpec = pbeCipher.parameters.getParameterSpec(IvParameterSpec::class.java)
	val cryptoText = pbeCipher.doFinal(message.toByteArray(charset("UTF-8")))
	val iv = ivParameterSpec.iv
	return iv.base64Encode() + DELIMITER + cryptoText.base64Encode()
}

fun decrypt(cipherText: String, password: String? = null, iterationCount: Int = 1_000_000, keyLength: Int = 128) =
	decrypt(cipherText, createSecretKey(password ?: passwordCache, iterationCount, keyLength))

fun String.decrypt(password: String? = null): String = decrypt(this, password)

fun decrypt(cipherText: String, key: SecretKeySpec): String {
	val (iv, property) = cipherText.split(DELIMITER)
	val pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
	pbeCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv.base64Decode()))
	return String(pbeCipher.doFinal(property.base64Decode()), charset("UTF-8"))
}

fun ByteArray.base64Encode(): String = Base64.getEncoder().encodeToString(this)

fun String.base64Encode(charset: Charset = charset("UTF-8")): String = toByteArray(charset).base64Encode()

fun String.base64Decode(): ByteArray = Base64.getDecoder().decode(this)

