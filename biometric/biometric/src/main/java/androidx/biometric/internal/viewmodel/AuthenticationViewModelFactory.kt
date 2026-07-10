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

package androidx.biometric.internal.viewmodel

import androidx.biometric.internal.data.AuthenticationStateRepository
import androidx.biometric.internal.data.PromptConfigRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A [ViewModelProvider.Factory] that can create instances of [AuthenticationViewModel].
 *
 * This is required because [AuthenticationViewModel] has a non-empty constructor.
 */
internal class AuthenticationViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthenticationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthenticationViewModel(
                PromptConfigRepository.instance,
                AuthenticationStateRepository.instance,
            )
                as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
