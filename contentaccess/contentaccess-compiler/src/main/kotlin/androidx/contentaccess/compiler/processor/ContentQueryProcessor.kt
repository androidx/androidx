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
import extractSingleTypeArgument
import isSupportedWrapper
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class ContentQueryProcessor(
    var contentEntity: ContentEntityVO,
    val method: ExecutableElement,
    val contentQueryAnnotation: ContentQuery,
    val
                                processingEnv: ProcessingEnvironment
) {
    fun process(): ContentQueryVO {

        // TODO(obenabde): right now for errors we are throwing RunTimeExceptions for the sake
        // of the prototype, eventually handle them the right "annotation processor" way.

        val returnType = method.returnType
        val types = processingEnv.typeUtils

        val potentialContentEntity = method.toAnnotationBox(ContentQuery::class)!!
            .getAsTypeMirror("contentEntity")!!
        if (!potentialContentEntity.isVoidObject()) {
            contentEntity = ContentEntityProcessor(potentialContentEntity,
                processingEnv).processEntity()
        }
        // TODO(obenabde): Support specifying the URI in real time through method parameter
        // and no simply hardcoded like what is assumed right now if we're extracting it from the
        // method's @ContentQuery.
        val toBeUsedUri = if (contentQueryAnnotation.uri.isNotEmpty()) {
            contentQueryAnnotation.uri
        } else {
            contentEntity.defaultUri
        }
        if (toBeUsedUri.isEmpty()) {
            error { "Neither the associated entity nor the content query " +
                    "definition specifies a URI, please specify a URI." }
        }
        val paramsNamesAndTypes = HashMap<String, TypeMirror>()
        for (param in method.parameters) {
            paramsNamesAndTypes.put(param.simpleName.toString(), param.asType())
        }
        val selectionVO = if (contentQueryAnnotation.selection.isEmpty()) {
            null
        } else {
            SelectionProcessor(contentQueryAnnotation.selection,
                contentEntity.columns, paramsNamesAndTypes).process()
        }

        if (contentQueryAnnotation.query.isNotEmpty()) {
            val query = contentQueryAnnotation.query
            if (!contentEntity.columns.containsKey(query)) {
                error { "Column $query being queried is not defined within the " +
                        "specified entity ${contentEntity.type}" }
            }
            val queriedFieldType = contentEntity.columns.get(query)!!.type
            if (queriedFieldType != returnType && (!returnType.isSupportedWrapper() ||
                    !processingEnv.typeUtils
                        .isSameType(returnType.extractSingleTypeArgument(processingEnv),
                            queriedFieldType))) {
                    throw error { "Return type $returnType does not match type " +
                            "${contentEntity.columns.get(query)!!.type} of column $query being " +
                            "queried" }
            }
            return ContentQueryVO(method.simpleName.toString(), listOf(contentEntity.columns.get
                (query)!!), selectionVO, toBeUsedUri, returnType, method.parameters)
        } else {
            // No query field, we should infer the fields from the object.
            if (types.isSameType(returnType, contentEntity.type)) {
                return ContentQueryVO(method.simpleName.toString(), contentEntity.columns
                    .map { e -> e.value }, selectionVO, toBeUsedUri, returnType, method.parameters)
            }
            val pojo = if (returnType.isSupportedWrapper()) {
                // TODO(obenabde) there could be more than one layer of generics (e.g Flowable etc
                //  ...) on top of List, Set, Optional etc... Assumes single layer of generics for
                //  now
                PojoProcessor(returnType.extractSingleTypeArgument(processingEnv),
                    processingEnv).process()
            } else {
                PojoProcessor(returnType, processingEnv).process()
            }
            if (!validatePojoCorrectnessAgainstEntity(pojo)) {
                throw error { "Invalid pojo, not all fields names and types match ones " +
                        "in the content entity." }
            }
            return ContentQueryVO(method.simpleName.toString(), contentEntity.columns.values
                .filter { column -> pojo.containsKey(column.name) }, selectionVO, toBeUsedUri,
                returnType,
                method.parameters)
        }
    }

    // Returns whether the pojo's fields names and types match ones in the entity being queried.
    fun validatePojoCorrectnessAgainstEntity(pojo: Map<String, TypeMirror>): Boolean {
        pojo.forEach { e ->
            if (!contentEntity.columns.containsKey(e.key) || !processingEnv
                .typeUtils.isSameType(contentEntity
                .columns.get(e.key)!!.type, e.value)
                ) {
                return false
            }
        }
        return true
    }
}