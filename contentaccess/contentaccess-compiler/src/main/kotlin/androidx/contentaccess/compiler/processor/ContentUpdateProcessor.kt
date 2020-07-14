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
import androidx.contentaccess.ContentUpdate
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.compiler.vo.ContentUpdateVO
import androidx.contentaccess.compiler.vo.SelectionVO
import androidx.contentaccess.ext.hasAnnotation
import androidx.contentaccess.ext.isSuspendFunction
import androidx.contentaccess.ext.toAnnotationBox
import boxIfPrimitive
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import isInt
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class ContentUpdateProcessor(
    private val contentEntity: ContentEntityVO?,
    private val method: ExecutableElement,
    private val contentUpdateAnnotation: ContentUpdate,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {
    @KotlinPoetMetadataPreview
    fun process(): ContentUpdateVO? {
        val isSuspendFunction = method.isSuspendFunction(processingEnv)
        val types = processingEnv.typeUtils
        val potentialContentEntity = method.toAnnotationBox(ContentUpdate::class)!!
            .getAsTypeMirror("contentEntity")!!
        val resolvedContentEntity = if (!potentialContentEntity.isVoidObject()) {
            ContentEntityProcessor(potentialContentEntity,
                processingEnv, errorReporter).processEntity()
        } else {
            contentEntity
        }
        if (resolvedContentEntity == null) {
            errorReporter.reportError(missingEntityOnMethod(method.simpleName.toString()), method)
            return null
        }
        val toBeUsedUri = determineToBeUsedUri(resolvedContentEntity, contentUpdateAnnotation.uri,
            errorReporter, method)
        if (toBeUsedUri.isEmpty()) {
            errorReporter.reportError(missingUriOnMethod(), method)
        }
        if (!method.returnType.isInt()) {
            errorReporter.reportError(contentUpdateAnnotatedMethodNotReturningAnInteger(), method)
        }
        val entitiesInParams = mutableListOf<String>()
        for (param in method.parameters) {
            if (types.isSameType(param.asType(), resolvedContentEntity.type)) {
                entitiesInParams.add(param.simpleName.toString())
            }
        }
        if (entitiesInParams.size > 1) {
            // TODO(obenabde) we could in theory also support updating a list of entities
            //  but that would mean multiple operations through the content resolver and may not
            //  happen atomically. Anyhow it would be easier to do the other way through
            //  parameters annotated with @ContentColumn. Explore this further later on, although I
            //  doubt we need to worry about this so much for the updates
            errorReporter.reportError(updatingMultipleEntitiesAtTheSameType
                (resolvedContentEntity.type.toString(), method.simpleName.toString()),
                method)
            return null
        }
        if (entitiesInParams.size == 1) {
            // Assume the user wants to update a single entity
            if (contentUpdateAnnotation.where.isNotEmpty()) {
                errorReporter.reportError(methodSpecifiesWhereClauseWhenUpdatingUsingEntity
                    (entitiesInParams.first().toString()), method)
                return null
            }
            val primaryKeyColumnName = resolvedContentEntity.primaryKeyColumn.columnName
            val primaryKeyVariableName = resolvedContentEntity.primaryKeyColumn.name
            val whereClause = "$primaryKeyColumnName = \${${entitiesInParams.first()}" +
                    ".$primaryKeyVariableName}"
            val updateList = mutableListOf<Pair<String, String>>()
            for (entityColumn in resolvedContentEntity.columns.values) {
                if (entityColumn.columnName != primaryKeyColumnName) {
                    val contentValue = Pair(
                        entityColumn.columnName,
                        "${entitiesInParams.first()}.${entityColumn.name}"
                    )
                    updateList.add(contentValue)
                }
            }
            return ContentUpdateVO(
                method.simpleName.toString(),
                updateList,
                SelectionVO(whereClause, emptyList()),
                toBeUsedUri,
                method,
                isSuspendFunction
            )
        }
        val paramsNamesAndTypes = HashMap<String, TypeMirror>()
        for (param in method.parameters) {
            paramsNamesAndTypes.put(param.simpleName.toString(), param.asType())
        }
        val selectionVO = if (contentUpdateAnnotation.where.isEmpty()) {
            null
        } else {
            SelectionProcessor(method, contentUpdateAnnotation.where,
                paramsNamesAndTypes, errorReporter, resolvedContentEntity).process()
        }
        val contentValues = mutableListOf<Pair<String, String>>()
        var foundContentColumnAnnotatedParameters = false
        for (param in method.parameters) {
            if (param.hasAnnotation(ContentColumn::class)) {
                foundContentColumnAnnotatedParameters = true
                val columnName = param.getAnnotation(ContentColumn::class.java).columnName
                if (!resolvedContentEntity.columns.containsKey(columnName)) {
                    errorReporter.reportError(columnInContentUpdateParametersNotInEntity(
                        param.simpleName.toString(), columnName,
                            resolvedContentEntity.type.toString()), method)
                } else if (param.asType().boxIfPrimitive(processingEnv) != resolvedContentEntity
                        .columns.get(columnName)!!.type.boxIfPrimitive(processingEnv)) {
                    errorReporter.reportError(
                        mismatchedColumnTypeForColumnToBeUpdated(param.simpleName.toString(),
                            columnName, param.asType().toString(), resolvedContentEntity.type
                                .toString(), resolvedContentEntity.columns.get(columnName)!!.type
                                .toString()),
                        method)
                } else if (fieldIsNullable(param) && !resolvedContentEntity
                        .columns.get(columnName)!!.isNullable) {
                    errorReporter.reportError(nullableUpdateParamForNonNullableEntityColumn(
                        param.simpleName.toString(), columnName,
                        resolvedContentEntity.type.toString()), method)
                } else {
                    contentValues.add(Pair(columnName, param.simpleName.toString()))
                }
            }
        }
        if (!foundContentColumnAnnotatedParameters) {
            errorReporter.reportError(unsureWhatToUpdate(), method)
        }
        return ContentUpdateVO(
            method.simpleName.toString(),
            contentValues,
            selectionVO,
            toBeUsedUri,
            method,
            isSuspendFunction
        )
    }
}