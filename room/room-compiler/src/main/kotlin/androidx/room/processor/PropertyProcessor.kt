/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.ColumnInfo
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XType
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Property
import java.util.Locale

class PropertyProcessor(
    baseContext: Context,
    val containing: XType,
    val element: XFieldElement,
    val bindingScope: BindingScope,
    val propertyParent:
        EmbeddedProperty?, // pass only if this is processed as a child of Embedded property
    val onBindingError: (property: Property, errorMsg: String) -> Unit
) {
    val context = baseContext.fork(element)

    fun process(): Property {
        val member = element.asMemberOf(containing)
        val columnInfoAnnotation = element.getAnnotation(ColumnInfo::class)
        val elementName = element.name
        val annotationColumnName = columnInfoAnnotation?.get("name")?.asString()
        val rawCName =
            if (
                annotationColumnName != null &&
                    annotationColumnName != ColumnInfo.INHERIT_FIELD_NAME
            ) {
                annotationColumnName
            } else {
                elementName
            }
        val columnName = (propertyParent?.prefix ?: "") + rawCName
        val affinity =
            try {
                val affinityInt =
                    columnInfoAnnotation?.get("typeAffinity")?.asInt() ?: ColumnInfo.UNDEFINED
                SQLTypeAffinity.fromAnnotationValue(affinityInt)
            } catch (ex: NumberFormatException) {
                null
            }

        context.checker.notBlank(columnName, element, ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
        context.checker.notUnbound(
            member,
            element,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_PROPERTIES
        )

        val adapter =
            context.typeAdapterStore.findColumnTypeAdapter(
                member,
                affinity,
                skipDefaultConverter = false
            )
        val adapterAffinity = adapter?.typeAffinity ?: affinity
        val nonNull = Property.calcNonNull(member, propertyParent)
        val collateInt = columnInfoAnnotation?.get("collate")?.asInt() ?: ColumnInfo.UNSPECIFIED
        val property =
            Property(
                name = elementName,
                type = member,
                element = element,
                columnName = columnName,
                affinity = affinity,
                collate = Collate.fromAnnotationValue(collateInt),
                defaultValue =
                    extractDefaultValue(
                        columnInfoAnnotation?.get("defaultValue")?.asString(),
                        adapterAffinity,
                        nonNull
                    ),
                parent = propertyParent,
                indexed = columnInfoAnnotation?.get("index")?.asBoolean() == true,
                nonNull = nonNull
            )

        // TODO(b/273592453): Figure out a way to detect value classes in KAPT and guard against it.
        if (
            member.typeElement?.isValueClass() == true &&
                context.codeLanguage != CodeLanguage.KOTLIN
        ) {
            onBindingError(property, ProcessorErrors.VALUE_CLASS_ONLY_SUPPORTED_IN_KSP)
        }

        when (bindingScope) {
            BindingScope.TWO_WAY -> {
                property.statementBinder = adapter
                property.statementValueReader = adapter
                property.affinity = adapterAffinity
                if (adapter == null) {
                    onBindingError(property, ProcessorErrors.CANNOT_FIND_COLUMN_TYPE_ADAPTER)
                }
            }
            BindingScope.BIND_TO_STMT -> {
                property.statementBinder =
                    context.typeAdapterStore.findStatementValueBinder(
                        property.type,
                        property.affinity
                    )
                if (property.statementBinder == null) {
                    onBindingError(property, ProcessorErrors.CANNOT_FIND_STMT_BINDER)
                }
            }
            BindingScope.READ_FROM_STMT -> {
                property.statementValueReader =
                    context.typeAdapterStore.findStatementValueReader(
                        property.type,
                        property.affinity
                    )
                if (property.statementValueReader == null) {
                    onBindingError(property, ProcessorErrors.CANNOT_FIND_STMT_READER)
                }
            }
        }

        return property
    }

    private fun extractDefaultValue(
        value: String?,
        affinity: SQLTypeAffinity?,
        propertyNonNull: Boolean
    ): String? {
        if (value == null) {
            return null
        }
        val trimmed = value.trim().lowercase(Locale.ENGLISH)
        val defaultValue =
            if (affinity == SQLTypeAffinity.TEXT) {
                if (value == ColumnInfo.VALUE_UNSPECIFIED) {
                    null
                } else if (trimmed.startsWith("(") || trimmed in SQLITE_VALUE_CONSTANTS) {
                    value
                } else {
                    "'${value.trim('\'')}'"
                }
            } else {
                if (value == ColumnInfo.VALUE_UNSPECIFIED || trimmed == "") {
                    null
                } else {
                    value
                }
            }
        if (trimmed == "null" && propertyNonNull) {
            context.logger.e(element, ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
        }
        return defaultValue
    }

    /** Defines what we need to assign */
    enum class BindingScope {
        TWO_WAY, // both bind and read.
        BIND_TO_STMT, // just value to statement
        READ_FROM_STMT // just statement to value
    }
}

internal val SQLITE_VALUE_CONSTANTS =
    listOf("null", "current_time", "current_date", "current_timestamp", "true", "false")
