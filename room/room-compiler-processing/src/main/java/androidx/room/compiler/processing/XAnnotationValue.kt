/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.compiler.processing

/**
 * This wraps information about an argument in an annotation.
 */
interface XAnnotationValue {
    /**
     * The property name.
     */
    val name: String

    /**
     * The value set on the annotation property, or the default value if it was not explicitly set.
     *
     * Possible types are:
     * - Primitives (Boolean, Byte, Int, Long, Float, Double)
     * - String
     * - XEnumEntry
     * - XAnnotation
     * - XType
     * - List of [XAnnotationValue]
     */
    val value: Any?

    /**
     * Returns the type of this annotation value as declared in the annotation class.
     *
     * For Java sources, this will be the return type of the method that declares this annotation
     * value. For Kotlin sources, this will be the constructor parameter type that declares this
     * annotation value.
     */
    val valueType: XType

    /** Returns true if the value is an [XType] */
    fun hasTypeValue(): Boolean

    /** Returns the value as a [XType]. */
    fun asType(): XType = value as XType

    /** Returns true if the value is a list of [XType] */
    fun hasTypeListValue(): Boolean

    /** Returns the value as a list of [XType]. */
    fun asTypeList(): List<XType> = asAnnotationValueList().map { it.asType() }

    /** Returns true if the value is an [XAnnotation] */
    fun hasAnnotationValue(): Boolean

    /** Returns the value as another [XAnnotation]. */
    fun asAnnotation(): XAnnotation = value as XAnnotation

    /** Returns true if the value is a list of [XAnnotation] */
    fun hasAnnotationListValue(): Boolean

    /** Returns the value as a list of [XAnnotation]. */
    fun asAnnotationList(): List<XAnnotation> = asAnnotationValueList().map { it.asAnnotation() }

    /** Returns true if the value is an [XEnumEntry] */
    fun hasEnumValue(): Boolean

    /** Returns the value as a [XEnumEntry]. */
    fun asEnum(): XEnumEntry = value as XEnumEntry

    /** Returns true if the value is a list of [XEnumEntry] */
    fun hasEnumListValue(): Boolean

    /** Returns the value as a list of [XEnumEntry]. */
    fun asEnumList(): List<XEnumEntry> = asAnnotationValueList().map { it.asEnum() }

    /** Returns true if the value is an [Boolean] */
    fun hasBooleanValue(): Boolean

    /** Returns the value as a [Boolean]. */
    fun asBoolean(): Boolean = value as Boolean

    /** Returns true if the value is a list of [Boolean] */
    fun hasBooleanListValue(): Boolean

    /** Returns the value as a list of [Boolean]. */
    fun asBooleanList(): List<Boolean> = asAnnotationValueList().map { it.asBoolean() }

    /** Returns true if the value is an [Boolean] */
    fun hasStringValue(): Boolean

    /** Returns the value as a [String]. */
    fun asString(): String = value as String

    /** Returns true if the value is a list of [String] */
    fun hasStringListValue(): Boolean

    /** Returns the value as a list of [String]. */
    fun asStringList(): List<String> = asAnnotationValueList().map { it.asString() }

    /** Returns true if the value is an [Int] */
    fun hasIntValue(): Boolean

    /** Returns the value as a [Int]. */
    fun asInt(): Int = value as Int

    /** Returns true if the value is a list of [Int] */
    fun hasIntListValue(): Boolean

    /** Returns the value as a list of [Int]. */
    fun asIntList(): List<Int> = asAnnotationValueList().map { it.asInt() }

    /** Returns true if the value is an [Long] */
    fun hasLongValue(): Boolean

    /** Returns the value as a [Long]. */
    fun asLong(): Long = value as Long

    /** Returns true if the value is a list of [Long] */
    fun hasLongListValue(): Boolean

    /** Returns the value as a list of [Long]. */
    fun asLongList(): List<Long> = asAnnotationValueList().map { it.asLong() }

    /** Returns true if the value is an [Short] */
    fun hasShortValue(): Boolean

    /** Returns the value as a [Short]. */
    fun asShort(): Short = value as Short

    /** Returns true if the value is a list of [Short] */
    fun hasShortListValue(): Boolean

    /** Returns the value as a list of [Short]. */
    fun asShortList(): List<Short> = asAnnotationValueList().map { it.asShort() }

    /** Returns true if the value is an [Float] */
    fun hasFloatValue(): Boolean

    /** Returns the value as a [Float]. */
    fun asFloat(): Float = value as Float

    /** Returns true if the value is a list of [Float] */
    fun hasFloatListValue(): Boolean

    /** Returns the value as a list of [Float]. */
    fun asFloatList(): List<Float> = asAnnotationValueList().map { it.value as Float }

    /** Returns true if the value is an [Double] */
    fun hasDoubleValue(): Boolean

    /** Returns the value as a [Double]. */
    fun asDouble(): Double = value as Double

    /** Returns true if the value is a list of [Double] */
    fun hasDoubleListValue(): Boolean

    /** Returns the value as a list of [Double]. */
    fun asDoubleList(): List<Double> = asAnnotationValueList().map { it.asDouble() }

    /** Returns true if the value is an [Byte] */
    fun hasByteValue(): Boolean

    /** Returns the value as a [Byte]. */
    fun asByte(): Byte = value as Byte

    /** Returns true if the value is a list of [Byte] */
    fun hasByteListValue(): Boolean

    /** Returns the value as a list of [Byte]. */
    fun asByteList(): List<Byte> = asAnnotationValueList().map { it.asByte() }

    /** Returns true if the value is an [Char] */
    fun hasCharValue(): Boolean

    /** Returns the value as a [Char]. */
    fun asChar(): Char = value as Char

    /** Returns true if the value is a list of [Char] */
    fun hasCharListValue(): Boolean

    /** Returns the value as a list of [Char]. */
    fun asCharList(): List<Char> = asAnnotationValueList().map { it.asChar() }

    /** Returns true if the value is a list. */
    fun hasListValue(): Boolean

    /**Returns the value a list of [XAnnotationValue]. */
    @Suppress("UNCHECKED_CAST") // Values in a list are always wrapped in XAnnotationValue
    fun asAnnotationValueList(): List<XAnnotationValue> = value as List<XAnnotationValue>
}
