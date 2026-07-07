/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring

import androidx.annotation.RestrictTo

/**
 * Marks declarations that are are part of the **experimental** Ink Authoring API that offers
 * advanced control of handoff between rendering of "wet" in-progress strokes and rendering of "dry"
 * finished strokes. (The two take different rendering approaches because the former is willing to
 * trade off some efficiency and consistency to avoid latency between the location of user pointer
 * input and the head of the drawn stroke.) These declarations may (or may not) be changed,
 * deprecated, or removed in the near future, or the semantics of their behavior may change in some
 * way that may break some code.
 *
 * You can opt in to using APIs in your code by marking your declaration with `@OptIn` passing the
 * opt-in requirement annotation as its argument: `@OptIn(ExperimentalInkHandoffApi::class)`.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
public annotation class ExperimentalInkHandoffApi
