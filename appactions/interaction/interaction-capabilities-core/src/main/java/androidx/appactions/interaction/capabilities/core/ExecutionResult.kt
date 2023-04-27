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

import androidx.annotation.RestrictTo
import java.util.Objects
/**
 * A class that represents the response after a [Capability] fulfills an action.
 * An [ExecutionResult] may contain an [output] based on the capability associated
 * with the execution.
 * For example, an execution associated with the CreateCalendarEvent capability would
 * produce an ExecutionResult containing a CreateCalendarEvent.Output, the created event.
 *
 * If [output] is null, the assistant client will know the execution has completed, but
 * may be unable to provide a natural language response or support confirmation from the user.
 *
 * @property output the object created by executing the capability.
 */
class ExecutionResult<OutputT> internal constructor(
    internal val shouldStartDictation: Boolean,
    val output: OutputT?,
) {
    override fun toString() =
        "ExecutionResult(shouldStartDictation=$shouldStartDictation,output=$output)"

    override fun equals(other: Any?): Boolean {
        return other is ExecutionResult<*> && output == other.output
    }

    override fun hashCode() = Objects.hash(shouldStartDictation, output)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun shouldStartDictation(): Boolean = shouldStartDictation

    /**
     * Builder for ExecutionResult.
     */
    class Builder<OutputT> {
        private var shouldStartDictation: Boolean = false
        private var output: OutputT? = null

        /**
         * If true, start dictation after returning the result of executing the [Capability].
         * Defaults to false.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun setShouldStartDictation(startDictation: Boolean) = apply {
            this.shouldStartDictation = startDictation
        }

        /** Sets the execution output. */
        fun setOutput(output: OutputT) = apply {
            this.output = output
        }

        /** Builds and returns the ExecutionResult instance. */
        fun build() = ExecutionResult(shouldStartDictation, output)
    }
}
