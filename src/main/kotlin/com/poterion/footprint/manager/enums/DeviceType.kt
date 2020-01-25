package com.poterion.footprint.manager.enums

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class DeviceType(val protocol: String, val remote: Boolean) {
	LOCAL("file://", false),
	REMOVABLE("file://", false),
	//MTP,
	SMB("smb://", true),
	//NFS,
	//DROPBOX,
	//GOOGLE_DRIVE
}