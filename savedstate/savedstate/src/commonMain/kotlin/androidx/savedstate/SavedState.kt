/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.savedstate

/**
 * An opaque (empty) common type that holds saveable values to be saved and restored by native
 * platforms that have a concept of System-initiated Process Death.
 *
 * That means, the OS will give the chance for the process to keep the state of the application
 * (normally using a serialization mechanism), and allow the app to restore its state later. That is
 * commonly referred to as "state restoration".
 *
 * required to act as a source input for a [SavedStateReader] or [SavedStateWriter].
 *
 * This class represents a container for persistable state data. It is designed to be
 * platform-agnostic, allowing seamless state saving and restoration across different environments.
 */
public expect class SavedState

/**
 * Builds a new [SavedState] with the specified [initialState], given as a [Map] of [String] keys
 * and [Any] value.
 *
 * Allows further modification of the state using the [builderAction].
 *
 * **IMPORTANT:** The [SavedStateWriter] passed as a receiver to the [builderAction] is valid only
 * inside that function. Using it outside of the function may produce an unspecified behavior.
 *
 * @param initialState An initial map of key-value pairs to populate the state. Defaults to an empty
 *   map.
 * @param builderAction A lambda function with a [SavedStateWriter] receiver to modify the state.
 * @return A [SavedState] instance containing the initialized key-value pairs.
 */
public expect inline fun savedState(
    initialState: Map<String, Any> = emptyMap(),
    builderAction: SavedStateWriter.() -> Unit = {},
): SavedState

/** Creates a new [SavedStateReader] for the [SavedState]. */
public fun SavedState.reader(): SavedStateReader = SavedStateReader(source = this)

/** Creates a new [SavedStateWriter] for the [SavedState]. */
public fun SavedState.writer(): SavedStateWriter = SavedStateWriter(source = this)

/**
 * Calls the specified function [block] with a [SavedStateReader] value as its receiver and returns
 * the [block] value.
 *
 * @param block A lambda function that performs read operations using the [SavedStateReader].
 * @return The result of the lambda function's execution.
 * @see [SavedStateReader]
 * @see [SavedStateWriter]
 */
public inline fun <T> SavedState.read(block: SavedStateReader.() -> T): T {
    return block(reader())
}

/**
 * Calls the specified function [block] with a [SavedStateReader] value as its receiver and returns
 * the [block] value.
 *
 * @param block A lambda function that performs read operations using the [SavedStateReader].
 * @return The result of the lambda function's execution.
 * @see [SavedStateReader]
 * @see [SavedStateWriter]
 */
public inline fun <T> SavedStateWriter.read(block: SavedStateReader.() -> T): T {
    return source.read(block)
}

/**
 * Calls the specified function [block] with a [SavedStateWriter] value as its receiver and returns
 * the [block] value.
 *
 * @param block A lambda function that performs write operations using the [SavedStateWriter].
 * @return The result of the lambda function's execution.
 * @see [SavedStateReader]
 * @see [SavedStateWriter]
 */
public inline fun <T> SavedState.write(block: SavedStateWriter.() -> T): T {
    return block(writer())
}

/**
 * Calls the specified function [block] with a [SavedStateWriter] value as its receiver and returns
 * the [block] value.
 *
 * @param block A lambda function that performs write operations using the [SavedStateWriter].
 * @return The result of the lambda function's execution.
 * @see [SavedStateReader]
 * @see [SavedStateWriter]
 */
public inline fun <T> SavedStateReader.write(block: SavedStateWriter.() -> T): T {
    return source.write(block)
}
