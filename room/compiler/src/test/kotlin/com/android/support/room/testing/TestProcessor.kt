/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.testing

import com.android.support.room.errors.ElementBoundException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.ERROR
import kotlin.reflect.KClass

@SupportedSourceVersion(SourceVersion.RELEASE_7)
class TestProcessor(val handlers: List<(TestInvocation) -> Boolean>,
                    val annotations: MutableSet<String>) : AbstractProcessor() {
    var count = 0
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment)
            : Boolean {
        try {
            return handlers.getOrNull(count++)?.invoke(
                    TestInvocation(processingEnv, annotations, roundEnv)) ?: true
        } catch (elmBound: ElementBoundException) {
            processingEnv.messager.printMessage(ERROR, elmBound.msg, elmBound.element)
        }
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return annotations
    }

    class Builder {
        private var handlers = arrayListOf<(TestInvocation) -> Boolean>()
        private var annotations = mutableSetOf<String>()
        fun nextRunHandler(f: (TestInvocation) -> Boolean): Builder {
            handlers.add(f)
            return this
        }

        fun forAnnotations(vararg klasses: KClass<*>): Builder {
            annotations.addAll(klasses.map { it.java.canonicalName })
            return this
        }

        fun build(): TestProcessor {
            if (annotations.isEmpty()) {
                throw IllegalStateException("must provide at least 1 annotation")
            }
            if (handlers.isEmpty()) {
                throw IllegalStateException("must provide at least 1 handler")
            }
            return TestProcessor(handlers, annotations)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}