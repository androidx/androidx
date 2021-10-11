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
 * This wraps annotations that may be declared in sources, and thus not representable with a
 * compiled type. This is an equivalent to the Java AnnotationMirror API.
 *
 * Values in the annotation can be accessed via [annotationValues], the [XAnnotation.get] extension
 * function, or any of the "getAs*" helper functions.
 *
 * In comparison, [XAnnotationBox] is used in situations where the annotation class is already
 * compiled and can be referenced. This can be converted with [asAnnotationBox] if the annotation
 * class is already compiled.
 */
interface XAnnotation {
    /**
     * The simple name of the annotation class.
     */
    val name: String

    /**
     * The fully qualified name of the annotation class.
     * Accessing this forces the type to be resolved.
     */
    val qualifiedName: String

    /**
     * The [XType] representing the annotation class.
     *
     * Accessing this requires resolving the type, and is thus more expensive that just accessing
     * [name].
     */
    val type: XType

    /** All values declared in the annotation class. */
    val annotationValues: List<XAnnotationValue>

    /** Returns the value of the given [methodName] as a type reference. */
    fun getAsType(methodName: String): XType = getAnnotationValue(methodName).asType()

    /** Returns the value of the given [methodName] as a list of type references. */
    fun getAsTypeList(methodName: String): List<XType> = getAnnotationValue(methodName).asTypeList()

    /** Returns the value of the given [methodName] as another [XAnnotation]. */
    fun getAsAnnotation(methodName: String): XAnnotation =
        getAnnotationValue(methodName).asAnnotation()

    /** Returns the value of the given [methodName] as a list of [XAnnotation]. */
    fun getAsAnnotationList(methodName: String): List<XAnnotation> =
        getAnnotationValue(methodName).asAnnotationList()

    /** Returns the value of the given [methodName] as a [XEnumEntry]. */
    fun getAsEnum(methodName: String): XEnumEntry = getAnnotationValue(methodName).asEnum()

    /** Returns the value of the given [methodName] as a list of [XEnumEntry]. */
    fun getAsEnumList(methodName: String): List<XEnumEntry> =
        getAnnotationValue(methodName).asEnumList()

    /** Returns the value of the given [methodName] as a [Boolean]. */
    fun getAsBoolean(methodName: String): Boolean = getAnnotationValue(methodName).asBoolean()

    /** Returns the value of the given [methodName] as a list of [Boolean]. */
    fun getAsBooleanList(methodName: String): List<Boolean> =
        getAnnotationValue(methodName).asBooleanList()

    /** Returns the value of the given [methodName] as a [String]. */
    fun getAsString(methodName: String): String = getAnnotationValue(methodName).asString()

    /** Returns the value of the given [methodName] as a list of [String]. */
    fun getAsStringList(methodName: String): List<String> =
        getAnnotationValue(methodName).asStringList()

    /** Returns the value of the given [methodName] as a [Int]. */
    fun getAsInt(methodName: String): Int = getAnnotationValue(methodName).asInt()

    /** Returns the value of the given [methodName] as a list of [Int]. */
    fun getAsIntList(methodName: String): List<Int> = getAnnotationValue(methodName).asIntList()

    /** Returns the value of the given [methodName] as a [Long]. */
    fun getAsLong(methodName: String): Long = getAnnotationValue(methodName).asLong()

    /** Returns the value of the given [methodName] as a list of [Long]. */
    fun getAsLongList(methodName: String): List<Long> = getAnnotationValue(methodName).asLongList()

    /** Returns the value of the given [methodName] as a [Short]. */
    fun getAsShort(methodName: String): Short = getAnnotationValue(methodName).asShort()

    /** Returns the value of the given [methodName] as a list of [Short]. */
    fun getAsShortList(methodName: String): List<Short> =
        getAnnotationValue(methodName).asShortList()

    /** Returns the value of the given [methodName] as a [Float]. */
    fun getAsFloat(methodName: String): Float = getAnnotationValue(methodName).asFloat()

    /** Returns the value of the given [methodName] as a list of [Float]. */
    fun getAsFloatList(methodName: String): List<Float> =
        getAnnotationValue(methodName).asFloatList()

    /** Returns the value of the given [methodName] as a [Double]. */
    fun getAsDouble(methodName: String): Double = getAnnotationValue(methodName).asDouble()

    /** Returns the value of the given [methodName] as a list of [Double]. */
    fun getAsDoubleList(methodName: String): List<Double> =
        getAnnotationValue(methodName).asDoubleList()

    /** Returns the value of the given [methodName] as a [Byte]. */
    fun getAsByte(methodName: String): Byte = getAnnotationValue(methodName).asByte()

    /** Returns the value of the given [methodName] as a list of [Byte]. */
    fun getAsByteList(methodName: String): List<Byte> = getAnnotationValue(methodName).asByteList()

    /** Returns the value of the given [methodName] as a list of [Byte]. */
    fun getAsAnnotationValueList(methodName: String): List<XAnnotationValue> =
        getAnnotationValue(methodName).asAnnotationValueList()

    /**Returns the value of the given [methodName] as a [XAnnotationValue]. */
    fun getAnnotationValue(methodName: String): XAnnotationValue {
        return annotationValues.firstOrNull { it.name == methodName }
            ?: error("No property named $methodName was found in annotation $name")
    }
}

/**
 * Returns the value of the given [methodName], throwing an exception if the method is not
 * found or if the given type [T] does not match the actual type.
 *
 * Note that non primitive types are wrapped by interfaces in order to allow them to be
 * represented by the process:
 * - "Class" types are represented with [XType]
 * - Annotations are represented with [XAnnotation]
 * - Enums are represented with [XEnumEntry]
 *
 * For convenience, wrapper functions are provided for these types, eg [XAnnotation.getAsType]
 */
// TODO: Consider deprecating this method for getAs*() methods
@Suppress("DEPRECATION")
inline fun <reified T> XAnnotation.get(methodName: String): T = get(methodName, T::class.java)

/**
 * Returns the value of the given [methodName], throwing an exception if the method is not
 * found or if the given type [T] does not match the actual type.
 *
 * This uses a non-reified type and takes in a Class so it is callable by Java users.
 *
 * Note that non primitive types are wrapped by interfaces in order to allow them to be
 * represented by the process:
 * - "Class" types are represented with [XType]
 * - Annotations are represented with [XAnnotation]
 * - Enums are represented with [XEnumEntry]
 *
 * For convenience, wrapper functions are provided for these types, eg [XAnnotation.getAsType]
 */
@Deprecated("Use one of the getAs*() methods instead, e.g. getAsBoolean().")
fun <T> XAnnotation.get(methodName: String, clazz: Class<T>): T {
    val argument = getAnnotationValue(methodName)

    val value = if (argument.value is List<*>) {
        // If the argument is for a list, unwrap each item in the list
        (argument.value as List<*>).map { (it as XAnnotationValue).value }
    } else {
        argument.value
    }

    if (!clazz.isInstance(value)) {
        error("Value of $methodName of type ${value?.javaClass} cannot be cast to $clazz")
    }

    @Suppress("UNCHECKED_CAST")
    return value as T
}

/**
 * Get a representation of this [XAnnotation] as a [XAnnotationBox]. This is helpful for converting
 * to [XAnnotationBox] after getting annotations with [XAnnotated.getAllAnnotations].
 *
 * Only possible if the annotation class is available (ie it is in the classpath and not in
 * the compiled sources).
 */
inline fun <reified T : Annotation> XAnnotation.asAnnotationBox(): XAnnotationBox<T> {
    return (this as InternalXAnnotation).asAnnotationBox(T::class.java)
}
