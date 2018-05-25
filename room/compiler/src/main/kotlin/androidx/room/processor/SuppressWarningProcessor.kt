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

package androidx.room.processor

import androidx.room.vo.Warning
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.util.SimpleAnnotationValueVisitor6

/**
 * A visitor that reads SuppressWarnings annotations and keeps the ones we know about.
 */
object SuppressWarningProcessor {

    fun getSuppressedWarnings(element: Element): Set<Warning> {
        val annotation = MoreElements.getAnnotationMirror(element,
                SuppressWarnings::class.java).orNull()
        return if (annotation == null) {
            emptySet()
        } else {
            val value = AnnotationMirrors.getAnnotationValue(annotation, "value")
            if (value == null) {
                emptySet()
            } else {
                VISITOR.visit(value)
            }
        }
    }

    private object VISITOR : SimpleAnnotationValueVisitor6<Set<Warning>, String>() {
        override fun visitArray(values: List<AnnotationValue>?, elementName: String?
        ): Set<Warning> {
            return values?.mapNotNull {
                Warning.fromPublicKey(it.value.toString())
            }?.toSet() ?: emptySet()
        }

        override fun defaultAction(o: Any?, p: String?): Set<Warning> {
            return emptySet()
        }
    }
}
