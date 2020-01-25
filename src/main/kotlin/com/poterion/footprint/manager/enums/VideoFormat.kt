package com.poterion.footprint.manager.enums

enum class VideoFormat(
	val ext: String,
	val containerFormat: VideoContainerFormat,
	val videoCodingFormats: Set<VideoCodingFormat>,
	val audioCodingFormats: Set<AudioCodingFormat>) {
	AVI("avi",
		VideoContainerFormat.AVI,
		emptySet(),
		emptySet()),
	MPEG1("mpg",
		  VideoContainerFormat.MPEG1,
		  setOf(VideoCodingFormat.MPEG1),
		  setOf(AudioCodingFormat.MP3)),
	MPEG2("mpeg",
		  VideoContainerFormat.MPEG2,
		  setOf(VideoCodingFormat.H262),
		  setOf(AudioCodingFormat.AAC, AudioCodingFormat.MP3)),
	MPEG4("mp4",
		  VideoContainerFormat.MPEG4,
		  setOf(VideoCodingFormat.H264),
		  setOf(AudioCodingFormat.AAC)),
}