package com.poterion.footprint.manager.xuggle

import com.xuggle.mediatool.MediaListenerAdapter
import com.xuggle.mediatool.demos.DecodeAndCaptureFrames.MICRO_SECONDS_BETWEEN_FRAMES
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.xuggler.Global
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
internal class ImageSnapListener(private val outputFile: File,
								 private val requestedWidth: Int = 0,
								 private val requestedHeight: Int = 0) : MediaListenerAdapter() {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(ImageSnapListener::class.java)
	}

	// The video stream index, used to ensure we display frames from one and
	// only one video stream from the media container.
	private var mVideoStreamIndex = -1

	// Time of last frame write
	private var mLastPtsWrite = Global.NO_PTS

	override fun onVideoPicture(event: IVideoPictureEvent) {
		if (event.streamIndex != mVideoStreamIndex) {
			// if the selected video stream id is not yet set, go ahead and select this lucky video stream
			if (mVideoStreamIndex == -1) mVideoStreamIndex = event.streamIndex else return
		}
		// if uninitialized, back date mLastPtsWrite to get the very first frame
		if (mLastPtsWrite == Global.NO_PTS) mLastPtsWrite = event.timeStamp - MICRO_SECONDS_BETWEEN_FRAMES

		// if it's time to write the next frame
		if (event.timeStamp - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {

			val width = when {
				requestedHeight > 0 -> requestedWidth
				requestedWidth == 0 -> event.image.width
				event.image.width >= event.image.height -> requestedWidth
				else -> event.image.width * requestedWidth / event.image.height
			}

			val height = when {
				requestedHeight > 0 -> requestedHeight
				requestedWidth == 0 -> event.image.height
				event.image.height >= event.image.width -> requestedWidth
				else -> event.image.height * requestedWidth / event.image.width
			}

			val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
			outputImage.createGraphics().drawImage(event.image, 0, 0, width, height, null)

			try {
				ImageIO.write(outputImage, "jpg", outputFile)
				val seconds: Double = event.timeStamp.toDouble() / Global.DEFAULT_PTS_PER_SECOND
				LOGGER.info("At elapsed time of %6.3f seconds wrote: %s".format(seconds, outputFile))
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
			}
			// update last write time
			mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES
		}
	}
}