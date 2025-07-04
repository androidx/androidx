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

package androidx.privacysandbox.sdkruntime.provider.controller

import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackend
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackendHolder
import androidx.privacysandbox.sdkruntime.core.controller.impl.LocalImpl
import androidx.privacysandbox.sdkruntime.core.controller.impl.PlatformUDCImpl
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Compat version of [android.app.public sdksandbox.sdkprovider.SdkSandboxController].
 *
 * Controller that is used by SDK loaded in the sandbox or locally to access information provided by
 * the sandbox environment.
 *
 * It enables the SDK to communicate with other SDKS and know about the state of the sdks that are
 * currently loaded.
 *
 * An instance can be obtained using [SdkSandboxControllerCompat.from]. The [Context] can be
 * obtained using [SandboxedSdkProviderCompat.context].
 *
 * @see [SdkSandboxController]
 */
public class SdkSandboxControllerCompat
internal constructor(private val controllerImpl: SdkSandboxControllerBackend) {

    /**
     * Load SDK in a SDK sandbox java process or locally.
     *
     * The caller may only load SDKs the client app depends on into the SDK sandbox.
     *
     * @param sdkName name of the SDK to be loaded.
     * @param params additional parameters to be passed to the SDK in the form of a [Bundle] as
     *   agreed between the client and the SDK.
     * @return [SandboxedSdkCompat] from SDK on a successful run.
     * @throws [LoadSdkCompatException] on fail.
     */
    public suspend fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat =
        suspendCancellableCoroutine { continuation ->
            controllerImpl.loadSdk(
                sdkName,
                params,
                Runnable::run,
                ContinuationLoadSdkCallback(continuation),
            )
        }

    /**
     * Fetches information about Sdks that are loaded in the sandbox or locally.
     *
     * @return List of [SandboxedSdkCompat] containing all currently loaded sdks
     * @see [SdkSandboxController.getSandboxedSdks]
     */
    public fun getSandboxedSdks(): List<SandboxedSdkCompat> = controllerImpl.getSandboxedSdks()

    /**
     * Fetches all [AppOwnedSdkSandboxInterfaceCompat] that are registered by the app.
     *
     * @return List of all currently registered [AppOwnedSdkSandboxInterfaceCompat]
     */
    public fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        controllerImpl.getAppOwnedSdkSandboxInterfaces()

    /**
     * Returns an identifier for a [SdkSandboxActivityHandlerCompat] after registering it.
     *
     * This function registers an implementation of [SdkSandboxActivityHandlerCompat] created by an
     * SDK and returns an [IBinder] which uniquely identifies the passed
     * [SdkSandboxActivityHandlerCompat] object.
     *
     * @param handlerCompat is the [SdkSandboxActivityHandlerCompat] to register
     * @return [IBinder] uniquely identify the passed [SdkSandboxActivityHandlerCompat]
     * @see SdkSandboxController.registerSdkSandboxActivityHandler
     */
    public fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder = controllerImpl.registerSdkSandboxActivityHandler(handlerCompat)

    /**
     * Registers a listener to be notified of changes in the client's
     * [android.app.ActivityManager.RunningAppProcessInfo.importance].
     *
     * @param executor Executor for running listenerCompat
     * @param listenerCompat an implementation of [SdkSandboxClientImportanceListenerCompat] to
     *   register.
     */
    public fun registerSdkSandboxClientImportanceListener(
        executor: Executor,
        listenerCompat: SdkSandboxClientImportanceListenerCompat,
    ): Unit = controllerImpl.registerSdkSandboxClientImportanceListener(executor, listenerCompat)

    /**
     * Unregisters a listener previously registered using
     * [registerSdkSandboxClientImportanceListener]
     *
     * @param listenerCompat an implementation of [SdkSandboxClientImportanceListenerCompat] to
     *   unregister.
     */
    public fun unregisterSdkSandboxClientImportanceListener(
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    ): Unit = controllerImpl.unregisterSdkSandboxClientImportanceListener(listenerCompat)

    /**
     * Unregister an already registered [SdkSandboxActivityHandlerCompat].
     *
     * If the passed [SdkSandboxActivityHandlerCompat] is registered, it will be unregistered.
     * Otherwise, it will do nothing.
     *
     * If the [IBinder] token of the unregistered handler used to start a [android.app.Activity],
     * the [android.app.Activity] will fail to start.
     *
     * @param handlerCompat is the [SdkSandboxActivityHandlerCompat] to unregister.
     * @see SdkSandboxController.unregisterSdkSandboxActivityHandler
     */
    public fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): Unit = controllerImpl.unregisterSdkSandboxActivityHandler(handlerCompat)

    /**
     * Returns the package name of the client app.
     *
     * @return Package name of the client app.
     */
    public fun getClientPackageName(): String = controllerImpl.getClientPackageName()

    public companion object {
        /**
         * Creates [SdkSandboxControllerCompat].
         *
         * @param context SDK context
         * @return SdkSandboxControllerCompat object.
         */
        @JvmStatic
        public fun from(context: Context): SdkSandboxControllerCompat {
            val clientVersion = Versions.CLIENT_VERSION
            if (clientVersion != null) {
                val implFromClient =
                    SdkSandboxControllerBackendHolder.LOCAL_BACKEND
                        ?: throw UnsupportedOperationException(
                            "Shouldn't happen: No controller implementation available"
                        )
                return SdkSandboxControllerCompat(LocalImpl(implFromClient, clientVersion))
            }
            val platformImpl = PlatformImplFactory.create(context)
            return SdkSandboxControllerCompat(platformImpl)
        }
    }

    private object PlatformImplFactory {
        fun create(context: Context): SdkSandboxControllerBackend {
            if (Build.VERSION.SDK_INT >= 34) {
                return PlatformUDCImpl.from(context)
            }
            throw UnsupportedOperationException("SDK should be loaded locally on API below 34")
        }
    }

    private class ContinuationLoadSdkCallback(
        private val continuation: Continuation<SandboxedSdkCompat>
    ) : LoadSdkCallback, AtomicBoolean(false) {
        override fun onResult(result: SandboxedSdkCompat) {
            // Do not attempt to resume more than once, even if the caller is buggy.
            if (compareAndSet(false, true)) {
                continuation.resume(result)
            }
        }

        override fun onError(error: LoadSdkCompatException) {
            // Do not attempt to resume more than once, even if the caller is buggy.
            if (compareAndSet(false, true)) {
                continuation.resumeWithException(error)
            }
        }

        override fun toString() = "ContinuationLoadSdkCallback(outcomeReceived = ${get()})"
    }
}
