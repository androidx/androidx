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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.core.os.BuildCompat
import androidx.core.os.asOutcomeReceiver
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityStarter
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfigsHolder
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.client.controller.LocalControllerFactory
import androidx.privacysandbox.sdkruntime.client.controller.LocallyLoadedSdks
import androidx.privacysandbox.sdkruntime.client.controller.impl.LocalAppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.client.controller.impl.PlatformAppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.client.loader.SdkLoader
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_ALREADY_LOADED
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_NOT_FOUND
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.toLoadCompatSdkException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.annotations.TestOnly

/**
 * Compat version of [SdkSandboxManager].
 *
 * Provides APIs to load [androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat]
 * into SDK sandbox process or locally, and then interact with them.
 *
 * SdkSandbox process is a java process running in a separate uid range. Each app has its own
 * SDK sandbox process.
 *
 * First app needs to declare SDKs it depends on in it's AndroidManifest.xml
 * using <uses-sdk-library> tag. App can only load SDKs it depends on into the
 * SDK sandbox process.
 *
 * For loading SDKs locally App need to bundle and declare local SDKs in
 * assets/RuntimeEnabledSdkTable.xml with following format:
 *
 * <runtime-enabled-sdk-table>
 *     <runtime-enabled-sdk>
 *         <package-name>com.sdk1</package-name>
 *         <compat-config-path>assets/RuntimeEnabledSdk-com.sdk1/CompatSdkConfig.xml</compat-config-path>
 *     </runtime-enabled-sdk>
 *     <runtime-enabled-sdk>
 *         <package-name>com.sdk2</package-name>
 *         <compat-config-path>assets/RuntimeEnabledSdk-com.sdk2/CompatSdkConfig.xml</compat-config-path>
 *     </runtime-enabled-sdk>
 * </runtime-enabled-sdk-table>
 *
 * Each local SDK should have config with following format:
 *
 * <compat-config>
 *     <dex-path>RuntimeEnabledSdk-sdk.package.name/dex/classes.dex</dex-path>
 *     <dex-path>RuntimeEnabledSdk-sdk.package.name/dex/classes2.dex</dex-path>
 *     <java-resources-root-path>RuntimeEnabledSdk-sdk.package.name/res</java-resources-root-path>
 *     <compat-entrypoint>com.sdk.EntryPointClass</compat-entrypoint>
 *     <resource-id-remapping>
 *         <r-package-class>com.test.sdk.RPackage</r-package-class>
 *         <resources-package-id>123</resources-package-id>
 *     </resource-id-remapping>
 * </compat-config>
 *
 * @see [SdkSandboxManager]
 */
