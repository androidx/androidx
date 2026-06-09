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

package androidx.room3

/**
 * Control flags for built-in column type converters.
 *
 * Room provides automatic column type converters for common types (enum classes, UUID). These
 * converters are enabled by default but have lower priority than user-provided converters.
 *
 * Configure these flags in [ColumnTypeConverters] to enable or disable specific built-in
 * converters.
 */
@Target(allowedTargets = []) // Complex annotation target
@Retention(AnnotationRetention.BINARY)
public annotation class BuiltInColumnTypeConverters(
    /**
     * Controls whether Room generates a [ColumnTypeConverter] for enum types using their `name()`.
     *
     * Defaults to [State.INHERITED].
     */
    val enums: State = State.INHERITED,

    /**
     * Controls whether Room generates a [ColumnTypeConverter] for `java.util.UUID` using a
     * [ByteArray].
     *
     * Defaults to [State.INHERITED].
     */
    val uuid: State = State.INHERITED,

    /**
     * Controls whether Room generates a [ColumnTypeConverter] for `java.nio.ByteBuffer`.
     *
     * Defaults to [State.INHERITED].
     */
    val byteBuffer: State = State.INHERITED,
) {
    /** Control flags for built-in converters. */
    public enum class State {
        /** Room can use the built-in converter. */
        ENABLED,
        /** Room cannot use the built-in converter. */
        DISABLED,
        /**
         * Inherits value from a higher scope. See [ColumnTypeConverters] documentation for scoping
         * rules. Defaults to [ENABLED] if never set.
         */
        INHERITED,
    }
}
