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
import androidx.contentaccess.ContentQuery
import androidx.contentaccess.compiler.ext.reportError
import androidx.contentaccess.compiler.utils.ErrorIndicator
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
    private val messager = processingEnv.messager

    @KotlinPoetMetadataPreview
    fun process() {
        val errorIndicator = ErrorIndicator()
        if (element.kind != ElementKind.INTERFACE) {
            messager.reportError("Only interfaces should be annotated with @ContentAccessObject," +
                    " ${element.qualifiedName} is not interface.", element, errorIndicator)
        } else {
            if (element.getAllMethodsIncludingSupers().isEmpty()) {
                messager.reportError("Interface ${element.qualifiedName} annotated with " +
                        "@ContentAccessObject doesn't delcare any methods. Interfaces annotated " +
                        "with @ContentAccessObject should declare at least one method.", element,
                    errorIndicator)
            }
        }
        val contentEntityType = element.toAnnotationBox(ContentAccessObject::class)!!
            .getAsTypeMirror("contentEntity")!!
        val entity = if (contentEntityType.isVoidObject()) {
            null
        } else {
            ContentEntityProcessor(contentEntityType, processingEnv, errorIndicator).processEntity()
        }
        if (errorIndicator.errorFound) {
            // Any of the above errors should handicap progress, stop early.
            return
        }
        val queryMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentQuery::class) }
            .map {
                ContentQueryProcessor(entity, it, it.getAnnotation
                    (ContentQuery::class
                    .java), processingEnv, errorIndicator).process()
            }
        // Return if there was an error.
        if (errorIndicator.errorFound) {
            return
        }
        ContentAccessObjectWriter(ContentAccessObjectVO(entity, element
            .qualifiedName.toString(), processingEnv.elementUtils.getPackageOf
            (element).toString(), element.asType(), queryMethods.map { it!! }), processingEnv)
            .generateFile()
    }
}