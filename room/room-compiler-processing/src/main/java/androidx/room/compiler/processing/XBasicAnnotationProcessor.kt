/*
 * Copyright 2021 The Android Open Source Project
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

/**
 * Common interface for basic annotation processors.
 *
 * A processor should not implement this interface directly and instead should extend
 * [androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor] or
 * [androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor].
 */
interface XBasicAnnotationProcessor {

    /**
     * Returns the [XProcessingEnv].
     */
    val xProcessingEnv: XProcessingEnv

    /**
     * The list of processing steps to execute.
     */
    fun processingSteps(): Iterable<XProcessingStep>

    /**
     * Called at the end of a processing round after all [processingSteps] have been executed.
     */
    fun postRound(env: XProcessingEnv, round: XRoundEnv) { }
}