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

package androidx.savedstate.internal

@PublishedApi
internal object SavedStateUtils {

    const val DEFAULT_BOOLEAN = false
    const val DEFAULT_FLOAT = 0f
    const val DEFAULT_DOUBLE = 0.0
    const val DEFAULT_INT = 0

    @Suppress("NOTHING_TO_INLINE")
    inline fun keyNotFoundError(key: String): Nothing =
        error("Saved state key '$key' was not found")

    inline fun <reified T> getValueFromSavedState(
        key: String,
        currentValue: () -> T?,
        contains: (key: String) -> Boolean,
        defaultValue: () -> T,
    ): T {
        return if (contains(key)) {
            currentValue() ?: defaultValue()
        } else {
            defaultValue()
        }
    }
}
