/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

/**
 * A [DslMarker] used to ensure that properties excluded by a [CustomStyleScope] do not resolve to
 * the outer scope. For example, nested styles are declared within the parent style. If a nested
 * style doesn't expose a property, it shouldn't incorrectly capture the parent property.
 */
@ExperimentalFoundationStyleApi
@DslMarker
@Target(AnnotationTarget.CLASS)
public annotation class StyleScopeMarker
