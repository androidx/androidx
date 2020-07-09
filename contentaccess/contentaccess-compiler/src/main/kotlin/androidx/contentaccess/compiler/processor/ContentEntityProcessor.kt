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

package androidx.contentaccess.compiler.processor

import androidx.contentaccess.ContentColumn
import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentPrimaryKey
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentColumnVO
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.ext.getAllConstructorParamsOrPublicFields
import androidx.contentaccess.ext.hasAnnotation
import androidx.contentaccess.ext.hasMoreThanOneNonPrivateNonIgnoredConstructor
import androidx.contentaccess.ext.isNotInstantiable
import asTypeElement
import com.google.auto.common.MoreTypes
import isPrimitive
import isSupportedColumnType
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

class ContentEntityProcessor(
    private val contentEntity: TypeMirror,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {

    fun processEntity(): ContentEntityVO? {
        val entity = contentEntity.asTypeElement()
        if (entity.hasMoreThanOneNonPrivateNonIgnoredConstructor()) {
            errorReporter.reportError(entityWithMultipleConstructors(contentEntity.toString()),
                entity)
            return null
        } else if (entity.isNotInstantiable()) {
            errorReporter.reportError(nonInstantiableEntity(contentEntity.toString()), entity)
            return null
        }
        val columns = entity.getAllConstructorParamsOrPublicFields()
        columns.forEach {
            if (fieldIsNullable(it) && it.asType().isPrimitive()) {
                errorReporter.reportError(entityWithNullablePrimitiveType(it.simpleName.toString(),
                    contentEntity.toString()), it)
            }
        }
        val contentColumns = HashMap<String, ContentColumnVO>()
        val contentPrimaryKey = ArrayList<ContentColumnVO>()
        columns.forEach { column ->
            if (column.hasAnnotation(ContentColumn::class) &&
                column.hasAnnotation(ContentPrimaryKey::class)) {
                errorReporter.reportError(entityFieldWithBothAnnotations(column.simpleName
                    .toString(), entity.qualifiedName.toString()), entity)
            } else if (column.hasAnnotation(ContentColumn::class)) {
                if (validateColumnType(column, errorReporter)) {
                    val vo = ContentColumnVO(
                        column.simpleName.toString(), column.asType(),
                        column.getAnnotation(ContentColumn::class.java).columnName,
                        fieldIsNullable(column)
                    )
                    contentColumns.put(vo.columnName, vo)
                }
            } else if (column.hasAnnotation(ContentPrimaryKey::class)) {
                if (validateColumnType(column, errorReporter)) {
                    val vo = ContentColumnVO(column.simpleName.toString(), column.asType(), column
                        .getAnnotation(ContentPrimaryKey::class.java).columnName,
                        fieldIsNullable(column)
                    )
                    contentColumns.put(vo.columnName, vo)
                    contentPrimaryKey.add(vo)
                }
            } else {
                errorReporter.reportError(
                    missingAnnotationOnEntityFieldErrorMessage(
                        column.simpleName.toString(),
                        entity.qualifiedName.toString()
                    ),
                    entity
                )
            }
        }
        if (contentPrimaryKey.isEmpty()) {
            if (columns.isEmpty()) {
                errorReporter.reportError(missingFieldsInContentEntityErrorMessage(entity
                    .qualifiedName.toString()), entity)
            } else {
                errorReporter.reportError(missingEntityPrimaryKeyErrorMessage(entity
                    .qualifiedName.toString()), entity)
            }
        }
        if (contentPrimaryKey.size > 1) {
            errorReporter.reportError(
                entityWithMultiplePrimaryKeys(entity.qualifiedName.toString()),
                entity
            )
        }
        if (errorReporter.errorReported) {
            return null
        }
        return ContentEntityVO(entity.getAnnotation(ContentEntity::class.java).uri, MoreTypes
            .asDeclared(entity.asType()), contentColumns, contentPrimaryKey.first())
    }

    fun validateColumnType(column: VariableElement, errorReporter: ErrorReporter): Boolean {
        if (!column.asType().isSupportedColumnType()) {
            errorReporter.reportError(
                unsupportedColumnType(column.simpleName.toString(),
                    contentEntity.toString(),
                    column.asType().toString()
                ), column)
            return false
        }
        return true
    }
}

fun fieldIsNullable(field: VariableElement): Boolean {
    return field.annotationMirrors.any { NULLABLE_ANNOTATIONS.contains(it.toString()) }
}

val NULLABLE_ANNOTATIONS = listOf(
    "@org.jetbrains.annotations.Nullable",
    "@androidx.annotation.Nullable"
)
