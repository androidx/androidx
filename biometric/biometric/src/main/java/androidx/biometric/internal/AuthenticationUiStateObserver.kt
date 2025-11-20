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

package androidx.biometric.internal

import androidx.biometric.internal.viewmodel.AuthenticationViewModel

/**
 * An interface for classes that manage the connection and disconnection of
 * [AuthenticationViewModel] observers for the authentication UI state.
 */
internal interface AuthenticationUiStateObserver {
    /** Connects all necessary observers to their respective data sources. */
    fun connectObservers()

    /** Disconnects all observers, stopping the observation process. */
    fun disconnectObservers()
}
