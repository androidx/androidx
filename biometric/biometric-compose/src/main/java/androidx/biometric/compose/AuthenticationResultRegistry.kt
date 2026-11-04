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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.AuthenticationResultLauncher
import androidx.biometric.internal.AuthenticationResultRegistry
import androidx.biometric.internal.ui.handleConfirmCredentialResult
import androidx.biometric.internal.ui.launchConfirmCredentialActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import java.util.UUID
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
    val currentCallbackExecutor by rememberUpdatedState(callbackExecutor)

    // Unique key consistent across configuration changes (negative to avoid colliding with
    // positive keys from AuthenticationViewModel).
    val key = rememberSaveable { UUID.randomUUID().hashCode() or Int.MIN_VALUE }

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

    val realLauncher = remember { AuthenticationResultLauncherHolder() }
    val confirmCredentialLauncherHolder = remember { ConfirmCredentialActivityLauncherHolder() }
    val returnedLauncher = remember {
        object : AuthenticationResultLauncher {
            override fun launch(input: AuthenticationRequest) {
                realLauncher.launch(input)
            }

            override fun cancel() {
                realLauncher.cancel()
            }
        }
    }

    // Register before rememberLauncherForActivityResult so collectors reconnect before any
    // pending credential result is dispatched.
    DisposableEffect(context, lifecycleOwner, viewModelStoreOwner, key, callbackExecutor != null) {
        realLauncher.launcher =
            authResultRegistry.register(
                context = context,
                viewModelStoreOwner = viewModelStoreOwner,
                lifecycleOwner = lifecycleOwner,
                resultCallback =
                    object : AuthenticationResultCallback {
                        override fun onAuthResult(result: AuthenticationResult) {
                            currentResultCallback.onAuthResult(result)
                        }

                        override fun onAuthAttemptFailed() {
                            currentResultCallback.onAuthAttemptFailed()
                        }
                    },
                callbackExecutor =
                    if (callbackExecutor != null) {
                        Executor { runnable -> currentCallbackExecutor?.execute(runnable) }
                    } else {
                        null
                    },
                confirmCredentialActivityLauncher = {
                    val activityLauncher =
                        checkNotNull(confirmCredentialLauncherHolder.launcher) {
                            "ConfirmCredential launcher has not been initialized"
                        }
                    context.launchConfirmCredentialActivity(viewModelStoreOwner, activityLauncher)
                },
                key = key,
            )
        onDispose {
            authResultRegistry.unregister()
            realLauncher.launcher = null
        }
    }

    confirmCredentialLauncherHolder.launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleConfirmCredentialResult(context, viewModelStoreOwner, result.resultCode)
        }

    return returnedLauncher
}

private class ConfirmCredentialActivityLauncherHolder {
    var launcher: ActivityResultLauncher<Intent>? = null
}

private class AuthenticationResultLauncherHolder {
    var launcher: AuthenticationResultLauncher? = null

    fun launch(input: AuthenticationRequest) {
        launcher?.launch(input) ?: error("Launcher has not been initialized")
    }

    fun cancel() {
        launcher?.cancel()
    }
}
