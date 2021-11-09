/*
 * Copyright 2020 The Android Open Source Project
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

import com.squareup.javapoet.ClassName
import kotlin.reflect.KClass

/**
 * Common interface implemented by elements that might have annotations.
 */
interface XAnnotated {
    /**
     * Returns the list of [XAnnotation] elements that have the same qualified name as the given
     * [annotationName]. Otherwise, returns an empty list.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type,
     * not the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotations(annotationName: ClassName): List<XAnnotation> {
        return getAllAnnotations().filter { annotationName.canonicalName() == it.qualifiedName }
    }

    /**
     * Gets the list of annotations with the given type.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type,
     * not the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotations(
        annotation: KClass<T>
    ): List<XAnnotationBox<T>>

    /**
     * Returns all annotations on this element represented as [XAnnotation].
     *
     * As opposed to other functions like [getAnnotations] this does not require you to have a
     * reference to each annotation class, and thus it can represent annotations in the module
     * sources being compiled. However, note that the returned [XAnnotation] cannot provide
     * an instance of the annotation (like [XAnnotationBox.value] can) and instead all values
     * must be accessed dynamically.
     *
     * The returned [XAnnotation]s can be converted to [XAnnotationBox] via
     * [XAnnotation.asAnnotationBox] if the annotation class is on the class path.
     */
    fun getAllAnnotations(): List<XAnnotation>

    /**
     * Returns `true` if this element is annotated with the given [annotation].
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type,
     * not the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(
        annotation: KClass<out Annotation>
    ): Boolean

    /**
     * Returns `true` if this element is annotated with an [XAnnotation] that has the same
     * qualified name as the given [annotationName].
     *
     * @see [hasAnyAnnotation]
     */
    fun hasAnnotation(annotationName: ClassName): Boolean {
        return getAnnotations(annotationName).isNotEmpty()
    }

    /**
     * Returns `true` if this element has an annotation that is declared in the given package.
     * Alternatively, all annotations can be accessed with [getAllAnnotations].
     */
    fun hasAnnotationWithPackage(pkg: String): Boolean

    /**
     * Returns `true` if this element has one of the [annotations].
     */
    fun hasAnyAnnotation(vararg annotations: ClassName) = annotations.any(this::hasAnnotation)

    /**
     * Returns `true` if this element has one of the [annotations].
     */
    fun hasAnyAnnotation(vararg annotations: KClass<out Annotation>) =
        annotations.any(this::hasAnnotation)

    /**
     * Returns `true` if this element has all the [annotations].
     */
    fun hasAllAnnotations(vararg annotations: ClassName): Boolean =
        annotations.all(this::hasAnnotation)

    /**
     * Returns `true` if this element has all the [annotations].
     */
    fun hasAllAnnotations(vararg annotations: KClass<out Annotation>): Boolean =
        annotations.all(this::hasAnnotation)

    @Deprecated(
        replaceWith = ReplaceWith("getAnnotation(annotation)"),
        message = "Use getAnnotation(not repeatable) or getAnnotations (repeatable)"
    )
    fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>? =
        getAnnotation(annotation)

    /**
     * If the current element has an annotation with the given [annotation] class, a boxed instance
     * of it will be returned where fields can be read. Otherwise, `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotation(annotation: KClass<T>): XAnnotationBox<T>? {
        return getAnnotations(annotation).firstOrNull()
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName].
     * Otherwise, `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun getAnnotation(annotationName: ClassName): XAnnotation? {
        return getAnnotations(annotationName).firstOrNull()
    }

    /**
     * Returns the [XAnnotation] that has the same qualified name as [annotationName].
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun requireAnnotation(annotationName: ClassName): XAnnotation {
        return getAnnotation(annotationName)!!
    }

    /**
     * Returns a boxed instance of the given [annotation] class where fields can be read.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> requireAnnotation(annotation: KClass<T>) =
        checkNotNull(getAnnotation(annotation)) {
            "Cannot find required annotation $annotation"
        }
}