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

package androidx.serialization.compiler.processing.steps

import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import javax.lang.model.element.Element
import kotlin.reflect.KClass

/** Wrapper for [ProcessingStep] for more idiomatic Kotlin. */
internal abstract class AbstractProcessingStep(
    vararg annotations: KClass<out Annotation>
) : ProcessingStep {
    private val javaAnnotations = ImmutableSet
        .builderWithExpectedSize<Class<out Annotation>>(annotations.size)
        .apply { annotations.forEach { add(it.java) } }
        .build()
    private val deferredElements = mutableSetOf<Element>()

    abstract fun process(elementsByAnnotation: Map<KClass<out Annotation>, Set<Element>>)

    /** Defer [element] for a later round of processing. */
    fun defer(element: Element) {
        deferredElements += element
    }

    final override fun annotations(): Set<Class<out Annotation>> {
        return javaAnnotations
    }

    final override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): Set<Element> {
        deferredElements.clear()

        try {
            process(Multimaps.asMap(elementsByAnnotation).mapKeys { (key, _) -> key.kotlin })
        } finally {
            return if (deferredElements.isNotEmpty()) {
                ImmutableSet.copyOf(deferredElements).also { deferredElements.clear() }
            } else {
                emptySet()
            }
        }
    }
}
