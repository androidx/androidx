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

package androidx.room.vo

import javax.lang.model.element.ExecutableElement

/**
 * For each Entity / Pojo we process has a constructor. It might be the empty constructor or a
 * constructor with fields.
 */
data class Constructor(val element: ExecutableElement, val params: List<Param>) {

    fun hasField(field: Field): Boolean {
        return params.any {
            when (it) {
                is FieldParam -> it.field === field
                is EmbeddedParam -> it.embedded.field === field
                else -> false
            }
        }
    }

    class FieldParam(val field: Field) : Param(ParamType.FIELD) {
        override fun log(): String = field.getPath()
    }

    class EmbeddedParam(val embedded: EmbeddedField) : Param(ParamType.EMBEDDED) {
        override fun log(): String = embedded.field.getPath()
    }

    abstract class Param(val type: ParamType) {
        abstract fun log(): String
    }

    enum class ParamType {
        FIELD,
        EMBEDDED
    }
}
