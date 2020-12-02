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

package androidx.serialization.compiler.processing.parsers

import androidx.serialization.Reserved.IdRange
import androidx.serialization.compiler.processing.ext.asAnnotationMirror
import androidx.serialization.compiler.processing.ext.asInt
import androidx.serialization.compiler.processing.ext.asList
import androidx.serialization.compiler.processing.ext.asString
import androidx.serialization.compiler.processing.ext.get
import androidx.serialization.compiler.models.Reserved
import javax.lang.model.element.TypeElement
import androidx.serialization.Reserved as ReservedAnnotation

/**
 * Extract the data from a [androidx.serialization.Reserved] annotation on [element].
 *
 * If no annotation is present, this returns an empty reserved data class. If it encounters an
 * [IdRange] with its `from` greater than its `to`, it reverses them before converting them to an
 * [IntRange], reserving the same range of IDs as if they had been correctly placed.
 */
internal fun parseReserved(element: TypeElement): Reserved {
    return when (val reserved = element[ReservedAnnotation::class]) {
        null -> Reserved.empty()
        else -> Reserved(
            ids = reserved["ids"].asList().mapTo(mutableSetOf()) { it.asInt() },
            names = reserved["names"].asList().mapTo(mutableSetOf()) { it.asString() },
            idRanges = reserved["idRanges"].asList().mapTo(mutableSetOf()) { annotationValue ->
                val idRange = annotationValue.asAnnotationMirror()
                val from = idRange["from"].asInt()
                val to = idRange["to"].asInt()

                when {
                    from <= to -> from..to
                    else -> to..from
                }
            }
        )
    }
}
