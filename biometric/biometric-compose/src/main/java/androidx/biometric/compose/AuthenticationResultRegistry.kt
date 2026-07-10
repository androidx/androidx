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

package androidx.biometric.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.AuthenticationResultLauncher
import androidx.biometric.internal.AuthenticationResultRegistry
import androidx.biometric.internal.ui.handleConfirmCredentialResult
import androidx.biometric.internal.ui.launchConfirmCredentialActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.util.concurrent.Executor

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A successful or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthAttemptFailed],
 * which is set by [resultCallback]. The callback will be executed on the thread provided by the
 * [callbackExecutor].
 *
 * @sample androidx.biometric.compose.samples.RememberLauncherForAuthResult
 * @see rememberAuthenticationLauncher(AuthenticationResultCallback)
 */
@Composable
@SuppressWarnings("ExecutorRegistration")
public fun rememberAuthenticationLauncher(
    callbackExecutor: Executor,
    resultCallback: AuthenticationResultCallback,
): AuthenticationResultLauncher {
    return rememberAuthenticationLauncherInternal(
        callbackExecutor = callbackExecutor,
        resultCallback = resultCallback,
    )
}

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A successful or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthAttemptFailed],
 * which is set by [resultCallback]. The callback will be executed on the main thread.
 *
 * @sample androidx.biometric.compose.samples.RememberLauncherForAuthResult
 * @see rememberAuthenticationLauncher(Executor, AuthenticationResultCallback)
 */
@Composable
@SuppressWarnings("ExecutorRegistration")
public fun rememberAuthenticationLauncher(
    resultCallback: AuthenticationResultCallback
): AuthenticationResultLauncher {
    return rememberAuthenticationLauncherInternal(
        callbackExecutor = null,
        resultCallback = resultCallback,
    )
}

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A successful or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthAttemptFailed],
 * which is set by [resultCallback]. The callback will be executed on the thread provided by the
 * optional [callbackExecutor]. If no [callbackExecutor] is provided, the callback will be executed
 * on the main thread.
 *
 * @sample androidx.biometric.compose.samples.RememberLauncherForAuthResult
 */
@Composable
@SuppressWarnings("ExecutorRegistration")
private fun rememberAuthenticationLauncherInternal(
    callbackExecutor: Executor? = null,
    resultCallback: AuthenticationResultCallback,
): AuthenticationResultLauncher {
    // Keep track of the current contract and onResult listener
    val currentResultCallback by rememberUpdatedState(resultCallback)

    val authResultRegistry = remember { AuthenticationResultRegistry() }

    val viewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "Authentication requires a ViewModelStoreOwner to be provided via " +
                "LocalViewModelStoreOwner"
        }

    val context =
        checkNotNull(LocalContext.current) {
            "Authentication requires a Context to be provided via LocalContext"
        }

    val lifecycleOwner =
        checkNotNull(LocalLifecycleOwner.current) {
            "Authentication requires a LifecycleOwner to be provided via LocalLifecycleOwner"
        }

    val confirmCredentialActivityLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleConfirmCredentialResult(context, viewModelStoreOwner, result.resultCode)
        }

    return remember(lifecycleOwner, callbackExecutor) {
        authResultRegistry.register(
            context = context,
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            resultCallback = currentResultCallback,
            callbackExecutor = callbackExecutor,
            confirmCredentialActivityLauncher = {
                context.launchConfirmCredentialActivity(
                    viewModelStoreOwner,
                    confirmCredentialActivityLauncher,
                )
            },
        )
    }
}
