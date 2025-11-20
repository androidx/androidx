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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * A repository for authentication events including a final error or result; and various events
 * (help messages, failures, button clicks).
 */
internal interface AuthenticationStateRepository {
    /** Indicating where the dialog was last canceled from. */
    var canceledFrom: CanceledFrom

    /** Whether the client callback is awaiting an authentication result. */
    var isAwaitingResult: Boolean

    /** Whether the prompt should ignore cancel requests not initiated by the client. */
    var isIgnoringCancel: Boolean

    /** A provider for cross-platform compatible cancellation signal objects. */
    val cancellationSignalProvider: CancellationSignalProvider

    /** A flow that emits the successful authentication result. */
    val authenticationResult: Flow<BiometricPrompt.AuthenticationResult>

    /** A flow that emits the authentication error data. */
    val authenticationError: Flow<BiometricErrorData>

    /** A flow that holds the current authentication help message. */
    val authenticationHelpMessage: Flow<CharSequence?>

    /** A flow that emits when an authentication failure occurs. */
    val isAuthenticationFailurePending: Flow<Unit>

    /** A flow that emits when the negative button is pressed. */
    val isNegativeButtonPressPending: Flow<Unit>

    /** A flow that emits when the more options button is pressed. */
    val isMoreOptionsButtonPressPending: Flow<Unit>

    /**
     * Sets whether the prompt should ignore cancel requests from the framework after a given amount
     * of time.
     *
     * @param ignoringCancel Whether framework-initiated cancellations should be ignored.
     * @param delayedTime The amount of time to wait, in milliseconds.
     */
    suspend fun setDelayedIgnoringCancel(ignoringCancel: Boolean, delayedTime: Long)

    /** Emits a successful authentication result to be sent to the client. */
    suspend fun setAuthenticationResult(authenticationResult: BiometricPrompt.AuthenticationResult?)

    /** Emits an authentication error to be sent to the client. */
    suspend fun setAuthenticationError(authenticationError: BiometricErrorData?)

    /** Sets the current authentication help message. */
    suspend fun setAuthenticationHelpMessage(authenticationHelpMessage: CharSequence?)

    /** Emits an event for a recoverable authentication failure. */
    suspend fun setAuthenticationFailurePending()

    /** Emits an event for a negative button press. */
    suspend fun setNegativeButtonPressPending()

    /** Emits an event for a more options button press. */
    suspend fun setMoreOptionsButtonPressPending()

    companion object {
        val instance: AuthenticationStateRepository by lazy { AuthenticationStateRepositoryImpl() }
    }
}

/**
 * A repository for authentication state and events.
 *
 * This repository and all of its data is persisted over the lifetime of the client activity that
 * hosts the [BiometricPrompt].
 */
internal class AuthenticationStateRepositoryImpl : AuthenticationStateRepository {
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
        delay(delayedTime)
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
