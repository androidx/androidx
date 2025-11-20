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

package androidx.pdf.annotation.models

import androidx.annotation.RestrictTo

/**
 * Represents a single, reversible edit operation on a piece of data of type [T].
 *
 * This internal class is a fundamental building block for tracking changes, typically used in an
 * undo/redo stack. It encapsulates both the type of action performed and the data that was
 * affected.
 *
 * @param T The type of the data that was edited.
 * @property op The type of operation, such as [Add], [Remove], or [Update].
 * @property edit The data that was added, removed, or updated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class EditOperation<T>(public open val op: Operation, public open val edit: T) {

    /** Marker interface for the different types of edit operations. */
    public interface Operation

    /** Represents an operation that adds data. */
    public data object Add : Operation

    /** Represents an operation that removes data. */
    public data object Remove : Operation

    // TODO: This will change to a class with the associated metadata
    /** Represents an operation that updates existing data. */
    public data object Update : Operation

    /** Represents an operation that does nothing. */
    public data object None : Operation

    public companion object {
        /** Returns the logical opposite of the current operation. */
        public fun Operation.invert(): Operation {
            return when (this) {
                is Add -> Remove
                is Remove -> Add
                is Update -> Update // TODO: Update this code when implementing this operation
                else -> None
            }
        }
    }
}
