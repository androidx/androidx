/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core

import java.util.Objects
/**
 * Class that represents the response after an ActionCapability fulfills an action.
 *
 * @param <OutputT>
 */
class ExecutionResult<OutputT> internal constructor(
    val startDictation: Boolean,
    val output: OutputT?,
) {
    override fun toString() =
        "ExecutionResult(startDictation=$startDictation,output=$output)"

    override fun equals(other: Any?): Boolean {
        return other is ExecutionResult<*> && output == other.output
    }

    override fun hashCode() = Objects.hash(startDictation, output)

    /**
     * Builder for ExecutionResult.
     *
     * @param <OutputT>
     */
    class Builder<OutputT> {
        private var startDictation: Boolean = false

        private var output: OutputT? = null

        /** Sets whether or not this fulfillment should start dictation. */
        fun setStartDictation(startDictation: Boolean) = apply {
            this.startDictation = startDictation
        }

        /** Sets the execution output. */
        fun setOutput(output: OutputT) = apply {
            this.output = output
        }

        /** Builds and returns the ExecutionResult instance. */
        fun build() = ExecutionResult(startDictation, output)
    }

    companion object {
        /** Returns a default ExecutionResult instance. */
        @JvmStatic
        fun <OutputT> getDefaultInstance() = ExecutionResult.Builder<OutputT>().build()
    }
}
