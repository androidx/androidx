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
import androidx.contentaccess.compiler.vo.ContentAccessObjectVO
import androidx.contentaccess.compiler.writer.ContentAccessObjectWriter
import androidx.contentaccess.ext.getAllMethodsIncludingSupers
import androidx.contentaccess.ext.hasAnnotation
import javax.annotation.processing.ProcessingEnvironment
import androidx.contentaccess.ext.toAnnotationBox
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class ContentAccessObjectProcessor(
    val element: TypeElement,
    private val processingEnv:
            ProcessingEnvironment
) {
    private val messager = processingEnv.messager

    @KotlinPoetMetadataPreview
    fun process() {
        // TODO(obenabde): create helper functions for errors.
        // TODO(obenabde): See if you can do more stuff before returning after some of the errors
        // here.
        // TODO(obenabde): Examine error propagation through passing some context/error reporter
        // validation type of structure.
        if (element.kind != ElementKind.INTERFACE) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Only interfaces should be annotated" +
                    " with @ContentAccessObject, ${element.qualifiedName} is not interface.",
                element)
            return
        }
        val entity = ContentEntityProcessor(element.toAnnotationBox
            (ContentAccessObject::class)!!.getAsTypeMirror("contentEntity")!!,
            processingEnv).processEntity()
        if (element.getAllMethodsIncludingSupers().isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Interface ${element.qualifiedName} " +
                    "annotated with @ContentAccessObject doesn't delcare any methods. Interfaces " +
                    "annotated with @ContentAccessObject should declare at least one method.",
                element)
            return
        }
        val queryMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentQuery::class) }
            .map {
                ContentQueryProcessor(entity, it, it.getAnnotation
                    (ContentQuery::class
                    .java), processingEnv).process()
            }
        // Return if there was an error.
        queryMethods.forEach { if (it.second) return }
        ContentAccessObjectWriter(ContentAccessObjectVO(entity, element
            .qualifiedName.toString(), processingEnv.elementUtils.getPackageOf
            (element).toString(), element.asType(), queryMethods.map { it.first!! }), processingEnv)
            .generateFile()
    }
}