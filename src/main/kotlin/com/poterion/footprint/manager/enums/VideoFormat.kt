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