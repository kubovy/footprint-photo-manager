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