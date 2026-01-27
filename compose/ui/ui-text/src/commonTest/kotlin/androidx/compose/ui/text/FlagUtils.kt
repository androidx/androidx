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

package androidx.compose.ui.text

import kotlin.reflect.KMutableProperty0

/**
 * Helper function to temporarily set a flag's value for the duration of a test block. This is
 * useful for testing different behaviors based on feature flags.
 *
 * Example:
 * ```
 * featureFlagTest(ComposeUiTextFlags::isCorrectShadowLerpWithNullsEnabled, true) {
 *     /* test code */
 * }
 * ```
 *
 * @param flag The property representing the flag to be modified.
 * @param forceValue The value to set the flag to for the test block.
 * @param block The test code to execute with the flag set to [forceValue].
 */
@Suppress("SameParameterValue")
internal inline fun featureFlagTest(
    flag: KMutableProperty0<Boolean>,
    forceValue: Boolean,
    block: () -> Unit,
) {
    val originalValue = flag.get()
    try {
        flag.set(forceValue)
        block()
    } finally {
        flag.set(originalValue)
    }
}
