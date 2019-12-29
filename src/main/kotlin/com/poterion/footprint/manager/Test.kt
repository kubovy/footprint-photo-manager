package com.poterion.footprint.manager

import java.io.File
import javax.imageio.ImageIO
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.imageio.IIOImage
import javax.imageio.stream.MemoryCacheImageOutputStream
import javax.imageio.ImageWriteParam
import java.awt.image.BufferedImage
import javax.swing.Spring.height




fun main(args: Array<String>) {
	println()
////	val width = 5184 / 2
////	val height = 3456 / 2
//	val inputFile = File("/Users/mariamrady/Pictures/Canon/DCIM/100CANON/IMG_0001.CR2")
//	val outputFile = File("/Users/mariamrady/Pictures/Canon/DCIM/100CANON/IMG_0001.jpg")
//
////	val inputImage = ImageIO.read(inputFile)
////	ImageIO.write(inputImage, "png", outputFile)
//
//	val inputStream = BufferedInputStream(FileInputStream(inputFile))
////	val imageReaderIterator = ImageIO.getImageReadersByMIMEType("image/x-canon-cr2")
//	val imageReaderIterator = ImageIO.getImageReadersBySuffix("cr2")
//	val reader = imageReaderIterator.next()
//	val imageInputStream = ImageIO.createImageInputStream(inputStream)
//	reader.setInput(imageInputStream, false, false)
////	val inputImage = reader.read(0)
//	val inputImage = reader.readThumbnail(0, 0)
//	val imageMetadata = reader.getImageMetadata(0)
//
////	val bbox = 500
////	val width = if (inputImage.width >= inputImage.height)
////		bbox else inputImage.width * bbox / inputImage.height
////	val height = if (inputImage.height >= inputImage.width)
////		bbox else inputImage.height * bbox / inputImage.width
////
////	val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_BGR)
////	outputImage.createGraphics().drawImage(inputImage, 0, 0, width, height, null)
//	val outputImage = inputImage
//
//	val imageWriterIterator = ImageIO.getImageWritersByMIMEType("image/jpeg")
//	val writer = imageWriterIterator.next()
//	val imageWriteParam = writer.defaultWriteParam
//	imageWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
//	imageWriteParam.compressionQuality = 0.95f
//	val imageOutputStream = MemoryCacheImageOutputStream(FileOutputStream(outputFile))
//	writer.output = imageOutputStream
//	val iioImage = IIOImage(outputImage, null, imageMetadata)
//	writer.write(null, iioImage, imageWriteParam)
//	writer.dispose()
}