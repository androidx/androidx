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
import androidx.contentaccess.compiler.ext.reportError
import androidx.contentaccess.compiler.utils.ErrorIndicator
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.compiler.vo.ContentQueryVO
import androidx.contentaccess.compiler.vo.PojoFieldVO
import androidx.contentaccess.ext.toAnnotationBox
import boxIfPrimitive
import extractIntendedReturnType
import isString
import isSupportedColumnType
import isSupportedGenericType
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class ContentQueryProcessor(
    var contentEntity: ContentEntityVO?,
    val method: ExecutableElement,
    val contentQueryAnnotation: ContentQuery,
    val processingEnv: ProcessingEnvironment,
    val errorIndicator: ErrorIndicator
) {

    private val messager = processingEnv.messager

    fun process(): ContentQueryVO? {
        val returnType = method.returnType
        val types = processingEnv.typeUtils

        val potentialContentEntity = method.toAnnotationBox(ContentQuery::class)!!
            .getAsTypeMirror("contentEntity")!!
        if (!potentialContentEntity.isVoidObject()) {
            contentEntity = ContentEntityProcessor(potentialContentEntity,
                processingEnv, errorIndicator).processEntity()
        }
        if (contentEntity == null) {
            messager.reportError("Method ${method.simpleName} has no associated entity, " +
                    "please ensure that either the content access object containing the method " +
                    "specifies an entity inside the @ContentAccessObject annotation or that the " +
                    "method specifies a content entity through the contentEntity parameter of " +
                    "@ContentQuery.", method)
            return null
        }
        val toBeUsedUri = determineToBeUsedUri()
        if (toBeUsedUri.isEmpty()) {
            messager.reportError("There is no way to determine the URI to use for the query, the" +
                    " URI is neither specified in the associated ContentEntity, nor in the " +
                    "@ContentQuery parameters.", method, errorIndicator)
        }
        val orderBy = if (contentQueryAnnotation.orderBy.isNotEmpty()) {
            // Just in case there are no spaces before or after commas
            val splitOrderBy = contentQueryAnnotation.orderBy.replace(",", ", ")
                .split(" ").toMutableList()
            for (i in 0..splitOrderBy.size - 1) {
                val commaLessName = splitOrderBy[i].replace(",", "")
                if (contentEntity!!.columns.containsKey(commaLessName)) {
                    splitOrderBy[i] = splitOrderBy[i].replace(commaLessName, contentEntity!!
                        .columns.get(commaLessName)!!.columnName)
                } else {
                    if (!ORDER_BY_KEYWORDS.contains(commaLessName.toLowerCase()) && commaLessName
                        != "") {
                        messager.reportError("Field $commaLessName specified in orderBy is not a" +
                                " recognizable field in the associated ContentEntity.", method,
                            errorIndicator)
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
            SelectionProcessor(method, messager, contentQueryAnnotation.selection,
                paramsNamesAndTypes, errorIndicator).process()
        }

        if (contentQueryAnnotation.projection.size == 1 &&
            returnType.extractIntendedReturnType().isSupportedColumnType()) {
            val queriedColumn = contentQueryAnnotation.projection[0]
            if (!contentEntity!!.columns.containsKey(queriedColumn)) {
                messager.reportError("Column $queriedColumn being queried is not " +
                        "defined within the specified entity ${contentEntity!!.type}.", method,
                    errorIndicator)
                return null
            }
            val queriedColumnType = contentEntity!!.columns.get(queriedColumn)!!.type
            if (queriedColumnType.boxIfPrimitive(processingEnv)
                != returnType.boxIfPrimitive(processingEnv) &&
                ((!returnType.isSupportedGenericType() ||
                    !processingEnv.typeUtils
                        .isSameType(returnType.extractIntendedReturnType(),
                            queriedColumnType.boxIfPrimitive(processingEnv))))) {
                messager.reportError("Return type $returnType does not match type" +
                        " $queriedColumnType of column $queriedColumn being queried.", method,
                    errorIndicator)
            }
            if (errorIndicator.errorFound) {
                return null
            }
            return ContentQueryVO(method.simpleName.toString(), listOf(contentEntity!!.columns
                .get(queriedColumn)!!), selectionVO, toBeUsedUri, returnType, method, orderBy)
        } else {
            // Either empty projection or more than one field, either way infer the return columns
            // from the return type POJO/entity.
            if (types.isSameType(returnType, contentEntity!!.type)) {
                return ContentQueryVO(method.simpleName.toString(), contentEntity!!.columns
                    .map { e -> e.value }, selectionVO, toBeUsedUri, returnType, method, orderBy)
            }
            var pojo = if (returnType.isSupportedGenericType()) {
                PojoProcessor(returnType.extractIntendedReturnType(), processingEnv).process()
            } else {
                PojoProcessor(returnType, processingEnv).process()
            }
            // Apply the projection (if existing) to the POJO
            val projectedPojo = validateAndApplyProjectionToPojo(contentQueryAnnotation
                .projection, pojo, returnType)
            if (projectedPojo.second) {
                errorIndicator.indicateError()
            } else {
                pojo = projectedPojo.first!!
                validatePojoCorrectnessAgainstEntity(pojo)
            }
            if (errorIndicator.errorFound) {
                return null
            }
            return ContentQueryVO(method.simpleName.toString(),
                pojo.map { contentEntity!!.columns[it.columnName]!! },
                selectionVO,
                toBeUsedUri,
                returnType,
                method,
                orderBy)
        }
    }

    // Returns whether the pojo's fields names and types match ones in the entity being queried.
    private fun validatePojoCorrectnessAgainstEntity(pojo: List<PojoFieldVO>) {
        pojo.forEach { e ->
            if (!contentEntity!!.columns.containsKey(e.columnName) || !processingEnv
                .typeUtils.isSameType(contentEntity!!.columns.get(e.columnName)!!
                    .type.boxIfPrimitive(processingEnv), e.type.boxIfPrimitive(processingEnv))) {
                messager.reportError("Field ${e.name} of type ${e.type} corresponding to content" +
                        " provider column ${e.columnName} doesn't match a field with same type " +
                        "and content column in content entity ${contentEntity!!.type}", method)
                errorIndicator.indicateError()
            } else {
                if (contentEntity!!.columns.get(e.columnName)!!.isNullable && !e.isNullable) {
                    // TODO(obenabde): clarify how to mark as nullable, i.e "?" for Kotlin and
                    // @androidx.annotations.Nullable for Java.
                    messager.reportError("Field ${e.name} of type ${e.type} " +
                            "corresponding to content provider column ${e.columnName} is not " +
                            "nullable, however that column is declared as nullable in the " +
                            "associated entity ${contentEntity!!.type}. Please mark the field as " +
                            "nullable.", method)
                }
            }
        }
    }

    private fun validateAndApplyProjectionToPojo(
        projection: Array<String>,
        pojo: List<PojoFieldVO>,
        returnType: TypeMirror
    ): Pair<List<PojoFieldVO>?, Boolean> {
        // alert now.
        if (projection.isEmpty()) {
            return Pair(pojo, false)
        }
        var errorFound = false
        val extractedColumnNames = pojo.map { it.columnName to it.isNullable }.toMap()
        for (column in projection) {
            if (!extractedColumnNames.containsKey(column)) {
                errorFound = true
                messager.reportError("Column $column in projection array isn't included in the " +
                        "return type ${returnType.extractIntendedReturnType()}", method)
            }
        }
        for (pojoField in pojo) {
            if (!projection.contains(pojoField.columnName) && !pojoField.isNullable) {
                errorFound = true
                messager.reportError("Field ${pojoField.name} in return object ${returnType
                    .extractIntendedReturnType()} is not included in the supplied projection and" +
                        " is not nullable. Fields that are not included in a query projection " +
                        "should all be nullable.", method)
            }
        }
        if (errorFound) {
            return Pair(null, true)
        }
        return Pair(pojo.filter { projection.contains(it.columnName) }, false)
    }

    private fun determineToBeUsedUri(): String {
        if (contentQueryAnnotation.uri.isEmpty()) {
            return contentEntity!!.defaultUri
        }

        if (!contentQueryAnnotation.uri.startsWith(":")) {
            return contentQueryAnnotation.uri
        }

        val specifiedParamName = contentQueryAnnotation.uri.substring(1)
        if (specifiedParamName.isEmpty()) {
            messager.reportError(": is an invalid uri, please follow it up with the parameter " +
                    "name", method, errorIndicator)
            return contentQueryAnnotation.uri
        } else if (!method.parameters.map { it.simpleName.toString() }.contains
                (specifiedParamName)) {
            messager.reportError("Parameter $specifiedParamName mentioned as the uri does not " +
                    "exist! Please add it to the method parameters", method, errorIndicator)
            return contentQueryAnnotation.uri
        } else if (!method.parameters.filter {
                it.simpleName.toString().equals(specifiedParamName) }[0].asType().isString()) {
            messager.reportError("Parameter $specifiedParamName mentioned as the uri should be " +
                    "of type String", method, errorIndicator)
            return contentQueryAnnotation.uri
        }
        return contentQueryAnnotation.uri
    }
}

internal val ORDER_BY_KEYWORDS = listOf("asc", "desc")