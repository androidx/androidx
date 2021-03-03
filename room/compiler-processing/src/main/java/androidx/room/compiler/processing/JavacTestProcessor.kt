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
package androidx.room.compiler.processing

import androidx.annotation.VisibleForTesting
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.javac.JavacRoundEnv
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/**
 * Javac processor implementation that provides access to the round environment.
 *
 * This is only used in tests, the main processor uses an API similar to the processing step
 * in Auto Common.
 */
@VisibleForTesting
@ExperimentalProcessingApi
abstract class JavacTestProcessor : AbstractProcessor() {
    val xProcessingEnv by lazy {
        // lazily create this as it is not available on construction time
        XProcessingEnv.create(super.processingEnv)
    }

    final override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        if (roundEnv.processingOver()) {
            return true
        }
        val env = xProcessingEnv as JavacProcessingEnv
        val javacRoundEnv = JavacRoundEnv(env, roundEnv)
        val xAnnotations = annotations.mapTo(mutableSetOf()) {
            env.wrapTypeElement(it)
        }
        return doProcess(xAnnotations, javacRoundEnv)
    }

    abstract fun doProcess(
        annotations: Set<XTypeElement>,
        roundEnv: XRoundEnv
    ): Boolean
}
