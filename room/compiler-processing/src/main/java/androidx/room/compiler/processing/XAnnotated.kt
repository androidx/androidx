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

import kotlin.reflect.KClass

/**
 * Common interface implemented by elements that might have annotations.
 */
interface XAnnotated {
    /**
     * If the current element has an annotation with the given [annotation] class, a boxed instance
     * of it will be returned where fields can be read. Otherwise, `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>?

    /**
     * Returns `true` if this element has an annotation that is declared in the given package.
     */
    // a very sad method but helps avoid abstraction annotation
    fun hasAnnotationWithPackage(pkg: String): Boolean

    /**
     * Returns `true` if this element is annotated with the given [annotation].
     *
     * @see [toAnnotationBox]
     * @see [hasAnyOf]
     */
    fun hasAnnotation(annotation: KClass<out Annotation>): Boolean

    /**
     * Returns `true` if this element has one of the [annotations].
     */
    fun hasAnyOf(vararg annotations: KClass<out Annotation>) = annotations.any(this::hasAnnotation)
}