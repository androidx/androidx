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

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts3
import androidx.room.Fts4
import androidx.room.ext.AnnotationBox
import androidx.room.ext.hasAnyOf
import androidx.room.vo.ForeignKeyAction
import androidx.room.vo.Index
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

interface EntityProcessor : EntityOrViewProcessor {
    override fun process(): androidx.room.vo.Entity

    companion object {
        fun extractTableName(element: TypeElement, annotation: Entity): String {
            return if (annotation.tableName == "") {
                element.simpleName.toString()
            } else {
                annotation.tableName
            }
        }

        fun extractIndices(annotation: AnnotationBox<Entity>, tableName: String): List<IndexInput> {
            return annotation.getAsAnnotationBox<androidx.room.Index>("indices").map {
                val indexAnnotation = it.value
                val nameValue = indexAnnotation.name
                val name = if (nameValue == "") {
                    createIndexName(indexAnnotation.value.asList(), tableName)
                } else {
                    nameValue
                }
                IndexInput(name, indexAnnotation.unique, indexAnnotation.value.asList())
            }
        }

        fun createIndexName(columnNames: List<String>, tableName: String): String {
            return Index.DEFAULT_PREFIX + tableName + "_" + columnNames.joinToString("_")
        }

        fun extractForeignKeys(annotation: AnnotationBox<Entity>): List<ForeignKeyInput> {
            return annotation.getAsAnnotationBox<ForeignKey>("foreignKeys")
                    .mapNotNull { annotationBox ->
                val foreignKey = annotationBox.value
                val parent = annotationBox.getAsTypeMirror("entity")
                if (parent != null) {
                    ForeignKeyInput(
                            parent = parent,
                            parentColumns = foreignKey.parentColumns.asList(),
                            childColumns = foreignKey.childColumns.asList(),
                            onDelete = ForeignKeyAction.fromAnnotationValue(foreignKey.onDelete),
                            onUpdate = ForeignKeyAction.fromAnnotationValue(foreignKey.onUpdate),
                            deferred = foreignKey.deferred)
                } else {
                    null
                }
            }
        }
    }
}

/**
 * Processed Index annotation output.
 */
data class IndexInput(val name: String, val unique: Boolean, val columnNames: List<String>)

/**
 * ForeignKey, before it is processed in the context of a database.
 */
data class ForeignKeyInput(
    val parent: TypeMirror,
    val parentColumns: List<String>,
    val childColumns: List<String>,
    val onDelete: ForeignKeyAction?,
    val onUpdate: ForeignKeyAction?,
    val deferred: Boolean
)

fun EntityProcessor(
    context: Context,
    element: TypeElement,
    referenceStack: LinkedHashSet<Name> = LinkedHashSet()
): EntityProcessor {
    return if (element.hasAnyOf(Fts3::class, Fts4::class)) {
        FtsTableEntityProcessor(context, element, referenceStack)
    } else {
        TableEntityProcessor(context, element, referenceStack)
    }
}