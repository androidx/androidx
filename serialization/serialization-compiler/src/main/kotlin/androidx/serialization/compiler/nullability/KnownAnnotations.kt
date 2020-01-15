/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.compiler.nullability

/**
 * A list of [Nullability] annotations ordered by preference.
 *
 * This is used for code generation, as `androidx.annotation` may not be present in the
 * compile-time class path and the code generator may fall back to other annotation packages.
 * These are derived from the set of annotations recognized by the Kotlin compiler.
 *
 * These will also be used for determining nullability of nested messages.
 */
internal val NULLABILITY_ANNOTATIONS = listOf(
    Nullability("androidx.annotation"),
    Nullability("org.jetbrains.annotations", nonNull = "NotNull"),
    Nullability("javax.annotation", nonNull = "Nonnull"),
    Nullability("android.support.annotation"),
    Nullability("android.annotation"),
    Nullability("com.android.annotations"),
    Nullability("org.eclipse.jdt.annotation"),
    Nullability("org.checkerframework.checker.nullness.qual"),
    Nullability("io.reactivex.annotations")
)
