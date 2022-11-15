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
import androidx.annotation.RestrictTo
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.impl.SdkV1
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException

/**
 * Load SDK bundled with App.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SdkLoader internal constructor(
    private val classLoaderFactory: ClassLoaderFactory,
    private val appContext: Context
) {

    internal interface ClassLoaderFactory {
        fun loadSdk(sdkConfig: LocalSdkConfig, parent: ClassLoader): ClassLoader
    }

    /**
     * Loading SDK in separate classloader:
     *  1. Create classloader for sdk;
     *  2. Performing handshake to determine api version;
     *  3. Select [LocalSdk] implementation that could work with that api version.
     *
     * @param sdkConfig sdk to load
     * @return LocalSdk implementation for loaded SDK
     */
    fun loadSdk(sdkConfig: LocalSdkConfig): LocalSdk {
        val classLoader = classLoaderFactory.loadSdk(sdkConfig, getParentClassLoader())
        return createLocalSdk(classLoader, sdkConfig.entryPoint)
    }

    private fun getParentClassLoader(): ClassLoader = appContext.classLoader.parent!!

    private fun createLocalSdk(classLoader: ClassLoader?, sdkProviderClassName: String): LocalSdk {
        try {
            val apiVersion = VersionHandshake.perform(classLoader)
            if (apiVersion >= 1) {
                return SdkV1.create(classLoader, sdkProviderClassName, appContext)
            }
        } catch (ex: Exception) {
            throw LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                "Failed to instantiate local SDK",
                ex
            )
        }

        throw LoadSdkCompatException(
            LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
            "Incorrect Api version"
        )
    }

    companion object {
        /**
         * Build chain of [ClassLoaderFactory] that could load SDKs with their resources.
         * Order is important because classloaders normally delegate calls to parent classloader
         * first:
         *  1. [JavaResourcesLoadingClassLoaderFactory] - to provide java resources to classes
         *  loaded by child classloaders;
         *  2. [InMemorySdkClassLoaderFactory] - to load SDK classes.
         *
         * @return SdkLoader that could load SDKs with their resources.
         */
        fun create(context: Context): SdkLoader {
            val classLoaderFactory = JavaResourcesLoadingClassLoaderFactory(context.classLoader)
                .andThen(
                    InMemorySdkClassLoaderFactory.create(context)
                )
            return SdkLoader(classLoaderFactory, context)
        }

        private fun ClassLoaderFactory.andThen(next: ClassLoaderFactory): ClassLoaderFactory {
            val prev = this
            return object : ClassLoaderFactory {
                override fun loadSdk(sdkConfig: LocalSdkConfig, parent: ClassLoader): ClassLoader =
                    next.loadSdk(sdkConfig, prev.loadSdk(sdkConfig, parent))
            }
        }
    }
}