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
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.compiler.vo.ContentQueryVO
import androidx.contentaccess.ext.toAnnotationBox
import boxIfPrimitive
import extractIntendedReturnType
import isString
import isSupportedGenericType
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

class ContentQueryProcessor(
    var contentEntity: ContentEntityVO,
    val method: ExecutableElement,
    val contentQueryAnnotation: ContentQuery,
    val processingEnv: ProcessingEnvironment
) {

    private val messager = processingEnv.messager

    fun process(): Pair<ContentQueryVO?, Boolean> {
        var errorFound = false
        val returnType = method.returnType
        val types = processingEnv.typeUtils

        val potentialContentEntity = method.toAnnotationBox(ContentQuery::class)!!
            .getAsTypeMirror("contentEntity")!!
        if (!potentialContentEntity.isVoidObject()) {
            contentEntity = ContentEntityProcessor(potentialContentEntity,
                processingEnv).processEntity()
        }
        val checkedUri = determineToBeUsedUri()
        val toBeUsedUri = checkedUri.first
        if (checkedUri.second) {
            errorFound = true
        }
        if (toBeUsedUri.isEmpty()) {
            errorFound = true
            messager.printMessage(Diagnostic.Kind.ERROR, "There is no way to determine the URI to" +
                    " use for the query, the URI is neither specified in the associated " +
                    "ContentEntity, nor in the @ContentQuery parameters.", method)
        }
        val orderBy = if (contentQueryAnnotation.orderBy.isNotEmpty()) {
            // Just in case there are no spaces before or after commas
            val splitOrderBy = contentQueryAnnotation.orderBy.replace(",", ", ")
                .split(" ").toMutableList()
            for (i in 0..splitOrderBy.size - 1) {
                val commaLessName = splitOrderBy[i].replace(",", "")
                if (contentEntity.columns.containsKey(commaLessName)) {
                    splitOrderBy[i] = splitOrderBy[i].replace(commaLessName, contentEntity
                        .columns.get(commaLessName)!!.columnName)
                } else {
                    if (!ORDER_BY_KEYWORDS.contains(commaLessName.toLowerCase()) && commaLessName
                        != "") {
                        errorFound = true
                        messager.printMessage(Diagnostic.Kind.ERROR, "Field $commaLessName " +
                                "specified in orderBy is not a recognizable field in the " +
                                "associated ContentEntity.", method)
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
            val selectionProcessingResult = SelectionProcessor(method, messager,
                contentQueryAnnotation.selection, contentEntity.columns,
                paramsNamesAndTypes).process()
            if (selectionProcessingResult.second) {
                errorFound = true
                null
            } else {
                selectionProcessingResult.first!!
            }
        }

        if (contentQueryAnnotation.query.isNotEmpty()) {
            val query = contentQueryAnnotation.query
            if (!contentEntity.columns.containsKey(query)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Column $query being queried is not " +
                        "defined within the specified entity ${contentEntity.type}.", method)
                return Pair(null, true)
            }
            val queriedFieldType = contentEntity.columns.get(query)!!.type
            if (queriedFieldType.boxIfPrimitive(processingEnv)
                != returnType.boxIfPrimitive(processingEnv) &&
                ((!returnType.isSupportedGenericType() ||
                    !processingEnv.typeUtils
                        .isSameType(returnType.extractIntendedReturnType(),
                            queriedFieldType.boxIfPrimitive(processingEnv))))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Return type $returnType does not " +
                        "match type $queriedFieldType of column $query being queried.", method)
                errorFound = true
            }
            if (errorFound) {
                return Pair(null, true)
            }
            return Pair(ContentQueryVO(method.simpleName.toString(), listOf(contentEntity.columns
                .get(query)!!), selectionVO, toBeUsedUri, returnType, method, orderBy), false)
        } else {
            // No query field, we should infer the fields from the object.
            if (types.isSameType(returnType, contentEntity.type)) {
                return Pair(ContentQueryVO(method.simpleName.toString(), contentEntity.columns
                    .map { e -> e.value }, selectionVO, toBeUsedUri, returnType, method, orderBy),
                    false)
            }
            val pojo = if (returnType.isSupportedGenericType()) {
                // TODO(obenabde) there could be more than one layer of generics (e.g Flowable etc
                //  ...) on top of List, Set, Optional etc... Assumes single layer of generics for
                //  now
                PojoProcessor(returnType.extractIntendedReturnType(),
                    processingEnv).process()
            } else {
                PojoProcessor(returnType, processingEnv).process()
            }
            if (!validatePojoCorrectnessAgainstEntity(pojo)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Invalid pojo, not all fields names" +
                        " and types match ones in the content entity.", method)
                errorFound = true
            }
            if (errorFound) {
                return Pair(null, true)
            }
            return Pair(ContentQueryVO(method.simpleName.toString(), pojo.map {
                contentEntity.columns[it.first]!! },
                selectionVO,
                toBeUsedUri,
                returnType,
                method,
                orderBy), false)
        }
    }

    // Returns whether the pojo's fields names and types match ones in the entity being queried.
    private fun validatePojoCorrectnessAgainstEntity(pojo: List<Pair<String, TypeMirror>>):
            Boolean {
        pojo.forEach { e ->
            if (!contentEntity.columns.containsKey(e.first) || !processingEnv
                .typeUtils.isSameType(contentEntity
                .columns.get(e.first)!!.type, e.second)
                ) {
                return false
            }
        }
        return true
    }

    private fun determineToBeUsedUri(): Pair<String, Boolean> {
        if (contentQueryAnnotation.uri.isEmpty()) {
            return Pair(contentEntity.defaultUri, false)
        }

        if (!contentQueryAnnotation.uri.startsWith(":")) {
            return Pair(contentQueryAnnotation.uri, false)
        }

        val specifiedParamName = contentQueryAnnotation.uri.substring(1)
        if (specifiedParamName.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR, ": is an invalid uri, please " +
                "follow it up with the parameter name", method)
            return Pair(contentQueryAnnotation.uri, true)
        } else if (!method.parameters.map { it.simpleName.toString() }.contains
                (specifiedParamName)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Parameter $specifiedParamName " +
                    "mentioned as the uri does not exist! Please add it to the method " +
                    "parameters", method)
            return Pair(contentQueryAnnotation.uri, true)
        } else if (!method.parameters.filter {
                it.simpleName.toString().equals(specifiedParamName) }[0].asType().isString()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Parameter $specifiedParamName " +
                    "mentioned as the uri should be of type String", method)
            return Pair(contentQueryAnnotation.uri, true)
        }
        return Pair(contentQueryAnnotation.uri, false)
    }
}

internal val ORDER_BY_KEYWORDS = listOf("asc", "desc")