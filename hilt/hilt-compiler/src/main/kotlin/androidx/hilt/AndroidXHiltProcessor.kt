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

package androidx.hilt

import androidx.hilt.work.WorkerStep
import com.google.auto.service.AutoService
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * Annotation processor for the various AndroidX Hilt extensions.
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class AndroidXHiltProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = setOf(
        ClassNames.HILT_WORKER.canonicalName()
    )

    override fun getSupportedSourceVersion() = SourceVersion.latest()

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        getSteps().forEach { step ->
            annotations.firstOrNull { it.qualifiedName.contentEquals(step.annotation()) }?.let {
                step.process(roundEnv.getElementsAnnotatedWith(it))
            }
        }
        return false
    }

    private fun getSteps() = listOf(
        WorkerStep(processingEnv)
    )

    interface Step {
        fun annotation(): String
        fun process(annotatedElements: Set<Element>)
    }
}