class SdkSandboxManagerCompat private constructor(
    private val platformApi: PlatformApi,
    private val configHolder: LocalSdkConfigsHolder,
    private val localLocallyLoadedSdks: LocallyLoadedSdks,
    private val appOwnedSdkRegistry: AppOwnedSdkRegistry,
    private val sdkLoader: SdkLoader
) {
    /**
     * Load SDK in a SDK sandbox java process or locally.
     *
     * App should already declare SDKs it depends on in its AndroidManifest using
     * <use-sdk-library> tag. App can only load SDKs it depends on into the SDK Sandbox process.
     *
     * When client application loads the first SDK, a new SdkSandbox process will be
     * created, otherwise other SDKs will be loaded into the same sandbox which already created for
     * the client application.
     *
     * Alternatively App could bundle and declare local SDKs dependencies in
     * assets/RuntimeEnabledSdkTable.xml to load SDKs locally.
     *
     * This API may only be called while the caller is running in the foreground. Calls from the
     * background will result in a [LoadSdkCompatException] being thrown.
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
        if (localLocallyLoadedSdks.isLoaded(sdkName)) {
            throw LoadSdkCompatException(LOAD_SDK_ALREADY_LOADED, "$sdkName already loaded")
        }

        val sdkConfig = configHolder.getSdkConfig(sdkName)
        if (sdkConfig != null) {
            val sdkProvider = sdkLoader.loadSdk(sdkConfig)
            val sandboxedSdkCompat = sdkProvider.onLoadSdk(params)
            localLocallyLoadedSdks.put(
                sdkName, LocallyLoadedSdks.Entry(
                    sdkProvider = sdkProvider,
                    sdk = sandboxedSdkCompat
                )
            )
            return sandboxedSdkCompat
        }

        return platformApi.loadSdk(sdkName, params)
    }

    /**
     * Unloads an SDK that has been previously loaded by the caller.
     *
     * It is not guaranteed that the memory allocated for this SDK will be freed immediately.
     *
     * @param sdkName name of the SDK to be unloaded.
     *
     * @see [SdkSandboxManager.unloadSdk]
     */
    fun unloadSdk(sdkName: String) {
        val localEntry = localLocallyLoadedSdks.remove(sdkName)
        if (localEntry == null) {
            platformApi.unloadSdk(sdkName)
        } else {
            localEntry.sdkProvider.beforeUnloadSdk()
            LocalSdkActivityHandlerRegistry.unregisterAllActivityHandlersForSdk(sdkName)
        }
    }

    /**
     * Adds a callback which gets registered for SDK sandbox lifecycle events, such as SDK sandbox
     * death. If the sandbox has not yet been created when this is called, the request will be
     * stored until a sandbox is created, at which point it is activated for that sandbox. Multiple
     * callbacks can be added to detect death.
     *
     * @param callbackExecutor the [Executor] on which to invoke the callback
     * @param callback the [SdkSandboxProcessDeathCallbackCompat] which will receive SDK sandbox
     *  lifecycle events.
     *
     * @see [SdkSandboxManager.addSdkSandboxProcessDeathCallback]
     */
    fun addSdkSandboxProcessDeathCallback(
        callbackExecutor: Executor,
        callback: SdkSandboxProcessDeathCallbackCompat
    ) {
        platformApi.addSdkSandboxProcessDeathCallback(callbackExecutor, callback)
    }

    /**
     * Removes an [SdkSandboxProcessDeathCallbackCompat] that was previously added using
     * [SdkSandboxManagerCompat.addSdkSandboxProcessDeathCallback]
     *
     * @param callback the [SdkSandboxProcessDeathCallbackCompat] which was previously added using
     *  [SdkSandboxManagerCompat.addSdkSandboxProcessDeathCallback]
     *
     * @see [SdkSandboxManager.removeSdkSandboxProcessDeathCallback]
     */
    fun removeSdkSandboxProcessDeathCallback(
        callback: SdkSandboxProcessDeathCallbackCompat
    ) {
        platformApi.removeSdkSandboxProcessDeathCallback(callback)
    }

    /**
     * Fetches information about Sdks that are loaded in the sandbox or locally.
     *
     * @return List of [SandboxedSdkCompat] containing all currently loaded sdks
     *
     * @see [SdkSandboxManager.getSandboxedSdks]
     */
    fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        val platformResult = platformApi.getSandboxedSdks()
        val localResult = localLocallyLoadedSdks.getLoadedSdks()
        return platformResult + localResult
    }

    /**
     * Registers [AppOwnedSdkSandboxInterfaceCompat] for an app process.
     *
     * Registering an [AppOwnedSdkSandboxInterfaceCompat] that has same name as a previously
     * registered interface will result in [IllegalStateException].
     *
     * [AppOwnedSdkSandboxInterfaceCompat.name] refers to the name of the interface.
     *
     * @param appOwnedSdk the [AppOwnedSdkSandboxInterfaceCompat] to be registered
     */
    fun registerAppOwnedSdkSandboxInterface(appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat) {
        appOwnedSdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedSdk)
    }

    /**
     * Unregisters [AppOwnedSdkSandboxInterfaceCompat] for an app process.
     *
     * @param sdkName the name under which [AppOwnedSdkSandboxInterfaceCompat] was registered.
     */
    fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
        appOwnedSdkRegistry.unregisterAppOwnedSdkSandboxInterface(sdkName)
    }

    /**
     * Fetches all [AppOwnedSdkSandboxInterfaceCompat] that are registered by the app.
     *
     * @return List of all currently registered [AppOwnedSdkSandboxInterfaceCompat]
     */
    fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
        appOwnedSdkRegistry.getAppOwnedSdkSandboxInterfaces()

    /**
     * Starts an [Activity] in the SDK sandbox.
     *
     * This function will start a new [Activity] in the same task of the passed `fromActivity` and
     * pass it to the SDK that shared the passed `sdkActivityToken` that identifies a request from
     * that SDK to stat this [Activity].
     *
     * @param fromActivity the [Activity] will be used to start the new sandbox [Activity] by
     * calling [Activity#startActivity] against it.
     * @param sdkActivityToken the identifier that is shared by the SDK which requests the
     * [Activity].
     * @see SdkSandboxManager.startSdkSandboxActivity
     */
    fun startSdkSandboxActivity(fromActivity: Activity, sdkActivityToken: IBinder) {
        if (LocalSdkActivityStarter.tryStart(fromActivity, sdkActivityToken)) {
            return
        }
        platformApi.startSdkSandboxActivity(fromActivity, sdkActivityToken)
    }

    @TestOnly
    internal fun getLocallyLoadedSdk(sdkName: String): LocallyLoadedSdks.Entry? =
        localLocallyLoadedSdks.get(sdkName)

    private interface PlatformApi {
        @DoNotInline
        suspend fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat

        @DoNotInline
        fun unloadSdk(sdkName: String)

        @DoNotInline
        fun addSdkSandboxProcessDeathCallback(
            callbackExecutor: Executor,
            callback: SdkSandboxProcessDeathCallbackCompat
        )

        @DoNotInline
        fun removeSdkSandboxProcessDeathCallback(
            callback: SdkSandboxProcessDeathCallbackCompat
        )

        @DoNotInline
        fun getSandboxedSdks(): List<SandboxedSdkCompat>

        fun startSdkSandboxActivity(fromActivity: Activity, sdkActivityToken: IBinder)
    }

    @RequiresApi(34)
    private open class Api34Impl(context: Context) : PlatformApi {
        protected val sdkSandboxManager = context.getSystemService(
            SdkSandboxManager::class.java
        )

        private val sandboxDeathCallbackDelegates:
            MutableList<SdkSandboxProcessDeathCallbackDelegate> = mutableListOf()

        @DoNotInline
        override suspend fun loadSdk(
            sdkName: String,
            params: Bundle
        ): SandboxedSdkCompat {
            try {
                val sandboxedSdk = loadSdkInternal(sdkName, params)
                return SandboxedSdkCompat(sandboxedSdk)
            } catch (ex: LoadSdkException) {
                throw toLoadCompatSdkException(ex)
            }
        }

        override fun unloadSdk(sdkName: String) {
            sdkSandboxManager.unloadSdk(sdkName)
        }

        override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
            return sdkSandboxManager
                .sandboxedSdks
                .map { platformSdk -> SandboxedSdkCompat(platformSdk) }
        }

        @DoNotInline
        override fun addSdkSandboxProcessDeathCallback(
            callbackExecutor: Executor,
            callback: SdkSandboxProcessDeathCallbackCompat
        ) {
            synchronized(sandboxDeathCallbackDelegates) {
                val delegate = SdkSandboxProcessDeathCallbackDelegate(callback)
                sdkSandboxManager.addSdkSandboxProcessDeathCallback(callbackExecutor, delegate)
                sandboxDeathCallbackDelegates.add(delegate)
            }
        }

        @DoNotInline
        override fun removeSdkSandboxProcessDeathCallback(
            callback: SdkSandboxProcessDeathCallbackCompat
        ) {
            synchronized(sandboxDeathCallbackDelegates) {
                for (i in sandboxDeathCallbackDelegates.lastIndex downTo 0) {
                    val delegate = sandboxDeathCallbackDelegates[i]
                    if (delegate.callback == callback) {
                        sdkSandboxManager.removeSdkSandboxProcessDeathCallback(delegate)
                        sandboxDeathCallbackDelegates.removeAt(i)
                    }
                }
            }
        }

        override fun startSdkSandboxActivity(fromActivity: Activity, sdkActivityToken: IBinder) {
            sdkSandboxManager.startSdkSandboxActivity(fromActivity, sdkActivityToken)
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

        private class SdkSandboxProcessDeathCallbackDelegate(
            val callback: SdkSandboxProcessDeathCallbackCompat
        ) : SdkSandboxManager.SdkSandboxProcessDeathCallback {
            @SuppressLint("Override") // b/273473397
            override fun onSdkSandboxDied() {
                callback.onSdkSandboxDied()
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

        override fun unloadSdk(sdkName: String) {
        }

        override fun getSandboxedSdks(): List<SandboxedSdkCompat> = emptyList()

        override fun addSdkSandboxProcessDeathCallback(
            callbackExecutor: Executor,
            callback: SdkSandboxProcessDeathCallbackCompat
        ) {
        }

        override fun removeSdkSandboxProcessDeathCallback(
            callback: SdkSandboxProcessDeathCallbackCompat
        ) {
        }

        override fun startSdkSandboxActivity(fromActivity: Activity, sdkActivityToken: IBinder) {
        }
    }

    companion object {

        private val sInstances = WeakHashMap<Context, WeakReference<SdkSandboxManagerCompat>>()

        /**
         *  Creates [SdkSandboxManagerCompat].
         *
         *  @param context Application context
         *
         *  @return SdkSandboxManagerCompat object.
         */
        @JvmStatic
        fun from(context: Context): SdkSandboxManagerCompat {
            synchronized(sInstances) {
                val reference = sInstances[context]
                var instance = reference?.get()
                if (instance == null) {
                    val configHolder = LocalSdkConfigsHolder.load(context)
                    val localSdks = LocallyLoadedSdks()
                    val appOwnedSdkRegistry = AppOwnedSdkRegistryFactory.create(context)
                    val controllerFactory = LocalControllerFactory(localSdks, appOwnedSdkRegistry)
                    val sdkLoader = SdkLoader.create(context, controllerFactory)
                    val platformApi = PlatformApiFactory.create(context)
                    instance = SdkSandboxManagerCompat(
                        platformApi,
                        configHolder,
                        localSdks,
                        appOwnedSdkRegistry,
                        sdkLoader
                    )
                    sInstances[context] = WeakReference(instance)
                }
                return instance
            }
        }

        @TestOnly
        internal fun reset() {
            synchronized(sInstances) {
                sInstances.clear()
            }
        }
    }

    private object PlatformApiFactory {
        fun create(context: Context): PlatformApi {
            return if (Build.VERSION.SDK_INT >= 34 || AdServicesInfo.isDeveloperPreview()) {
                Api34Impl(context)
            } else {
                FailImpl()
            }
        }
    }

    private object AppOwnedSdkRegistryFactory {
        @SuppressLint("NewApi", "ClassVerificationFailure") // For supporting DP Builds
        fun create(context: Context): AppOwnedSdkRegistry {
            return if (BuildCompat.AD_SERVICES_EXTENSION_INT >= 8 ||
                AdServicesInfo.isDeveloperPreview()
            ) {
                PlatformAppOwnedSdkRegistry(context)
            } else {
                LocalAppOwnedSdkRegistry()
            }
        }
    }
}
