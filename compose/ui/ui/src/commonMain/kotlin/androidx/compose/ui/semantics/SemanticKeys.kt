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

package androidx.compose.ui.semantics

internal fun <T> AccessibilityKey(
    name: String
) =
    SemanticsPropertyKey<T>(
        name = name,
        isImportantForAccessibility = true
    )

internal fun <T> AccessibilityKey(
    name: String,
    mergePolicy: (T?, T) -> T?
) =
    SemanticsPropertyKey<T>(
        name = name,
        isImportantForAccessibility = true,
        mergePolicy = mergePolicy
    )

@Suppress("NOTHING_TO_INLINE")
// inline to break static initialization cycle issue
internal inline fun <T : Function<Boolean>> ActionPropertyKey(
    name: String
) =
    AccessibilityKey<AccessibilityAction<T>>(
        name = name,
        mergePolicy = { parentValue, childValue ->
            AccessibilityAction(
                parentValue?.label ?: childValue.label,
                parentValue?.action ?: childValue.action
            )
        }
    )