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
package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.impl.SandboxControllerInjector
import androidx.privacysandbox.sdkruntime.client.loader.impl.SdkProviderV1
import androidx.privacysandbox.sdkruntime.client.loader.storage.CachedLocalSdkStorage
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature

/**
 * Load SDK bundled with App.
 */
internal class SdkLoader internal constructor(
    private val classLoaderFactory: ClassLoaderFactory,
    private val appContext: Context,
    private val controllerFactory: ControllerFactory
) {

    internal interface ClassLoaderFactory {
        fun createClassLoaderFor(sdkConfig: LocalSdkConfig, parent: ClassLoader): ClassLoader
    }

    internal interface ControllerFactory {
        fun createControllerFor(
            sdkConfig: LocalSdkConfig
        ): SdkSandboxControllerCompat.SandboxControllerImpl
    }

    /**
     * Loading SDK in separate classloader:
     *  1. Create classloader for sdk;
     *  2. Performing handshake to determine api version;
     *  3. (optional) Update RPackage.packageId to support Android Resource remapping for SDK
     *  4. Select [LocalSdkProvider] implementation that could work with that api version.
     *
     * @param sdkConfig sdk to load
     * @param overrideVersionHandshake (optional) override internal api level handshake
     * @return LocalSdk implementation for loaded SDK
     */
    fun loadSdk(
        sdkConfig: LocalSdkConfig,
        overrideVersionHandshake: VersionHandshake? = null
    ): LocalSdkProvider {
        val classLoader = classLoaderFactory.createClassLoaderFor(
            sdkConfig,
            getParentClassLoader()
        )
        val versionHandshake = overrideVersionHandshake ?: VersionHandshake.DEFAULT
        return createLocalSdk(classLoader, sdkConfig, versionHandshake)
    }

    private fun getParentClassLoader(): ClassLoader = appContext.classLoader.parent!!

    private fun createLocalSdk(
        sdkClassLoader: ClassLoader,
        sdkConfig: LocalSdkConfig,
        versionHandshake: VersionHandshake
    ): LocalSdkProvider {
        try {
            val sdkApiVersion = versionHandshake.perform(sdkClassLoader)
            ResourceRemapping.apply(sdkClassLoader, sdkConfig.resourceRemapping)
            if (ClientFeature.SDK_SANDBOX_CONTROLLER.isAvailable(sdkApiVersion)) {
                val controller = controllerFactory.createControllerFor(sdkConfig)
                SandboxControllerInjector.inject(sdkClassLoader, sdkApiVersion, controller)
            }
            return SdkProviderV1.create(sdkClassLoader, sdkConfig, appContext)
        } catch (ex: Exception) {
            throw LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                "Failed to instantiate local SDK",
                ex
            )
        }
    }

    companion object {
        /**
         * Build chain of [ClassLoaderFactory] that could load SDKs with their resources.
         * Order is important because classloaders normally delegate calls to parent classloader
         * first:
         *  1. [JavaResourcesLoadingClassLoaderFactory] - to provide java resources to classes
         *  loaded by child classloaders;
         *  2a. [FileClassLoaderFactory] - first trying to use factory that extracting SDK Dex
         *  to storage and load it using [dalvik.system.BaseDexClassLoader].
         *  Supports all platform versions (Api14+, minSdkVersion for library).
         *  2b. [InMemorySdkClassLoaderFactory] - fallback for low available space. Trying to load
         *  SDK in-memory using [dalvik.system.InMemoryDexClassLoader].
         *  Supports Api27+ only, fails SDK loading on non-supported platform versions.
         *
         * @param context App context
         * @param lowSpaceThreshold Minimal available space in bytes required to proceed with
         * extracting SDK Dex files.
         *
         * @return SdkLoader that could load SDKs with their resources.
         */
        fun create(
            context: Context,
            controllerFactory: ControllerFactory,
            lowSpaceThreshold: Long = 100 * 1024 * 1024
        ): SdkLoader {
            val cachedLocalSdkStorage = CachedLocalSdkStorage.create(
                context,
                lowSpaceThreshold
            )
            val classLoaderFactory = JavaResourcesLoadingClassLoaderFactory(
                context.classLoader,
                codeClassLoaderFactory = FileClassLoaderFactory(
                    cachedLocalSdkStorage,
                    fallback = InMemorySdkClassLoaderFactory.create(context)
                )
            )
            return SdkLoader(classLoaderFactory, context, controllerFactory)
        }
    }
}
