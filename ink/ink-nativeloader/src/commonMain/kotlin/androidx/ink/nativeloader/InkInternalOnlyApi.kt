/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.annotation.RestrictTo
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HiddenFromObjC

/**
 * Marks declarations that are are part of the **Ink Internal Only** API. Clients should not use
 * these APIs, and the interfaces annotated with this are excluded from the public documentation.
 * Clients should also not use this annotation itself.
 *
 * Parts of the code marked with this annotation generally involve interacting with the core Ink
 * native implementation or access to cross-package internals required for performance reasons.
 */
@OptIn(ExperimentalObjCRefinement::class)
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@HiddenFromObjC
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "These APIs should only be used for cross-package access to internals within Ink.",
)
public annotation class InkInternalOnlyApi
