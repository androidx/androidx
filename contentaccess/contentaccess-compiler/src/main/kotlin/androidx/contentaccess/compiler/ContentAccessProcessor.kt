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

package androidx.contentaccess.compiler

import androidx.contentaccess.ContentAccessObject
import androidx.contentaccess.compiler.processor.ContentAccessObjectProcessor
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.annotation.processing.Processor
import com.google.auto.service.AutoService

@AutoService(Processor::class)
// TODO(obenabde): Make this Gradle incremental
class ContentAccessProcessor : BasicAnnotationProcessor() {

    override fun initSteps(): MutableIterable<ProcessingStep>? {
        return mutableListOf(ContentAccessProcessStep(processingEnv))
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    class ContentAccessProcessStep(val processingEnv: ProcessingEnvironment) : ProcessingStep {
        override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>?):
                Set<Element> {

            elementsByAnnotation?.get(ContentAccessObject::class.java)?.forEach {
                ContentAccessObjectProcessor(MoreElements.asType(it), processingEnv).process()
            }
            return emptySet()
        }

        override fun annotations(): MutableSet<out Class<out Annotation>> {
            return mutableSetOf(ContentAccessObject::class.java)
        }
    }
}