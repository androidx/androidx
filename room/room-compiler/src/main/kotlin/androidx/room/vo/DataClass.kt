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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.processor.DatabaseViewProcessor
import androidx.room.processor.EntityProcessor

/** A class is turned into a data class if it is used in a query response. */
open class DataClass(
    val element: XTypeElement,
    val type: XType,
    properties: List<Property>,
    val embeddedProperties: List<EmbeddedProperty>,
    val relations: List<Relation>,
    val constructor: Constructor? = null
) : HasProperties {
    val className: XClassName by lazy { element.asClassName() }
    val typeName: XTypeName by lazy { type.asTypeName() }

    override val properties = Properties(properties)

    /**
     * All table or view names that are somehow accessed by this data class. Might be via Embedded
     * or Relation.
     */
    fun accessedTableNames(): List<String> {
        val entityAnnotation = element.getAnnotation(androidx.room.Entity::class)
        return if (entityAnnotation != null) {
            listOf(EntityProcessor.extractTableName(element, entityAnnotation))
        } else {
            val viewAnnotation = element.getAnnotation(androidx.room.DatabaseView::class)
            if (viewAnnotation != null) {
                listOf(DatabaseViewProcessor.extractViewName(element, viewAnnotation))
            } else {
                emptyList()
            } +
                embeddedProperties.flatMap { it.dataClass.accessedTableNames() } +
                relations.map { it.entity.tableName }
        }
    }
}
