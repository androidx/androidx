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

import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.javac.JavacRoundEnv
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspRoundEnv
import javax.annotation.processing.RoundEnvironment
import kotlin.reflect.KClass

/**
 * Representation of an annotation processing round.
 *
 * @see javax.annotation.processing.RoundEnvironment
 */
interface XRoundEnv {
    /**
     * The root elements in the round.
     */
    val rootElements: Set<XElement>

    /**
     * Returns true if no further rounds of processing will be done.
     *
     * Sources generated in this round will not be not be subject to a subsequent round of
     * annotation processing, however they will be compiled.
     */
    val isProcessingOver: Boolean

    /**
     * Returns the set of [XElement]s that are annotated with the given annotation [klass].
     */
    fun getElementsAnnotatedWith(klass: KClass<out Annotation>): Set<XElement>

    fun getElementsAnnotatedWith(annotationQualifiedName: String): Set<XElement>

    companion object {
        /**
         * Creates an [XRoundEnv] from the given Java processing parameters.
         */
        @JvmStatic
        fun create(
            processingEnv: XProcessingEnv,
            roundEnvironment: RoundEnvironment? = null,
            isProcessingOver: Boolean = roundEnvironment?.processingOver() ?: false,
        ): XRoundEnv {
            return when (processingEnv) {
                is JavacProcessingEnv -> {
                    checkNotNull(roundEnvironment)
                    JavacRoundEnv(processingEnv, roundEnvironment)
                }
                is KspProcessingEnv -> {
                    KspRoundEnv(processingEnv, isProcessingOver)
                }
                else -> error("invalid processing environment type: $processingEnv")
            }
        }
    }
}
