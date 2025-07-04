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
@file:JvmName("AuthenticationUtils")

package androidx.biometric

import androidx.biometric.BiometricPrompt.LifecycleContainer
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A success or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthFailure], which
 * is set by [resultCallback]. The callback will be executed on the thread provided by the
 * [callbackExecutor].
 *
 * This *must* be called unconditionally, as part of initialization path, typically as a field
 * initializer of an Activity.
 *
 * Note that if multiple calls to this method are made within a single Fragment or Activity, only
 * the callback registered by the last invocation will be saved and receive authentication results.
 * This can result in unexpected behavior if you intend to manage multiple independent
 * authentication flows. It is strongly recommended to avoid multiple calls to this method in such
 * scenarios.
 *
 * @sample androidx.biometric.samples.activitySample
 */
@Suppress("ExecutorRegistration")
public fun FragmentActivity.registerForAuthenticationResult(
    callbackExecutor: Executor,
    resultCallback: AuthenticationResultCallback,
): AuthenticationResultLauncher {
    return AuthenticationResultRegistry()
        .register(
            viewModelStoreOwner = this,
            fragmentManager = this.supportFragmentManager,
            lifecycleContainer = LifecycleContainer(this.lifecycle),
            resultCallback = resultCallback,
            callbackExecutor = callbackExecutor,
        )
}

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A success or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthFailure], which
 * is set by [resultCallback]. The callback will be executed on the main thread.
 *
 * This *must* be called unconditionally, as part of initialization path, typically as a field
 * initializer of an Activity.
 *
 * Note that if multiple calls to this method are made within a single Fragment or Activity, only
 * the callback registered by the last invocation will be saved and receive authentication results.
 * This can result in unexpected behavior if you intend to manage multiple independent
 * authentication flows. It is strongly recommended to avoid multiple calls to this method in such
 * scenarios.
 *
 * @sample androidx.biometric.samples.activitySample
 * @see FragmentActivity.registerForAuthenticationResult(Executor, AuthenticationResultCallback)
 */
@Suppress("ExecutorRegistration")
public fun FragmentActivity.registerForAuthenticationResult(
    resultCallback: AuthenticationResultCallback
): AuthenticationResultLauncher {
    return AuthenticationResultRegistry()
        .register(
            viewModelStoreOwner = this,
            fragmentManager = this.supportFragmentManager,
            lifecycleContainer = LifecycleContainer(this.lifecycle),
            resultCallback = resultCallback,
            callbackExecutor = null,
        )
}

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A success or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthFailure], which
 * is set by [resultCallback]. The callback will be executed on the thread provided by the
 * [callbackExecutor].
 *
 * This *must* be called unconditionally, as part of initialization path, typically as a field
 * initializer of an Fragment.
 *
 * Note that if multiple calls to this method are made within a single Fragment or Activity, only
 * the callback registered by the last invocation will be saved and receive authentication results.
 * This can result in unexpected behavior if you intend to manage multiple independent
 * authentication flows. It is strongly recommended to avoid multiple calls to this method in such
 * scenarios.
 *
 * @sample androidx.biometric.samples.fragmentSample
 */
@Suppress("ExecutorRegistration")
public fun Fragment.registerForAuthenticationResult(
    callbackExecutor: Executor,
    resultCallback: AuthenticationResultCallback,
): AuthenticationResultLauncher {
    return AuthenticationResultRegistry()
        .register(
            viewModelStoreOwner = this,
            fragmentManager = this.childFragmentManager,
            lifecycleContainer = LifecycleContainer(this.lifecycle),
            resultCallback = resultCallback,
            callbackExecutor = callbackExecutor,
        )
}

/**
 * Returns an [AuthenticationResultLauncher] that can be used to initiate authentication.
 *
 * A success or error result will be delivered to [AuthenticationResultCallback.onAuthResult] and
 * (one or more) failures will be delivered to [AuthenticationResultCallback.onAuthFailure], which
 * is set by [resultCallback]. The callback will be executed on the main thread.
 *
 * This *must* be called unconditionally, as part of initialization path, typically as a field
 * initializer of an Fragment.
 *
 * Note that if multiple calls to this method are made within a single Fragment or Activity, only
 * the callback registered by the last invocation will be saved and receive authentication results.
 * This can result in unexpected behavior if you intend to manage multiple independent
 * authentication flows. It is strongly recommended to avoid multiple calls to this method in such
 * scenarios.
 *
 * @sample androidx.biometric.samples.fragmentSample
 * @see Fragment.registerForAuthenticationResult(Executor, AuthenticationResultCallback)
 */
@Suppress("ExecutorRegistration")
public fun Fragment.registerForAuthenticationResult(
    resultCallback: AuthenticationResultCallback
): AuthenticationResultLauncher {
    return AuthenticationResultRegistry()
        .register(
            viewModelStoreOwner = this,
            fragmentManager = this.childFragmentManager,
            lifecycleContainer = LifecycleContainer(this.lifecycle),
            resultCallback = resultCallback,
            callbackExecutor = null,
        )
}
