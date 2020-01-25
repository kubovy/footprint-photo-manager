package com.poterion.footprint.manager.utils

import com.poterion.utils.kotlin.fileExtension
import jcifs.smb.SmbFile

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
val SmbFile.extension: String
	get() = name.fileExtension()
