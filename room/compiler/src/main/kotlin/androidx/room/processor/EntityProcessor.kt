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

import androidx.room.Fts3
import androidx.room.Fts4
import androidx.room.ext.getAsBoolean
import androidx.room.ext.getAsInt
import androidx.room.ext.getAsString
import androidx.room.ext.getAsStringList
import androidx.room.ext.hasAnyOf
import androidx.room.ext.toType
import androidx.room.vo.Entity
import androidx.room.vo.ForeignKeyAction
import androidx.room.vo.Index
import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleAnnotationValueVisitor6

interface EntityProcessor {
    fun process(): Entity

    companion object {
        fun extractTableName(element: TypeElement, annotation: AnnotationMirror): String {
            val annotationValue = getAnnotationValue(annotation, "tableName").value.toString()
            return if (annotationValue == "") {
                element.simpleName.toString()
            } else {
                annotationValue
            }
        }

        fun extractIndices(
            annotation: AnnotationMirror,
            tableName: String
        ): List<IndexInput> {
            val arrayOfIndexAnnotations = getAnnotationValue(annotation, "indices")
            return INDEX_LIST_VISITOR.visit(arrayOfIndexAnnotations, tableName)
        }

        private val INDEX_LIST_VISITOR = object
            : SimpleAnnotationValueVisitor6<List<IndexInput>, String>() {
            override fun visitArray(
                values: MutableList<out AnnotationValue>?,
                tableName: String
            ): List<IndexInput> {
                return values?.mapNotNull {
                    INDEX_VISITOR.visit(it, tableName)
                } ?: emptyList()
            }
        }

        private val INDEX_VISITOR = object : SimpleAnnotationValueVisitor6<IndexInput?, String>() {
            override fun visitAnnotation(a: AnnotationMirror?, tableName: String): IndexInput? {
                val fieldInput = getAnnotationValue(a, "value").getAsStringList()
                val unique = getAnnotationValue(a, "unique").getAsBoolean(false)
                val nameValue = getAnnotationValue(a, "name").getAsString("")
                val name = if (nameValue == null || nameValue == "") {
                    createIndexName(fieldInput, tableName)
                } else {
                    nameValue
                }
                return IndexInput(name, unique, fieldInput)
            }
        }

        fun createIndexName(columnNames: List<String>, tableName: String): String {
            return Index.DEFAULT_PREFIX + tableName + "_" + columnNames.joinToString("_")
        }

        fun extractForeignKeys(annotation: AnnotationMirror): List<ForeignKeyInput> {
            val arrayOfForeignKeyAnnotations = getAnnotationValue(annotation, "foreignKeys")
            return FOREIGN_KEY_LIST_VISITOR.visit(arrayOfForeignKeyAnnotations)
        }

        private val FOREIGN_KEY_LIST_VISITOR = object
            : SimpleAnnotationValueVisitor6<List<ForeignKeyInput>, Void?>() {
            override fun visitArray(
                values: MutableList<out AnnotationValue>?,
                void: Void?
            ): List<ForeignKeyInput> {
                return values?.mapNotNull {
                    FOREIGN_KEY_VISITOR.visit(it)
                } ?: emptyList()
            }
        }

        private val FOREIGN_KEY_VISITOR = object : SimpleAnnotationValueVisitor6<ForeignKeyInput?,
                Void?>() {
            override fun visitAnnotation(a: AnnotationMirror?, void: Void?): ForeignKeyInput? {
                val entityClass = try {
                    getAnnotationValue(a, "entity").toType()
                } catch (notPresent: TypeNotPresentException) {
                    return null
                }
                val parentColumns = getAnnotationValue(a, "parentColumns").getAsStringList()
                val childColumns = getAnnotationValue(a, "childColumns").getAsStringList()
                val onDeleteInput = getAnnotationValue(a, "onDelete").getAsInt()
                val onUpdateInput = getAnnotationValue(a, "onUpdate").getAsInt()
                val deferred = getAnnotationValue(a, "deferred").getAsBoolean(true)
                val onDelete = ForeignKeyAction.fromAnnotationValue(onDeleteInput)
                val onUpdate = ForeignKeyAction.fromAnnotationValue(onUpdateInput)
                return ForeignKeyInput(
                        parent = entityClass,
                        parentColumns = parentColumns,
                        childColumns = childColumns,
                        onDelete = onDelete,
                        onUpdate = onUpdate,
                        deferred = deferred)
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