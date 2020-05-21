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
import javax.lang.model.element.TypeElement

class ContentAccessObjectProcessor(
    val element: TypeElement,
    private val processingEnv:
            ProcessingEnvironment
) {
    fun process() {
        // TODO(obenabde): Verify this is indeed an interface
        // TODO(obenabde): Ensure only one ContentAccessObject annotation exists on the object
        val entity = ContentEntityProcessor(element.toAnnotationBox
            (ContentAccessObject::class)!!.getAsTypeMirror("contentEntity")!!,
            processingEnv).processEntity()
        // TODO(obenabde): ensure there are some methods!
        val queryMethods = element.getAllMethodsIncludingSupers()
            .filter { it.hasAnnotation(ContentQuery::class) }
            .map {
                ContentQueryProcessor(entity, it, it.getAnnotation
                    (ContentQuery::class
                    .java), processingEnv).process()
            }
        ContentAccessObjectWriter(ContentAccessObjectVO(entity, element
            .qualifiedName.toString(), processingEnv.elementUtils.getPackageOf
            (element).toString(), element.asType(), queryMethods), processingEnv).generateFile()
    }
}