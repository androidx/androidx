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
import javax.annotation.processing.RoundEnvironment

/**
 * Representation of an annotation processing round.
 *
 * @see javax.annotation.processing.RoundEnvironment
 */
@VisibleForTesting
interface XRoundEnv {
    /**
     * The root elements in the round.
     */
    val rootElements: Set<XElement>

    /**
     * Returns the set of [XElement]s that are annotated with the given [klass].
     */
    fun getElementsAnnotatedWith(klass: Class<out Annotation>): Set<XElement>

    companion object {
        /**
         * Creates an [XRoundEnv] from the given Java processing parameters.
         */
        fun create(
            processingEnv: XProcessingEnv,
            roundEnvironment: RoundEnvironment
        ): XRoundEnv {
            check(processingEnv is JavacProcessingEnv)
            return JavacRoundEnv(processingEnv, roundEnvironment)
        }
    }
}
