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
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.impl.ContinuationLoadSdkCallback
import java.util.concurrent.Executor
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.annotations.TestOnly

/**
 * Responsible for lifecycle of SDKs bundled with app. Shared between:
 * 1) [androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat]
 * 2) [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat]
 */
internal class LocalSdkRegistry(
    private val configHolder: LocalSdkConfigsHolder,
    private val mainThreadExecutor: Executor,
) : SdkRegistry {
    private lateinit var sdkLoader: SdkLoader

    private val sdks = HashMap<String, Entry>()

    override fun isResponsibleFor(sdkName: String): Boolean {
        return configHolder.getSdkConfig(sdkName) != null
    }

    override fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: LoadSdkCallback,
    ) = loadSdk(sdkName, params, executor, callback, overrideVersionHandshake = null)

    suspend fun loadSdk(
        sdkName: String,
        params: Bundle,
        overrideVersionHandshake: VersionHandshake? = null,
    ): SandboxedSdkCompat {
        return suspendCancellableCoroutine { continuation ->
            loadSdk(
                sdkName,
                params,
                Runnable::run,
                ContinuationLoadSdkCallback(continuation),
                overrideVersionHandshake,
            )
        }
    }

    private fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: LoadSdkCallback,
        overrideVersionHandshake: VersionHandshake?,
    ) {
        val sdkConfig = configHolder.getSdkConfig(sdkName)
        if (sdkConfig == null) {
            reportError(
                executor,
                callback,
                loadSdkErrorCode = LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                message = "$sdkName not bundled with app",
            )
            return
        }

        synchronized(sdks) {
            val existingEntry = sdks.get(sdkName)
            if (existingEntry != null) {
                // Do not remove currently loading / loaded entry, only report error
                reportError(
                    executor,
                    callback,
                    loadSdkErrorCode = LoadSdkCompatException.LOAD_SDK_ALREADY_LOADED,
                    message =
                        if (existingEntry.isLoading) {
                            "$sdkName is currently loading"
                        } else {
                            "$sdkName already loaded"
                        },
                )
                return
            }
            sdks.put(sdkName, Entry(isLoading = true))
        }

        withErrorHandling(sdkName, executor, callback) {
            // Could be done from any thread
            val sdkProvider = sdkLoader.loadSdk(sdkConfig, overrideVersionHandshake)
            mainThreadExecutor.execute {
                withErrorHandling(sdkName, executor, callback) {
                    // Must be done from main thread
                    val sandboxedSdkCompat = sdkProvider.onLoadSdk(params)
                    synchronized(sdks) {
                        sdks.put(
                            sdkName,
                            Entry(
                                isLoading = false,
                                sdkProvider = sdkProvider,
                                sdk = sandboxedSdkCompat,
                            ),
                        )
                    }
                    reportSuccess(executor, callback, sandboxedSdkCompat)
                }
            }
        }
    }

    override fun unloadSdk(sdkName: String) {
        val loadedEntry =
            synchronized(sdks) {
                val entry = sdks.get(sdkName)
                if (entry == null) {
                    Log.w(LOG_TAG, "Unloading SDK that is not loaded - $sdkName")
                    return
                }
                if (entry.isLoading) {
                    throw IllegalArgumentException(
                        "$sdkName is currently loading - please wait to unload"
                    )
                }
                sdks.remove(sdkName)
                entry
            }

        loadedEntry.sdkProvider?.beforeUnloadSdk()
        LocalSdkActivityHandlerRegistry.unregisterAllActivityHandlersForSdk(sdkName)
        LocalClientImportanceListenerRegistry.unregisterAllListenersForSdk(sdkName)
    }

    override fun getLoadedSdks(): List<SandboxedSdkCompat> =
        synchronized(sdks) {
            return sdks.values.mapNotNull { it.sdk }
        }

    @TestOnly
    fun getLoadedSdkProvider(sdkName: String): LocalSdkProvider? =
        synchronized(sdks) {
            return sdks[sdkName]?.sdkProvider
        }

    @TestOnly
    fun isLoading(sdkName: String): Boolean =
        synchronized(sdks) {
            return sdks[sdkName]?.isLoading == true
        }

    private data class Entry(
        val isLoading: Boolean,
        val sdkProvider: LocalSdkProvider? = null,
        val sdk: SandboxedSdkCompat? = null,
    )

    /**
     * Run [block] in try-catch and in case of errors report them via callback and removes sdk entry
     * from [sdks] to unblock further sdk loading.
     */
    private fun withErrorHandling(
        sdkName: String,
        executor: Executor,
        callback: LoadSdkCallback,
        block: () -> Unit,
    ) {
        try {
            return block()
        } catch (ex: LoadSdkCompatException) {
            // Remove entry to unblock further re-loading
            synchronized(sdks) { sdks.remove(sdkName) }
            reportError(executor, callback, ex)
        } catch (ex: Throwable) {
            // Remove entry to unblock further re-loading
            synchronized(sdks) { sdks.remove(sdkName) }
            reportError(
                executor,
                callback,
                loadSdkErrorCode = LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                message = "Unexpected error",
                cause = ex,
            )
        }
    }

    private fun reportError(
        executor: Executor,
        callback: LoadSdkCallback,
        loadSdkErrorCode: Int,
        message: String,
        cause: Throwable? = null,
    ) = reportError(executor, callback, LoadSdkCompatException(loadSdkErrorCode, message, cause))

    private fun reportError(
        executor: Executor,
        callback: LoadSdkCallback,
        exception: LoadSdkCompatException,
    ) = executor.execute { callback.onError(exception) }

    private fun reportSuccess(
        executor: Executor,
        callback: LoadSdkCallback,
        sandboxedSdkCompat: SandboxedSdkCompat,
    ) = executor.execute { callback.onResult(sandboxedSdkCompat) }

    companion object {
        const val LOG_TAG = "LocalSdkRegistry"

        /**
         * Create and initialize all required components for loading SDKs bundled with app.
         *
         * @param context App context
         * @param appOwnedSdkRegistry AppOwnedSdkRegistry for [LocalControllerFactory]
         * @param mainThreadExecutor Executor for main thread
         * @return LocalSdkRegistry that could load SDKs bundled with app.
         */
        fun create(
            context: Context,
            appOwnedSdkRegistry: AppOwnedSdkRegistry,
            mainThreadExecutor: Executor = MainThreadExecutor,
        ): LocalSdkRegistry {
            val configHolder = LocalSdkConfigsHolder.load(context)

            val localSdkRegistry = LocalSdkRegistry(configHolder, mainThreadExecutor)
            localSdkRegistry.sdkLoader =
                SdkLoader.create(
                    context,
                    LocalControllerFactory(context, localSdkRegistry, appOwnedSdkRegistry),
                )

            return localSdkRegistry
        }
    }
}
