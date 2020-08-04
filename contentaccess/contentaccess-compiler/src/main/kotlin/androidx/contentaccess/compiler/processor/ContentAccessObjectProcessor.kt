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

import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.ContentDelete
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.ContentUpdate
import androidx.contentaccess.compiler.utils.ErrorReporter
import androidx.contentaccess.compiler.vo.ContentAccessObjectVO
import androidx.contentaccess.compiler.writer.ContentAccessObjectWriter
import androidx.contentaccess.ext.getAllMethodsIncludingSupers
import androidx.contentaccess.ext.hasAnnotation
import javax.annotation.processing.ProcessingEnvironment
import androidx.contentaccess.ext.toAnnotationBox
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import isVoidObject
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class ContentAccessObjectProcessor(
    val element: TypeElement,
    private val processingEnv:
            ProcessingEnvironment
) {
    @KotlinPoetMetadataPreview
    fun process() {
        val errorReporter = ErrorReporter(processingEnv.messager)
        if (element.kind != ElementKind.INTERFACE) {
            errorReporter.reportError("Only interfaces should be annotated with " +
                    "@ContentAccessObject, '${element.qualifiedName}' is not interface.", element)
        } else {
            if (element.getAllMethodsIncludingSupers().isEmpty()) {
                errorReporter.reportError("Interface '${element.qualifiedName}' annotated with " +
                        "@ContentAccessObject doesn't delcare any functions. Interfaces annotated" +
                        " with @ContentAccessObject should declare at least one function.", element)
            }
        }
        val contentEntityType = element.toAnnotationBox(ContentAccessObject::class)!!
            .getAsTypeMirror("contentEntity")!!
        val entity = if (contentEntityType.isVoidObject()) {
            null
        } else {
            ContentEntityProcessor(contentEntityType, processingEnv, errorReporter).processEntity()
        }
        if (errorReporter.errorReported) {
            // Any of the above errors should handicap progress, stop early.
            return
        }
        val queryMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentQuery::class) }
            .map {
                ContentQueryProcessor(
                    contentEntity = entity,
                    method = it,
                    contentQueryAnnotation = it.getAnnotation(ContentQuery::class.java),
                    processingEnv = processingEnv,
                    errorReporter = errorReporter
                ).process()
            }

        val updateMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentUpdate::class) }
            .map {
                ContentUpdateProcessor(
                    contentEntity = entity,
                    method = it,
                    contentUpdateAnnotation = it.getAnnotation(ContentUpdate::class.java),
                    processingEnv = processingEnv,
                    errorReporter = errorReporter
                ).process()
            }

        val deleteMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentDelete::class) }
            .map {
                ContentDeleteProcessor(
                    contentEntity = entity,
                    method = it,
                    contentDeleteAnnotation = it.getAnnotation(ContentDelete::class.java),
                    processingEnv = processingEnv,
                    errorReporter = errorReporter
                ).process()
            }

        // Return if there was an error.
        if (errorReporter.errorReported) {
            return
        }
        ContentAccessObjectWriter(
            ContentAccessObjectVO(
                contentEntity = entity,
                interfaceName = element.qualifiedName.toString(),
                packageName = processingEnv.elementUtils.getPackageOf(element).toString(),
                interfaceType = element.asType(),
                queries = queryMethods.mapNotNull { it },
                updates = updateMethods.mapNotNull { it },
                deletes = deleteMethods.filterNotNull()
            ),
            processingEnv
        ).generateFile()
    }
}