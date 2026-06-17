/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3.processor

import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XPropertyElement
import androidx.room3.vo.Warning
import kotlin.reflect.KClass

/** A visitor that reads SuppressWarnings annotations and keeps the ones we know about. */
object SuppressWarningProcessor {

    fun getSuppressedWarnings(element: XElement): Set<Warning> = buildSet {
        element.getAnnotationOnElementOrField(SuppressWarnings::class)?.let {
            addAll(it.getAsStringList("value").mapNotNull(Warning.Companion::fromPublicKey))
        }
        element.getAnnotationOnElementOrField(Suppress::class)?.let {
            addAll(it.getAsStringList("names").mapNotNull(Warning.Companion::fromPublicKey))
        }
    }

    private fun <T : Annotation> XElement.getAnnotationOnElementOrField(annotation: KClass<T>) =
        if (this is XPropertyElement) {
            getAnnotation(annotation) ?: backingField?.getAnnotation(annotation)
        } else {
            getAnnotation(annotation)
        }
}
