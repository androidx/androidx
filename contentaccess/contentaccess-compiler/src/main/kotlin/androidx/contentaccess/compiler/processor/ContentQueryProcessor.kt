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

import androidx.contentaccess.ContentQuery
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.compiler.vo.ContentQueryVO
import androidx.contentaccess.compiler.vo.PojoVO
import androidx.contentaccess.ext.getSuspendFunctionReturnType
import androidx.contentaccess.ext.hasMoreThanOneNonPrivateNonIgnoredConstructor
import androidx.contentaccess.ext.isFilledThroughConstructor
import androidx.contentaccess.ext.isNotInstantiable
import androidx.contentaccess.ext.isSuspendFunction
import androidx.contentaccess.ext.toAnnotationBox
import asTypeElement
import boxIfPrimitive
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import extractIntendedReturnType
import isPrimitive
import isString
import isSupportedColumnType
import isSupportedGenericType
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class ContentQueryProcessor(
    private val contentEntity: ContentEntityVO?,
    private val method: ExecutableElement,
    private val contentQueryAnnotation: ContentQuery,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {
    @KotlinPoetMetadataPreview
    fun process(): ContentQueryVO? {
        val isSuspendFunction = method.isSuspendFunction(processingEnv)
        val returnType = if (isSuspendFunction) {
            method.getSuspendFunctionReturnType()
        } else {
            method.returnType
        }
        val types = processingEnv.typeUtils
        val potentialContentEntity = method.toAnnotationBox(ContentQuery::class)!!
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
        val toBeUsedUri = determineToBeUsedUri(resolvedContentEntity, contentQueryAnnotation.uri,
            errorReporter, method)
        if (toBeUsedUri.isEmpty()) {
            errorReporter.reportError(missingUriOnMethod(), method)
        }
        val orderBy = if (contentQueryAnnotation.orderBy.isNotEmpty()) {
            for (orderByMember in contentQueryAnnotation.orderBy) {
                val trimmedOrderByMember = orderByMember.trim()
                if (!resolvedContentEntity.columns.containsKey(trimmedOrderByMember)) {
                    // Maybe it was "column desc" or "column asc", check.
                    val splitOrderBy = trimmedOrderByMember.split(" ")
                    if (splitOrderBy.size == 2 && resolvedContentEntity.columns.containsKey
                            (splitOrderBy.get(0)) && ORDER_BY_KEYWORDS.contains(splitOrderBy
                            .get(1))) {
                        continue
                    }
                    errorReporter.reportError(badlyFormulatedOrderBy(orderByMember), method)
                }
            }
            contentQueryAnnotation.orderBy.joinToString(" ")
        } else {
            ""
        }
        val paramsNamesAndTypes = HashMap<String, TypeMirror>()
        for (param in method.parameters) {
            paramsNamesAndTypes.put(param.simpleName.toString(), param.asType())
        }
        val selectionVO = if (contentQueryAnnotation.selection.isEmpty()) {
            null
        } else {
            SelectionProcessor(method, contentQueryAnnotation.selection,
                paramsNamesAndTypes, errorReporter, resolvedContentEntity).process()
        }
        if (contentQueryAnnotation.projection.size == 1 &&
            returnType.extractIntendedReturnType().isSupportedColumnType()) {
            val queriedColumn = contentQueryAnnotation.projection[0]
            if (!resolvedContentEntity.columns.containsKey(queriedColumn)) {
                errorReporter.reportError(queriedColumnInProjectionNotInEntity(queriedColumn,
                    resolvedContentEntity.type.toString()), method)
                return null
            }
            val queriedColumnType = resolvedContentEntity.columns.get(queriedColumn)!!.type
            if (queriedColumnType.boxIfPrimitive(processingEnv)
                != returnType.boxIfPrimitive(processingEnv) &&
                ((!returnType.isSupportedGenericType() ||
                    !processingEnv.typeUtils
                        .isSameType(returnType.extractIntendedReturnType(),
                            queriedColumnType.boxIfPrimitive(processingEnv))))) {
                errorReporter.reportError(
                    queriedColumnInProjectionTypeDoesntMatchReturnType(returnType.toString(),
                        queriedColumnType.toString(), queriedColumn),
                    method
                )
            }
            if (errorReporter.errorReported) {
                return null
            }
            return ContentQueryVO(
                name = method.simpleName.toString(),
                toQueryFor = listOf(resolvedContentEntity.columns.get(queriedColumn)!!),
                selection = selectionVO,
                uri = toBeUsedUri,
                returnType = returnType,
                method = method,
                orderBy = orderBy,
                isSuspend = isSuspendFunction)
        } else {
            // Either empty projection or more than one field, either way infer the return columns
            // from the return type POJO/entity.
            if (types.isSameType(returnType, resolvedContentEntity.type)) {
                // TODO(obenabde): no need for projection with entity.
                return ContentQueryVO(
                    name = method.simpleName.toString(),
                    toQueryFor = resolvedContentEntity.columns.map { it.value },
                    selection = selectionVO,
                    orderBy = orderBy,
                    uri = toBeUsedUri,
                    returnType = returnType,
                    method = method,
                    isSuspend = isSuspendFunction)
            }

            val intendedReturnType = if (returnType.isSupportedGenericType()) {
                returnType.extractIntendedReturnType()
            } else {
                returnType
            }

            if (intendedReturnType.asTypeElement()
                    .hasMoreThanOneNonPrivateNonIgnoredConstructor()) {
                errorReporter.reportError(
                    pojoHasMoreThanOneQualifyingConstructor(intendedReturnType.toString()),
                    intendedReturnType.asTypeElement())
                return null
            } else if (intendedReturnType.asTypeElement().isNotInstantiable()) {
                errorReporter.reportError(pojoIsNotInstantiable(intendedReturnType.asTypeElement()
                    .qualifiedName.toString()), intendedReturnType.asTypeElement())
                return null
            }
            val pojo = PojoProcessor(intendedReturnType).process()
            // Apply the projection (if existing) to the POJO
            val pojoWithProjection = validateAndApplyProjectionToPojo(contentQueryAnnotation
                .projection, pojo, returnType, resolvedContentEntity)
            if (errorReporter.errorReported) {
                return null
            }
            pojoWithProjection!!.pojoFields.forEach {
                if (it.isNullable && it.type.isPrimitive()) {
                    errorReporter.reportError(pojoWithNullablePrimitive(it.name,
                        pojo.type.toString()), method)
                }
            }
            return ContentQueryVO(
                name = method.simpleName.toString(),
                toQueryFor = pojoWithProjection.pojoFields.map {
                    resolvedContentEntity.columns[it.columnName]!!
                },
                selection = selectionVO,
                uri = toBeUsedUri,
                returnType = returnType,
                method = method,
                orderBy = orderBy,
                isSuspend = isSuspendFunction
            )
        }
    }

    // Returns whether the pojo's fields names and types match ones in the entity being queried.
    private fun validatePojoCorrectnessAgainstEntity(
        pojo: PojoVO,
        resolvedContentEntity: ContentEntityVO
    ) {
        pojo.pojoFields.forEach { field ->
            if (!resolvedContentEntity.columns.containsKey(field.columnName) || !processingEnv
                .typeUtils.isSameType(resolvedContentEntity.columns.get(field.columnName)!!
                    .type.boxIfPrimitive(processingEnv),
                    field.type.boxIfPrimitive(processingEnv)
                )) {
                errorReporter.reportError(pojoFieldNotInEntity(field.name, field.type.toString(),
                    field.columnName, pojo.type.toString(), resolvedContentEntity.type.toString()),
                    method)
            } else {
                if (resolvedContentEntity.columns.get(field.columnName)!!.isNullable &&
                    !field.isNullable) {
                    // TODO(obenabde): clarify how to mark as nullable, i.e "?" for Kotlin and
                    // @androidx.annotations.Nullable for Java.
                    errorReporter.reportError(nullableEntityColumnNotNullableInPojo(
                        field.name,
                        field.type.toString(),
                        field.columnName,
                        resolvedContentEntity.type.toString()
                    ), method)
                }
            }
        }
    }

    private fun validateAndApplyProjectionToPojo(
        projection: Array<String>,
        pojo: PojoVO,
        returnType: TypeMirror,
        resolvedContentEntity: ContentEntityVO
    ): PojoVO? {
        // alert now.
        if (projection.isEmpty()) {
            validatePojoCorrectnessAgainstEntity(pojo, resolvedContentEntity)
            return pojo
        }
        var errorFound = false
        val extractedColumnNames = pojo.pojoFields.map { it.columnName to it.isNullable }.toMap()
        for (column in projection) {
            if (!extractedColumnNames.containsKey(column)) {
                errorFound = true
                errorReporter.reportError(columnInProjectionNotIncludedInReturnPojo(column,
                    pojo.type.toString()), method)
            }
        }
        for (pojoField in pojo.pojoFields) {
            if (!projection.contains(pojoField.columnName) && !pojoField.isNullable &&
                pojo.type.asTypeElement().isFilledThroughConstructor()) {
                errorFound = true
                errorReporter.reportError(
                    constructorFieldNotIncludedInProjectionNotNullable(pojoField.name, returnType
                        .extractIntendedReturnType().toString()), method)
            }
        }
        if (errorFound) {
            return null
        }
        val pojoFieldsWithProjection = pojo.pojoFields.filter { projection.contains(it.columnName) }
        validatePojoCorrectnessAgainstEntity(
            PojoVO(pojoFieldsWithProjection, pojo.type),
            resolvedContentEntity
        )
        return PojoVO(pojoFieldsWithProjection, pojo.type)
    }
}

fun determineToBeUsedUri(
    resolvedContentEntity: ContentEntityVO,
    uriInAnnotation: String,
    errorReporter: ErrorReporter,
    method: ExecutableElement
): String {
    if (uriInAnnotation.isEmpty()) {
        return resolvedContentEntity.defaultUri
    }

    if (!uriInAnnotation.startsWith(":")) {
        return uriInAnnotation
    }

    val specifiedParamName = uriInAnnotation.substring(1)
    if (specifiedParamName.isEmpty()) {
        errorReporter.reportError(columnOnlyAsUri(), method)
        return uriInAnnotation
    } else if (!method.parameters.map { it.simpleName.toString() }.contains
            (specifiedParamName)) {
        errorReporter.reportError(missingUriParameter(specifiedParamName), method)
        return uriInAnnotation
    } else if (!method.parameters.filter {
            it.simpleName.toString().equals(specifiedParamName) }[0].asType().isString()) {
        errorReporter.reportError(uriParameterIsNotString(specifiedParamName), method)
        return uriInAnnotation
    }
    return uriInAnnotation
}

internal val ORDER_BY_KEYWORDS = listOf("asc", "desc")