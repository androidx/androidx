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
 * Declares the nullability of a type or element.
 */
enum class XNullability {
    /**
     * The type is guaranteed to be nullable. This means it is either a Kotlin Type declared with a
     * `?` at the end or it is a Java type that has one of the `nullable` annotations (e.g.
     * [androidx.annotation.Nullable].
     */
    NULLABLE,
    /**
     * The type is guaranteed to be nonnull. This means it is either a Kotlin Type declared
     * without a `?` at the end or it is a Java type that has one of the `non-null` annotations
     * (e.g. [androidx.annotation.NonNull].
     */
    NONNULL,
    /**
     * The nullability of the type is unknown. This happens if this is a non-primitive Java type
     * that does not have a nullability annotation or a Type in Kotlin where it is inferred from
     * the platform.
     */
    UNKNOWN
}
