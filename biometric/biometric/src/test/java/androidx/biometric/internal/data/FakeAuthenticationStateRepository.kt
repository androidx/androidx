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

import androidx.biometric.BiometricPrompt
import androidx.biometric.internal.CanceledFrom
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.CancellationSignalProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull

/** A fake implementation of [AuthenticationStateRepository] for testing purposes. */
internal class FakeAuthenticationStateRepository : AuthenticationStateRepository {
    override var canceledFrom: CanceledFrom = CanceledFrom.INTERNAL

    override var isAwaitingResult: Boolean = false

    override var isIgnoringCancel: Boolean = false

    override val cancellationSignalProvider: CancellationSignalProvider by lazy {
        CancellationSignalProvider()
    }

    private val _authenticationResult = MutableSharedFlow<BiometricPrompt.AuthenticationResult?>()
    override val authenticationResult: Flow<BiometricPrompt.AuthenticationResult> =
        _authenticationResult.asSharedFlow().filterNotNull()

    private val _authenticationError = MutableSharedFlow<BiometricErrorData?>()
    override val authenticationError: Flow<BiometricErrorData> =
        _authenticationError.asSharedFlow().filterNotNull()

    private val _authenticationHelpMessage = MutableSharedFlow<CharSequence?>()
    override val authenticationHelpMessage: SharedFlow<CharSequence?> =
        _authenticationHelpMessage.asSharedFlow()

    private val _isAuthenticationFailurePending = MutableSharedFlow<Unit>()
    override val isAuthenticationFailurePending: SharedFlow<Unit> =
        _isAuthenticationFailurePending.asSharedFlow()

    private val _isNegativeButtonPressPending = MutableSharedFlow<Unit>()
    override val isNegativeButtonPressPending: SharedFlow<Unit> =
        _isNegativeButtonPressPending.asSharedFlow()

    private val _isMoreOptionsButtonPressPending = MutableSharedFlow<Unit>()
    override val isMoreOptionsButtonPressPending: SharedFlow<Unit> =
        _isMoreOptionsButtonPressPending.asSharedFlow()

    override suspend fun setDelayedIgnoringCancel(ignoringCancel: Boolean, delayedTime: Long) {
        isIgnoringCancel = ignoringCancel
    }

    override suspend fun setAuthenticationResult(
        authenticationResult: BiometricPrompt.AuthenticationResult?
    ) {
        _authenticationResult.emit(authenticationResult)
    }

    override suspend fun setAuthenticationError(authenticationError: BiometricErrorData?) {
        _authenticationError.emit(authenticationError)
    }

    override suspend fun setAuthenticationHelpMessage(authenticationHelpMessage: CharSequence?) {
        _authenticationHelpMessage.emit(authenticationHelpMessage)
    }

    override suspend fun setAuthenticationFailurePending() {
        _isAuthenticationFailurePending.emit(Unit)
    }

    override suspend fun setNegativeButtonPressPending() {
        _isNegativeButtonPressPending.emit(Unit)
    }

    override suspend fun setMoreOptionsButtonPressPending() {
        _isMoreOptionsButtonPressPending.emit(Unit)
    }
}
