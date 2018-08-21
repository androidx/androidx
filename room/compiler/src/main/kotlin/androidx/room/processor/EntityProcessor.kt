/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.room.Fts3Entity
import androidx.room.Fts4Entity
import androidx.room.ext.hasAnyOf
import androidx.room.vo.Entity
import com.google.auto.common.AnnotationMirrors
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

interface EntityProcessor {
    fun process(): Entity

    companion object {
        fun extractTableName(element: TypeElement, annotation: AnnotationMirror): String {
            val annotationValue = AnnotationMirrors
                    .getAnnotationValue(annotation, "tableName").value.toString()
            return if (annotationValue == "") {
                element.simpleName.toString()
            } else {
                annotationValue
            }
        }
    }
}

fun EntityProcessor(
    context: Context,
    element: TypeElement,
    referenceStack: LinkedHashSet<Name> = LinkedHashSet()
): EntityProcessor {
    return if (element.hasAnyOf(Fts3Entity::class, Fts4Entity::class)) {
        FtsTableEntityProcessor(context, element, referenceStack)
    } else {
        TableEntityProcessor(context, element, referenceStack)
    }
}