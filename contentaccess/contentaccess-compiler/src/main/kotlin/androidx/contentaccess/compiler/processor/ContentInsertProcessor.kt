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
import androidx.contentaccess.ext.hasAnnotation
import androidx.contentaccess.ext.isSuspendFunction
import asTypeElement
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

class ContentInsertProcessor(
    private val method: ExecutableElement,
    private val contentInsertAnnotation: ContentInsert,
    private val processingEnv: ProcessingEnvironment,
    private val errorReporter: ErrorReporter
) {
    @KotlinPoetMetadataPreview
    fun process(): ContentInsertVO? {
        val isSuspendFunction = method.isSuspendFunction(processingEnv)
        val methodParameter = method.parameters.first()

        if (methodParameter == null) {
            // TODO(yrezgui): Improve error messaging
            errorReporter.reportError(
                """
                    Method ${method.simpleName} has no parameter.
                """.trimIndent(), method
            )

            return null
        }

        if (!methodParameter.asType().asTypeElement().hasAnnotation(ContentEntity::class)) {
            // TODO(yrezgui): Improve error messaging
            errorReporter.reportError(missingEntityOnMethod(method.simpleName.toString()), method)

            return null
        }

        val parameterName = methodParameter.simpleName.toString()
        val contentEntity = ContentEntityProcessor(
            methodParameter.asType(),
            processingEnv,
            errorReporter
        ).processEntity()

        if (contentEntity == null) {
            errorReporter.reportError(
                """
                    Method ${method.simpleName} has no parameter annotated with @ContentEntity.
                """.trimIndent(), method
            )

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

        return ContentInsertVO(
            name = method.simpleName.toString(),
            uri = toBeUsedUri,
            method = method,
            isSuspend = isSuspendFunction,
            parameterName = parameterName,
            columns = contentEntity.columns.values.toList()
        )
    }
}