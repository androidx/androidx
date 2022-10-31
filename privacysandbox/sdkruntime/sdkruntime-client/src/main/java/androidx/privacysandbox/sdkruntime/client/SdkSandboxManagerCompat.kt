/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.privacysandbox.sdkruntime.client

import android.adservices.AdServicesVersion
import android.annotation.SuppressLint
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfigsHolder
import androidx.privacysandbox.sdkruntime.client.loader.LocalSdk
import androidx.privacysandbox.sdkruntime.client.loader.SdkLoader
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_ALREADY_LOADED
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_NOT_FOUND
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.toLoadCompatSdkException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat.Companion.toSandboxedSdkCompat
import java.util.WeakHashMap
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Compat version of [SdkSandboxManager].
 *
 * Provides APIs to load [androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat]
 * into SDK sandbox process or locally, and then interact with them.
 *
 * @see [SdkSandboxManager]
 */
class SdkSandboxManagerCompat private constructor(
    private val platformApi: PlatformApi,
    private val configHolder: LocalSdkConfigsHolder,
    private val sdkLoader: SdkLoader
) {

    private val localLoadedSdks = HashMap<String, LocalSdk>()

    /**
     * Load SDK in a SDK sandbox java process or locally.
     *
     * @param sdkName name of the SDK to be loaded.
     * @param params additional parameters to be passed to the SDK in the form of a [Bundle]
     *  as agreed between the client and the SDK.
     * @return [SandboxedSdkCompat] from SDK on a successful run.
     * @throws [LoadSdkCompatException] on fail.
     *
     * @see [SdkSandboxManager.loadSdk]
     */
    @Throws(LoadSdkCompatException::class)
    suspend fun loadSdk(
        sdkName: String,
        params: Bundle
    ): SandboxedSdkCompat {
        if (localLoadedSdks.containsKey(sdkName)) {
            throw LoadSdkCompatException(LOAD_SDK_ALREADY_LOADED, "$sdkName already loaded")
        }

        val sdkConfig = configHolder.getSdkConfig(sdkName)
        if (sdkConfig != null) {
            val sdkHolder = sdkLoader.loadSdk(sdkConfig)
            val sandboxedSdkCompat = sdkHolder.onLoadSdk(params)
            localLoadedSdks.put(sdkName, sdkHolder)
            return sandboxedSdkCompat
        }

        return platformApi.loadSdk(sdkName, params)
    }

    private interface PlatformApi {
        @DoNotInline
        suspend fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat
    }

    // TODO(b/249981547) Remove suppress when prebuilt with SdkSandbox APIs dropped to T
    @SuppressLint("NewApi", "ClassVerificationFailure")
    @RequiresApi(TIRAMISU)
    private class Api33Impl(context: Context) : PlatformApi {
        private val sdkSandboxManager = context.getSystemService(
            SdkSandboxManager::class.java
        )

        @DoNotInline
        override suspend fun loadSdk(
            sdkName: String,
            params: Bundle
        ): SandboxedSdkCompat {
            try {
                val sandboxedSdk = loadSdkInternal(sdkName, params)
                return toSandboxedSdkCompat(sandboxedSdk)
            } catch (ex: LoadSdkException) {
                throw toLoadCompatSdkException(ex)
            }
        }

        private suspend fun loadSdkInternal(
            sdkName: String,
            params: Bundle
        ): SandboxedSdk {
            return suspendCancellableCoroutine { continuation ->
                sdkSandboxManager.loadSdk(
                    sdkName,
                    params,
                    Runnable::run,
                    continuation.asOutcomeReceiver()
                )
            }
        }

        companion object {
            @DoNotInline
            fun isSandboxAvailable(): Boolean {
                return AdServicesVersion.API_VERSION >= 2
            }
        }
    }

    private class FailImpl : PlatformApi {
        @DoNotInline
        override suspend fun loadSdk(
            sdkName: String,
            params: Bundle
        ): SandboxedSdkCompat {
            throw LoadSdkCompatException(LOAD_SDK_NOT_FOUND, "$sdkName not bundled with app")
        }
    }

    companion object {

        private val sInstances = WeakHashMap<Context, SdkSandboxManagerCompat>()

        /**
         *  Creates [SdkSandboxManagerCompat].
         *
         *  @param context Application context
         *
         *  @return SdkSandboxManagerCompat object.
         */
        @JvmStatic
        fun obtain(context: Context): SdkSandboxManagerCompat {
            synchronized(sInstances) {
                var instance = sInstances[context]
                if (instance == null) {
                    val configHolder = LocalSdkConfigsHolder.load(context)
                    val sdkLoader = SdkLoader.create(context)
                    val platformApi =
                        if (Build.VERSION.SDK_INT >= TIRAMISU && Api33Impl.isSandboxAvailable()) {
                            Api33Impl(context)
                        } else {
                            FailImpl()
                        }
                    instance = SdkSandboxManagerCompat(platformApi, configHolder, sdkLoader)
                    sInstances[context] = instance
                }
                return instance
            }
        }
    }
}