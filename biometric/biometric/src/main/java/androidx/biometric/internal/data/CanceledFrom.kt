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

package androidx.biometric.internal.data

/** Represents the source or reason why an authentication operation was canceled. */
internal enum class CanceledFrom {
    /** Authentication was canceled by the library or framework. */
    INTERNAL,

    /** Authentication was canceled by the user (e.g., by pressing the system back button). */
    USER,

    /**
     * Authentication was canceled by the client application via an explicit cancel call.
     *
     * @see androidx.biometric.AuthenticationResultLauncher.cancel
     */
    CLIENT,

    /** Authentication was canceled by the user by pressing the negative button on the prompt. */
    NEGATIVE_BUTTON,

    /**
     * Authentication was canceled by the user by pressing the fallback option on the prompt
     * fallback option page.
     */
    FALLBACK_OPTION,

    /**
     * Authentication was canceled by the user by pressing the "more options" button on the prompt
     * content.
     */
    MORE_OPTIONS_BUTTON;

    /**
     * Whether the cancellation was triggered by the system or the client application, rather than
     * an explicit user interaction with the prompt UI.
     */
    fun isNotUserInitiated(): Boolean {
        return this == INTERNAL || this == CLIENT
    }
}
