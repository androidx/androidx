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

    fun loadSdk(sdkConfig: LocalSdkConfig): LocalSdk {
        val classLoader = classLoaderFactory.loadSdk(sdkConfig, getParentClassLoader())
        return createLocalSdk(classLoader, sdkConfig.entryPoint)
    }

    private fun getParentClassLoader(): ClassLoader = javaClass.classLoader!!.parent!!

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
        fun create(context: Context): SdkLoader {
            val classLoaderFactory = InMemorySdkClassLoaderFactory.create(context)
            return SdkLoader(classLoaderFactory, context)
        }
    }
}