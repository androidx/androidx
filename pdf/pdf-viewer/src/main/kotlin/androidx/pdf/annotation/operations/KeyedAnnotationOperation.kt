/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation.operations

import androidx.annotation.RestrictTo
import androidx.pdf.annotation.KeyedPdfAnnotation

/** Represents a single atomic change operation performed on a PDF annotation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class KeyedAnnotationOperation(
    public val operationType: OperationType,
    public val keyedAnnotation: KeyedPdfAnnotation,
) {
    /**
     * Defines the supported types of operations that can be performed on an annotation and
     * encapsulates the state transition logic.
     */
    public enum class OperationType {
        ADD,
        REMOVE,
        UPDATE;

        /**
         * Validates whether it is logically permissible to transition from the current operation
         * state to the [next] operation state.
         *
         * @param next The proposed next operation type.
         * @return `true` if the transition is allowed, `false` otherwise.
         */
        public fun canTransitionTo(next: OperationType): Boolean =
            validTransitions[this]?.contains(next) == true

        public companion object {
            /** A static mapping of valid state transitions. */
            private val validTransitions =
                mapOf(
                    ADD to setOf(UPDATE, REMOVE),
                    UPDATE to setOf(UPDATE, REMOVE),
                    REMOVE to setOf(ADD),
                )
        }
    }
}
