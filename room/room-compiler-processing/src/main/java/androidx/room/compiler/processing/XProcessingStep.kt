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

/**
 * Processing step to simplify processing a set of annotations.
 */
interface XProcessingStep {
    /**
     * The implementation of processing logic for the step. It is guaranteed that the keys in
     * [elementsByAnnotation] will be a subset of the set returned by [annotations].
     *
     * @return the elements (a subset of the values of [elementsByAnnotation]) that this step
     *     is unable to process, possibly until a later processing round. These elements will be
     *     passed back to this step at the next round of processing.
     */
    fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>
    ): Set<XElement>

    /**
     * An optional hook for logic to be executed in the last round of processing.
     *
     * @see [XRoundEnv.isProcessingOver]
     */
    fun processOver(env: XProcessingEnv) { }

    /**
     * The set of annotation qualified names processed by this step.
     */
    fun annotations(): Set<String>
}
