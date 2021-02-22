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

package androidx.serialization.compiler.processing.ext

import javax.annotation.processing.Messager
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.WARNING
import kotlin.reflect.KClass

/** Print [message] as a warning with optional positional information. */
internal inline fun Messager.warn(
    element: Element? = null,
    annotation: AnnotationMirror? = null,
    annotationValue: AnnotationValue? = null,
    message: () -> String
) {
    printMessage(WARNING, message(), element, annotation, annotationValue)
}

/** Print [message] as an error with optional positional information. */
internal inline fun Messager.error(
    element: Element? = null,
    annotation: AnnotationMirror? = null,
    annotationValue: AnnotationValue? = null,
    message: () -> String
) {
    printMessage(ERROR, message(), element, annotation, annotationValue)
}

/** Print [message] as an error on the annotation of type [annotationClass] on [element]. */
internal inline fun Messager.error(
    element: Element,
    annotationClass: KClass<out Annotation>,
    message: () -> String
) {
    printMessage(ERROR, message(), element, element[annotationClass])
}
