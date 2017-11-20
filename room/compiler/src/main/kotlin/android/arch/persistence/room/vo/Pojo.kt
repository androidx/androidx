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

package android.arch.persistence.room.vo

import android.arch.persistence.room.ext.typeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/**
 * A class is turned into a Pojo if it is used in a query response.
 */
// TODO make data class when move to kotlin 1.1
open class Pojo(val element: TypeElement, val type: DeclaredType, val fields: List<Field>,
                val embeddedFields: List<EmbeddedField>, val relations: List<Relation>,
                val constructor: Constructor? = null) {
    val typeName: TypeName by lazy { type.typeName() }
}
