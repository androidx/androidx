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

import androidx.contentaccess.ContentDelete
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentDeleteVO
import androidx.contentaccess.compiler.vo.ContentEntityVO
import androidx.contentaccess.ext.isSuspendFunction
import androidx.contentaccess.ext.toAnnotationBox
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import isVoidObject
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

class ContentDeleteProcessor(
    private val contentEntity: ContentEntityVO?,
    private val method: ExecutableElement,
    private val contentDeleteAnnotation: ContentDelete,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {
    @KotlinPoetMetadataPreview
    fun process(): ContentDeleteVO? {
        val isSuspendFunction = method.isSuspendFunction(processingEnv)

        val potentialContentEntity = method.toAnnotationBox(ContentDelete::class)!!
            .getAsTypeMirror("contentEntity")!!

        val resolvedContentEntity = if (!potentialContentEntity.isVoidObject()) {
            ContentEntityProcessor(
                potentialContentEntity,
                processingEnv, errorReporter
            ).processEntity()
        } else {
            contentEntity
        }

        if (resolvedContentEntity == null) {
            errorReporter.reportError(missingEntityOnMethod(method.simpleName.toString()), method)

            return null
        }

        val toBeUsedUri = determineToBeUsedUri(
            resolvedContentEntity = resolvedContentEntity,
            uriInAnnotation = contentDeleteAnnotation.uri,
            errorReporter = errorReporter,
            method = method
        )

        if (toBeUsedUri.isEmpty()) {
            errorReporter.reportError(missingUriOnMethod(), method)
        }

        // TODO(yrezgui): Prefer mapOf instead of HashMap
        val paramsNamesAndTypes = HashMap<String, TypeMirror>()
        for (param in method.parameters) {
            paramsNamesAndTypes[param.simpleName.toString()] = param.asType()
        }

        val selectionVO = if (contentDeleteAnnotation.where.isEmpty()) {
            null
        } else {
            SelectionProcessor(
                method = method,
                selection = contentDeleteAnnotation.where,
                paramsNamesAndTypes = paramsNamesAndTypes,
                errorReporter = errorReporter,
                resolvedContentEntity = resolvedContentEntity
            ).process()
        }

        return ContentDeleteVO(
            name = method.simpleName.toString(),
            where = selectionVO,
            uri = toBeUsedUri,
            method = method,
            isSuspend = isSuspendFunction
        )
    }
}