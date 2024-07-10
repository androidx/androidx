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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfigsHolder
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.client.controller.LocalControllerFactory
import androidx.privacysandbox.sdkruntime.client.controller.SdkRegistry
import androidx.privacysandbox.sdkruntime.client.loader.LocalSdkProvider
import androidx.privacysandbox.sdkruntime.client.loader.SdkLoader
import androidx.privacysandbox.sdkruntime.client.loader.VersionHandshake
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import org.jetbrains.annotations.TestOnly

/**
 * Responsible for lifecycle of SDKs bundled with app. Shared between:
 * 1) [androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat]
 * 2) [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat]
 */
internal class LocalSdkRegistry(
    private val configHolder: LocalSdkConfigsHolder,
) : SdkRegistry {
    private lateinit var sdkLoader: SdkLoader

    private val sdks = HashMap<String, Entry>()

    override fun isResponsibleFor(sdkName: String): Boolean {
        return configHolder.getSdkConfig(sdkName) != null
    }

    override fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat =
        loadSdk(sdkName, params, overrideVersionHandshake = null)

    fun loadSdk(
        sdkName: String,
        params: Bundle,
        overrideVersionHandshake: VersionHandshake?
    ): SandboxedSdkCompat {
        val sdkConfig =
            configHolder.getSdkConfig(sdkName)
                ?: throw LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                    "$sdkName not bundled with app"
                )

        synchronized(sdks) {
            if (sdks.containsKey(sdkName)) {
                throw LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_ALREADY_LOADED,
                    "$sdkName already loaded"
                )
            }

            val sdkProvider = sdkLoader.loadSdk(sdkConfig, overrideVersionHandshake)
            val sandboxedSdkCompat = sdkProvider.onLoadSdk(params)
            sdks.put(sdkName, Entry(sdkProvider = sdkProvider, sdk = sandboxedSdkCompat))
            return sandboxedSdkCompat
        }
    }

    override fun unloadSdk(sdkName: String) {
        val loadedEntry = synchronized(sdks) { sdks.remove(sdkName) }
        if (loadedEntry == null) {
            Log.w(LOG_TAG, "Unloading SDK that is not loaded - $sdkName")
            return
        }

        loadedEntry.sdkProvider.beforeUnloadSdk()
        LocalSdkActivityHandlerRegistry.unregisterAllActivityHandlersForSdk(sdkName)
    }

    override fun getLoadedSdks(): List<SandboxedSdkCompat> =
        synchronized(sdks) {
            return sdks.values.map { it.sdk }
        }

    @TestOnly
    fun getLoadedSdkProvider(sdkName: String): LocalSdkProvider? =
        synchronized(sdks) {
            return sdks[sdkName]?.sdkProvider
        }

    private data class Entry(val sdkProvider: LocalSdkProvider, val sdk: SandboxedSdkCompat)

    companion object {
        const val LOG_TAG = "LocalSdkRegistry"

        /**
         * Create and initialize all required components for loading SDKs bundled with app.
         *
         * @param context App context
         * @param appOwnedSdkRegistry AppOwnedSdkRegistry for [LocalControllerFactory]
         * @return LocalSdkRegistry that could load SDKs bundled with app.
         */
        fun create(context: Context, appOwnedSdkRegistry: AppOwnedSdkRegistry): LocalSdkRegistry {
            val configHolder = LocalSdkConfigsHolder.load(context)

            val localSdkRegistry = LocalSdkRegistry(configHolder)
            localSdkRegistry.sdkLoader =
                SdkLoader.create(
                    context,
                    LocalControllerFactory(context, localSdkRegistry, appOwnedSdkRegistry)
                )

            return localSdkRegistry
        }
    }
}
