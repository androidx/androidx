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

import java.lang.annotation.Repeatable

@PublishedApi
internal interface InternalXAnnotation : XAnnotation {
    fun <T : Annotation> asAnnotationBox(annotationClass: Class<T>): XAnnotationBox<T>
}

/**
 * If this represents a repeatable annotation container this will return the repeated annotations
 * nested inside it.
 */
internal fun XAnnotation.unwrapRepeatedAnnotationsFromContainer(): List<XAnnotation>? {
    return try {
        // The contract of a repeatable annotation requires that the container annotation have a single
        // "default" method that returns an array typed with the repeatable annotation type.
        val nestedAnnotations = getAsAnnotationList("value")

        // Ideally we would read the value of the Repeatable annotation to get the container class
        // type and check that it matches "this" type. However, there seems to be a KSP bug where
        // the value of Repeatable is not present so the best we can do is check that all the nested
        // members are annotated with repeatable.
        // https://github.com/google/ksp/issues/358
        val isRepeatable = nestedAnnotations.all {
            // The java and kotlin versions of Repeatable are not interchangeable.
            // https://github.com/google/ksp/issues/459 asks whether the built in type mapper
            // should convert them, but it may not be possible because there are differences
            // to how they work (eg different parameters).
            it.type.typeElement?.hasAnnotation(Repeatable::class) == true ||
            it.type.typeElement?.hasAnnotation(kotlin.annotation.Repeatable::class) == true
        }

        if (isRepeatable) nestedAnnotations else null
    } catch (e: Throwable) {
        // If the "value" type either doesn't exist or isn't an array of annotations then the
        // above code will throw.
        null
    }
}