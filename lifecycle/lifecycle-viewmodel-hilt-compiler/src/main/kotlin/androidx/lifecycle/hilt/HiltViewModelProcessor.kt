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

package androidx.lifecycle.hilt

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement

/**
 * Annotation processor that generates code enabling assisted injection of ViewModels using Hilt.
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class HiltViewModelProcessor : BasicAnnotationProcessor() {
    override fun initSteps() = listOf(ViewModelInjectStep(processingEnv))

    override fun getSupportedSourceVersion() = SourceVersion.latest()
}

class ViewModelInjectStep(
    private val processingEnv: ProcessingEnvironment
) : BasicAnnotationProcessor.ProcessingStep {

    override fun annotations() = setOf(ViewModelInject::class.java)

    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): MutableSet<out Element> {
        elementsByAnnotation[ViewModelInject::class.java].forEach { element ->
            val constructorElement = MoreElements.asExecutable(element)
            parse(constructorElement)?.let { viewModel ->
                HiltViewModelGenerator(processingEnv, viewModel).generate()
            }
        }
        return mutableSetOf()
    }

    private fun parse(constructorElement: ExecutableElement): HiltViewModelElements? {
        val typeElement = MoreElements.asType(constructorElement.enclosingElement)
        // TODO(danysantiago): Validate type extends ViewModel
        // TODO(danysantiago): Validate only one constructor is annotated
        return HiltViewModelElements(typeElement, constructorElement)
    }
}