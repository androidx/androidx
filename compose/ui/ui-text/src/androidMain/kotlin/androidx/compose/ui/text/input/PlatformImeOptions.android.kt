/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable

/**
 * Used to configure Android platform IME options.
 *
 * @param privateImeOptions defines a [String] for supplying additional information options that
 * are private to a particular IME implementation.
 */
@Immutable
actual class PlatformImeOptions(
    val privateImeOptions: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformImeOptions) return false

        if (privateImeOptions != other.privateImeOptions) return false

        return true
    }

    override fun hashCode(): Int {
        return privateImeOptions?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "PlatformImeOptions(privateImeOptions=$privateImeOptions)"
    }
}
