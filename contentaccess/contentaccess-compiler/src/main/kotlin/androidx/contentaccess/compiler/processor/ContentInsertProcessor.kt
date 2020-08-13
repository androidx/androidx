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

import androidx.contentaccess.ContentEntity
import androidx.contentaccess.ContentInsert
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentInsertVO
import androidx.contentaccess.ext.getSuspendFunctionReturnType
import androidx.contentaccess.ext.hasAnnotation
import androidx.contentaccess.ext.isSuspendFunction
import asTypeElement
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

class ContentInsertProcessor(
    private val method: ExecutableElement,
    private val contentInsertAnnotation: ContentInsert,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {
    @KotlinPoetMetadataPreview
    fun process(): ContentInsertVO? {
        val isSuspendFunction = method.isSuspendFunction(processingEnv)
        val returnType = if (isSuspendFunction) {
            method.getSuspendFunctionReturnType()
        } else {
            method.returnType
        }
        val entitiesInParams = mutableListOf<VariableElement>()
        for (param in method.parameters) {
            if (param.asType().asTypeElement().hasAnnotation(ContentEntity::class)) {
                entitiesInParams.add(param)
            }
        }
        if (entitiesInParams.size > 1) {
            errorReporter.reportError(
                insertMethodHasMoreThanOneEntity(), method
            )
            return null
        } else if (entitiesInParams.isEmpty()) {
            errorReporter.reportError(
                insertMethodHasNoEntityInParameters(), method)
            return null
        }

        val entity = entitiesInParams.first()
        val entityParameterName = entity.simpleName.toString()

        val contentEntity = ContentEntityProcessor(
            entity.asType(),
            processingEnv,
            errorReporter
        ).processEntity()
        if (contentEntity == null) {
            return null
        }
        val toBeUsedUri = determineToBeUsedUri(
            resolvedContentEntity = contentEntity,
            uriInAnnotation = contentInsertAnnotation.uri,
            errorReporter = errorReporter,
            method = method
        )
        if (toBeUsedUri.isEmpty()) {
            errorReporter.reportError(missingUriOnMethod(), method)
        }

        if (!returnType.toString().equals("android.net.Uri")) {
            errorReporter.reportError(contentInsertAnnotatedMethodNotReturningAUri(), method)
        }

        return ContentInsertVO(
            name = method.simpleName.toString(),
            uri = toBeUsedUri,
            method = method,
            isSuspend = isSuspendFunction,
            parameterName = entityParameterName,
            columns = contentEntity.columns.values.toList()
        )
    }
}