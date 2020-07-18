/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.room.processing.javac.JavacElement
import androidx.room.processing.javac.JavacProcessingEnv
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.reflect.KClass

/**
 * Specialized processing step which only supports annotations on TypeElements.
 *
 * We can generalize it but for now, Room only needs annotations on TypeElements to start
 * processing.
 */
interface XProcessingStep {
    fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<KClass<out Annotation>, List<XTypeElement>>
    ): Set<XTypeElement>

    fun annotations(): Set<KClass<out Annotation>>

    fun asAutoCommonProcessor(
        env: XProcessingEnv
    ): BasicAnnotationProcessor.ProcessingStep {
        return JavacProcessingStepDelegate(
            env = env as JavacProcessingEnv,
            delegate = this
        )
    }
}

@Suppress("UnstableApiUsage")
internal class JavacProcessingStepDelegate(
    val env: JavacProcessingEnv,
    val delegate: XProcessingStep
) : BasicAnnotationProcessor.ProcessingStep {
    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): Set<Element> {
        val converted = mutableMapOf<KClass<out Annotation>, List<XTypeElement>>()
        annotations().forEach { annotation ->
            val elements = elementsByAnnotation[annotation].mapNotNull { element ->
                if (MoreElements.isType(element)) {
                    env.wrapTypeElement(MoreElements.asType(element))
                } else {
                    env.delegate.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unsupported element type: ${element.kind}",
                        element
                    )
                    null
                }
            }
            converted[annotation.kotlin] = elements
        }
        val result = delegate.process(env, converted)
        return result.map {
            (it as JavacElement).element
        }.toSet()
    }

    override fun annotations(): Set<Class<out Annotation>> {
        return delegate.annotations().mapTo(mutableSetOf()) {
            it.java
        }
    }
}
