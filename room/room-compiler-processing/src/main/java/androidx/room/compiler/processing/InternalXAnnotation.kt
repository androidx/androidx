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

internal fun XAnnotation.unwrapRepeatedAnnotationsFromContainer(): List<XAnnotation>? {
    val nestedAnnotations = try {
        getAsAnnotationList("value")
    } catch (e: Throwable) {
        return null
    }

    // Ideally we would read the value of the Repeatable annotation to get the container class
    // type and check that it matches "this" type. However, there seems to be a KSP bug where
    // the value of Repeatable is not present so the best we can do is check that all the nested
    // members are annotated with repeatable.
    val isRepeatable = nestedAnnotations.all {
        it.type.typeElement?.hasAnnotation(Repeatable::class) == true
    }
    return if (isRepeatable) nestedAnnotations else null
}