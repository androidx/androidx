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
package androidx.privacysandbox.sdkruntime.core

import android.annotation.SuppressLint
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.content.Context
import android.os.Bundle
import android.os.ext.SdkExtensions.AD_SERVICES
import android.view.View
import androidx.annotation.RequiresExtension

/**
 * Implementation of platform [SandboxedSdkProvider] that delegate to [SandboxedSdkProviderCompat]
 * Gets compat class name from asset "SandboxedSdkProviderCompatClassName.txt"
 *
 */
@SuppressLint("Override") // b/273473397
@RequiresExtension(extension = AD_SERVICES, version = 4)
class SandboxedSdkProviderAdapter internal constructor(
    private val classNameProvider: CompatClassNameProvider
) : SandboxedSdkProvider() {

    /**
     * Provides classname of [SandboxedSdkProviderCompat] implementation.
     */
    internal interface CompatClassNameProvider {
        fun getCompatProviderClassName(context: Context): String
    }

    constructor () : this(DefaultClassNameProvider())

    internal val delegate: SandboxedSdkProviderCompat by lazy {
        val currentContext = context!!
        val compatSdkProviderClassName =
            classNameProvider.getCompatProviderClassName(currentContext)
        val clz = Class.forName(compatSdkProviderClassName)
        val newDelegate = clz.getConstructor().newInstance() as SandboxedSdkProviderCompat
        newDelegate.attachContext(currentContext)
        newDelegate
    }

    @Throws(LoadSdkException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdk {
        return try {
            delegate.onLoadSdk(params).toSandboxedSdk()
        } catch (e: LoadSdkCompatException) {
            throw e.toLoadSdkException()
        }
    }

    override fun beforeUnloadSdk() {
        delegate.beforeUnloadSdk()
    }

    override fun getView(
        windowContext: Context,
        params: Bundle,
        width: Int,
        height: Int
    ): View {
        return delegate.getView(windowContext, params, width, height)
    }

    private class DefaultClassNameProvider : CompatClassNameProvider {
        override fun getCompatProviderClassName(context: Context): String {
            // TODO(b/257966930) Read classname from SDK manifest property
            return context.assets.open(COMPAT_SDK_PROVIDER_CLASS_ASSET_NAME)
                .use { inputStream ->
                    inputStream.bufferedReader().readLine()
                }
        }
    }

    private companion object {
        private const val COMPAT_SDK_PROVIDER_CLASS_ASSET_NAME =
            "SandboxedSdkProviderCompatClassName.txt"
    }
}
