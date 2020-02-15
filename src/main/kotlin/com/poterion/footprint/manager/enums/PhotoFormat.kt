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

enum class PhotoFormat(val ext: String, val brand: String, val raw: Boolean, val comression: Boolean, val lossless: Boolean) {
    HASSELBLAD_3FR("3fr", "Hasselblad", true, false, true),
    ARRI_ALEXA_ARI("ari", "Arri Alexa", true, false, true),
    SONY_ARW("arw", "Sony", true, false, true),
    SONY_SRF("srf", "Sony", true, false, true),
    SONY_SR2("sr2", "Sony", true, false, true),
    CASIO_BAY("bay", "Casio", true, false, true),
    BLACKMAGIC_RAW("braw", "Blackmagic Design", true, false, true),
    CINTEL_CRI("cri", "Cintel", true, false, true),
    CANON_CRW("crw", "Canon", true, false, true),
    CANON_CR2("cr2", "Canon", true, false, true),
    CANON_CR3("cr3", "Canon", true, false, true),
    PHASE_ONE_CAP("cap", "Phase One", true, false, true),
    PHASE_ONE_IIQ("iiq", "Phase One", true, false, true),
    PHASE_ONE_EIP("eip", "Phase One", true, false, true),
    KODAK_DCS("dcs", "Kodak", true, false, true),
    KODAK_DCR("dcr", "Kodak", true, false, true),
    KODAK_DRF("drf", "Kodak", true, false, true),
    KODAK_K25("k25", "Kodak", true, false, true),
    KODAK_KDC("kdc", "Kodak", true, false, true),
    ADOBE_DNG("dng", "Adobe", true, false, true),
    EPSON_ERF("erf", "Epson", true, false, true),
    HASSELBLAD_FFF("fff", "Imacon/Hasselblad", true, false, true),
    GOPRO_GPR("gpr", "GoPro", true, false, true),
    MAMIYA_MEF("mef", "Mamiya", true, false, true),
    MINOLTA_MDC("mdc", "Minolta, Agfa", true, false, true),
    LEAF_MOS("mos", "Leaf", true, false, true),
    MINOLTA_MRW("mrw", "Minolta, Konica Minolta", true, false, true),
    NIKON_NEF("nef", "Nikon", true, false, true),
    NIKON_NRW("nrw", "Nikon", true, false, true),
    OLYMUS_ORF("orf", "Olympus", true, false, true),
    PENTAX_PEF("pef", "Pentax", true, false, true),
    PENTAX_PTX("ptx", "Pentax", true, false, true),
    LOGITECHPXN("pxn", "Logitech", true, false, true),
    RED_DIGITAL_CINEMA_R3D("r3d", "Red Digital Cinema", true, false, true),
    FUJI_RAF("raf", "Fuji", true, false, true),
    PANASONIC_RAW("raw", "Panasonic", true, false, true),
    PANASONIC_RW2("rw2", "Panasonic", true, false, true),
    LEICA_RAW("raw", "Leica", true, false, true),
    LEICA_RWL("rwl", "Leica", true, false, true),
    LEICA_DNG("dng", "Leica", true, false, true),
    RAWZOR_RWZ("rwz", "Rawzor", true, false, true),
    SAMSUNG_SRW("srw", "Samsung", true, false, true),
    SIGMA_X3F("x3f", "Sigma", true, false, true),

    JPEG("jpg", "", false, true, false),
    TIFF("tif", "", false, true, true)
}