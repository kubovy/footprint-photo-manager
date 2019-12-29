package com.poterion.footprint.manager.utils

import jcifs.smb.SmbFile

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
val SmbFile.extension: String
	get() = name.fileExtension()
