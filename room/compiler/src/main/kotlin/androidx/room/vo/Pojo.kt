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

import androidx.room.ext.toAnnotationBox
import androidx.room.ext.typeName
import androidx.room.processor.DatabaseViewProcessor
import androidx.room.processor.EntityProcessor
import com.squareup.javapoet.TypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

/**
 * A class is turned into a Pojo if it is used in a query response.
 */
open class Pojo(
    val element: TypeElement,
    val type: DeclaredType,
    fields: List<Field>,
    val embeddedFields: List<EmbeddedField>,
    val relations: List<Relation>,
    val constructor: Constructor? = null
) : HasFields {
    val typeName: TypeName by lazy { type.typeName() }

    override val fields = Fields(fields)

    /**
     * All table or view names that are somehow accessed by this Pojo.
     * Might be via Embedded or Relation.
     */
    fun accessedTableNames(): List<String> {
        val entityAnnotation = element.toAnnotationBox(androidx.room.Entity::class)
        return if (entityAnnotation != null) {
            listOf(EntityProcessor.extractTableName(element, entityAnnotation.value))
        } else {
            val viewAnnotation = element.toAnnotationBox(androidx.room.DatabaseView::class)
            if (viewAnnotation != null) {
                listOf(DatabaseViewProcessor.extractViewName(element, viewAnnotation.value))
            } else {
                emptyList()
            } + embeddedFields.flatMap {
                it.pojo.accessedTableNames()
            } + relations.map {
                it.entity.tableName
            }
        }
    }
}
