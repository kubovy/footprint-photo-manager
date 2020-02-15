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

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class TagValueType(val displayName: String) {
	UNKNOWN(""),
	BOOLEAN("Boolean"),
	BYTE_ARRAY("Byte[]"),
	DATE("Date"),
	DOUBLE("Double"),
	DOUBLE_ARRAY("Double[]"),
	FLOAT("Float"),
	FLOAT_ARRAY("Float[]"),
	INT("Int"),
	INT_ARRAY("Int[]"),
	LONG("Long"),
	OBJECT("Object"),
	RATIONAL("Rational"),
	RATIONAL_ARRAY("Rational[]"),
	STRING("String");

	val isInteger
		get() = this == INT || this == LONG

	val isFloating
		get() = this == FLOAT || this == DOUBLE || this == RATIONAL

	val isNumeric
		get() = isInteger || isFloating
}