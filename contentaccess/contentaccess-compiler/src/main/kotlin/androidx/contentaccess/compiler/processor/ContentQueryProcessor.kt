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
import androidx.contentaccess.ext.toAnnotationBox
import boxIfPrimitive
import extractIntendedReturnType
import isString
import isSupportedColumnType
import isSupportedGenericType
import isVoidObject
import java.util.Locale
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
    fun process(): ContentQueryVO? {
        val returnType = method.returnType
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
            errorReporter.reportError("Method ${method.simpleName} has no associated entity, " +
                    "please ensure that either the content access object containing the method " +
                    "specifies an entity inside the @ContentAccessObject annotation or that the " +
                    "method specifies a content entity through the contentEntity parameter of " +
                    "@ContentQuery.", method)
            return null
        }
        val toBeUsedUri = determineToBeUsedUri(resolvedContentEntity)
        if (toBeUsedUri.isEmpty()) {
            errorReporter.reportError("Failed to determine URI for query, the" +
                    " URI is neither specified in the associated ContentEntity, nor in the " +
                    "@ContentQuery parameters.", method)
        }
        val orderBy = if (contentQueryAnnotation.orderBy.isNotEmpty()) {
            // Just in case there are no spaces before or after commas
            val splitOrderBy = contentQueryAnnotation.orderBy.replace(",", ", ")
                .split(" ").toMutableList()
            for (i in 0..splitOrderBy.size - 1) {
                val commaLessName = splitOrderBy[i].replace(",", "")
                if (resolvedContentEntity.columns.containsKey(commaLessName)) {
                    splitOrderBy[i] = splitOrderBy[i].replace(commaLessName, resolvedContentEntity
                        .columns.get(commaLessName)!!.columnName)
                } else {
                    if (!ORDER_BY_KEYWORDS.contains(commaLessName.toLowerCase(Locale.ROOT)) &&
                        commaLessName != "") {
                        errorReporter.reportError("Field $commaLessName specified in orderBy is " +
                                "not a recognizable field in the associated ContentEntity.", method)
                    }
                }
            }
            splitOrderBy.joinToString(" ")
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
                paramsNamesAndTypes, errorReporter).process()
        }

        if (contentQueryAnnotation.projection.size == 1 &&
            returnType.extractIntendedReturnType().isSupportedColumnType()) {
            val queriedColumn = contentQueryAnnotation.projection[0]
            if (!resolvedContentEntity.columns.containsKey(queriedColumn)) {
                errorReporter.reportError("Column $queriedColumn being queried is not " +
                        "defined within the specified entity ${resolvedContentEntity.type}.",
                    method)
                return null
            }
            val queriedColumnType = resolvedContentEntity.columns.get(queriedColumn)!!.type
            if (queriedColumnType.boxIfPrimitive(processingEnv)
                != returnType.boxIfPrimitive(processingEnv) &&
                ((!returnType.isSupportedGenericType() ||
                    !processingEnv.typeUtils
                        .isSameType(returnType.extractIntendedReturnType(),
                            queriedColumnType.boxIfPrimitive(processingEnv))))) {
                errorReporter.reportError("Return type $returnType does not match type" +
                        " $queriedColumnType of column $queriedColumn being queried.", method)
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
                orderBy = orderBy)
        } else {
            // Either empty projection or more than one field, either way infer the return columns
            // from the return type POJO/entity.
            if (types.isSameType(returnType, resolvedContentEntity.type)) {
                return ContentQueryVO(method.simpleName.toString(), resolvedContentEntity.columns
                    .map { it.value }, selectionVO, toBeUsedUri, returnType, method, orderBy)
            }
            val pojo = if (returnType.isSupportedGenericType()) {
                PojoProcessor(returnType.extractIntendedReturnType(), processingEnv).process()
            } else {
                PojoProcessor(returnType, processingEnv).process()
            }
            // Apply the projection (if existing) to the POJO
            val pojoWithProjection = validateAndApplyProjectionToPojo(contentQueryAnnotation
                .projection, pojo, returnType, resolvedContentEntity)
            if (errorReporter.errorReported) {
                return null
            }
            return ContentQueryVO(method.simpleName.toString(),
                toQueryFor = pojoWithProjection!!.pojoFields.map {
                    resolvedContentEntity.columns[it.columnName]!!
                },
                selection = selectionVO,
                uri = toBeUsedUri,
                returnType = returnType,
                method = method,
                orderBy = orderBy
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
                errorReporter.reportError("Field ${field.name} of type ${field.type} " +
                        "corresponding to content" +
                        " provider column ${field.columnName} doesn't match a field with same " +
                        "type and content column in content entity ${resolvedContentEntity.type}",
                    method)
            } else {
                if (resolvedContentEntity.columns.get(field.columnName)!!.isNullable &&
                    !field.isNullable) {
                    // TODO(obenabde): clarify how to mark as nullable, i.e "?" for Kotlin and
                    // @androidx.annotations.Nullable for Java.
                    errorReporter.reportError("Field ${field.name} of type ${field.type} " +
                            "corresponding to content provider column ${field.columnName} is not " +
                            "nullable, however that column is declared as nullable in the " +
                            "associated entity ${resolvedContentEntity.type}. Please mark the " +
                            "field as nullable.", method)
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
                errorReporter.reportError("Column $column in projection array isn't included in " +
                        "the return type ${returnType.extractIntendedReturnType()}", method)
            }
        }
        for (pojoField in pojo.pojoFields) {
            if (!projection.contains(pojoField.columnName) && !pojoField.isNullable) {
                errorFound = true
                errorReporter.reportError("Field ${pojoField.name} in return object ${returnType
                    .extractIntendedReturnType()} is not included in the supplied projection and" +
                        " is not nullable. Fields that are not included in a query projection " +
                        "should all be nullable.", method)
            }
        }
        if (errorFound) {
            return null
        }
        val pojoFieldsWithProjection = pojo.pojoFields.filter { projection.contains(it.columnName) }
        validatePojoCorrectnessAgainstEntity(
            PojoVO(pojoFieldsWithProjection),
            resolvedContentEntity
        )
        return PojoVO(pojoFieldsWithProjection)
    }

    private fun determineToBeUsedUri(resolvedContentEntity: ContentEntityVO): String {
        if (contentQueryAnnotation.uri.isEmpty()) {
            return resolvedContentEntity.defaultUri
        }

        if (!contentQueryAnnotation.uri.startsWith(":")) {
            return contentQueryAnnotation.uri
        }

        val specifiedParamName = contentQueryAnnotation.uri.substring(1)
        if (specifiedParamName.isEmpty()) {
            errorReporter.reportError(": is an invalid uri, please follow it up with the " +
                    "parameter name", method)
            return contentQueryAnnotation.uri
        } else if (!method.parameters.map { it.simpleName.toString() }.contains
                (specifiedParamName)) {
            errorReporter.reportError("Parameter $specifiedParamName mentioned as the uri does " +
                    "not exist! Please add it to the method parameters", method)
            return contentQueryAnnotation.uri
        } else if (!method.parameters.filter {
                it.simpleName.toString().equals(specifiedParamName) }[0].asType().isString()) {
            errorReporter.reportError("Parameter $specifiedParamName mentioned as the uri should " +
                    "be of type String", method)
            return contentQueryAnnotation.uri
        }
        return contentQueryAnnotation.uri
    }
}

internal val ORDER_BY_KEYWORDS = listOf("asc", "desc")