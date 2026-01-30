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

package androidx.room3.processor

import androidx.room3.ForeignKey.Companion.NO_ACTION
import androidx.room3.Fts3
import androidx.room3.Fts4
import androidx.room3.compiler.processing.XAnnotation
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.vo.ForeignKeyAction
import androidx.room3.vo.Index

interface EntityProcessor : EntityOrViewProcessor {
    override fun process(): androidx.room3.vo.Entity

    companion object {
        fun extractTableName(element: XTypeElement, annotation: XAnnotation): String {
            val tableName = annotation["tableName"]?.asString()
            return if (tableName == null || tableName == "") {
                element.name
            } else {
                tableName
            }
        }

        fun extractIndices(annotation: XAnnotation, tableName: String): List<IndexInput> {
            val indicesAnnotations = annotation["indices"]?.asAnnotationList() ?: emptyList()
            return indicesAnnotations.map { indexAnnotation ->
                val unique = indexAnnotation["unique"]?.asBoolean() ?: false
                val nameValue = indexAnnotation["name"]?.asString() ?: ""
                val columns = indexAnnotation["value"]?.asStringList() ?: emptyList()
                val orders =
                    (indexAnnotation["orders"]?.asEnumList() ?: emptyList()).map {
                        androidx.room3.Index.Order.valueOf(it.name)
                    }
                val name =
                    if (nameValue == "") {
                        createIndexName(columns, tableName)
                    } else {
                        nameValue
                    }
                IndexInput(name, unique, columns, orders)
            }
        }

        fun createIndexName(columnNames: List<String>, tableName: String): String {
            return Index.DEFAULT_PREFIX + tableName + "_" + columnNames.joinToString("_")
        }

        fun extractForeignKeys(annotation: XAnnotation): List<ForeignKeyInput> {
            val foreignKeyAnnotations = annotation["foreignKeys"]?.asAnnotationList() ?: emptyList()
            return foreignKeyAnnotations.map { foreignKeyAnnotation ->
                ForeignKeyInput(
                    parent = foreignKeyAnnotation.getAsType("entity"),
                    parentColumns = foreignKeyAnnotation.getAsStringList("parentColumns"),
                    childColumns = foreignKeyAnnotation.getAsStringList("childColumns"),
                    onDelete =
                        ForeignKeyAction.fromAnnotationValue(
                            foreignKeyAnnotation["onDelete"]?.asInt() ?: NO_ACTION
                        ),
                    onUpdate =
                        ForeignKeyAction.fromAnnotationValue(
                            foreignKeyAnnotation["onUpdate"]?.asInt() ?: NO_ACTION
                        ),
                    deferred = foreignKeyAnnotation["deferred"]?.asBoolean() ?: false,
                )
            }
        }
    }
}

/** Processed Index annotation output. */
data class IndexInput(
    val name: String,
    val unique: Boolean,
    val columnNames: List<String>,
    val orders: List<androidx.room3.Index.Order>,
)

/** ForeignKey, before it is processed in the context of a database. */
data class ForeignKeyInput(
    val parent: XType,
    val parentColumns: List<String>,
    val childColumns: List<String>,
    val onDelete: ForeignKeyAction?,
    val onUpdate: ForeignKeyAction?,
    val deferred: Boolean,
)

fun EntityProcessor(
    context: Context,
    element: XTypeElement,
    referenceStack: LinkedHashSet<String> = LinkedHashSet(),
): EntityProcessor {
    return if (element.hasAnyAnnotation(Fts3::class, Fts4::class)) {
        FtsTableEntityProcessor(context, element, referenceStack)
    } else {
        TableEntityProcessor(context, element, referenceStack)
    }
}
