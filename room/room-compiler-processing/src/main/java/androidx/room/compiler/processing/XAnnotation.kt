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

    /**
     * All values declared in the annotation class.
     */
    val annotationValues: List<XAnnotationValue>

    /**
     * Returns the value of the given [methodName] as a type reference.
     */
    fun getAsType(methodName: String): XType = get(methodName)

    /**
     * Returns the value of the given [methodName] as a list of type references.
     */
    fun getAsTypeList(methodName: String): List<XType> = get(methodName)

    /**
     * Returns the value of the given [methodName] as another [XAnnotation].
     */
    fun getAsAnnotation(methodName: String): XAnnotation = get(methodName)

    /**
     * Returns the value of the given [methodName] as a list of [XAnnotation].
     */
    fun getAsAnnotationList(methodName: String): List<XAnnotation> = get(methodName)

    /**
     * Returns the value of the given [methodName] as a [XEnumEntry].
     */
    fun getAsEnum(methodName: String): XEnumEntry = get(methodName)

    /**
     * Returns the value of the given [methodName] as a list of [XEnumEntry].
     */
    fun getAsEnumList(methodName: String): List<XEnumEntry> = get(methodName)
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
inline fun <reified T> XAnnotation.get(methodName: String): T {
    val argument = annotationValues.firstOrNull { it.name == methodName }
        ?: error("No property named $methodName was found in annotation $name")

    return argument.value as? T ?: error(
        "Value of $methodName of type ${argument.value?.javaClass} " +
            "cannot be cast to ${T::class.java}"
    )
}

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
fun <T> XAnnotation.get(methodName: String, clazz: Class<T>): T {
    val argument = annotationValues.firstOrNull { it.name == methodName }
        ?: error("No property named $methodName was found in annotation $name")

    if (!clazz.isInstance(argument.value)) {
        error("Value of $methodName of type ${argument.value?.javaClass} cannot be cast to $clazz")
    }

    @Suppress("UNCHECKED_CAST")
    return argument.value as T
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
