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

package androidx.activity

/**
 * Marks declarations that are **experimental** in the Activity APIs. This means the design and
 * behavior of the annotated elements are not yet finalized and may change in future releases.
 *
 * Experimental APIs are provided to gather feedback and validate new functionality before
 * stabilizing. Use of these APIs requires explicit opt-in.
 *
 * Roughly speaking, these declarations may be deprecated, removed, or have their semantics changed
 * in a way that could break existing code.
 */
@RequiresOptIn(message = "This API is experimental and may change or be removed in the future.")
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalActivityApi
