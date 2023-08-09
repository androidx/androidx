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
package androidx.privacysandbox.sdkruntime.client.loader.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.client.config.LocalSdkConfig
import androidx.privacysandbox.sdkruntime.client.loader.LocalSdkProvider
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Provides interface for interaction with locally loaded SDK with ApiVersion 1.
 *
 */
internal class SdkProviderV1 private constructor(
    sdkProvider: Any,

    private val onLoadSdkMethod: Method,
    private val beforeUnloadSdkMethod: Method,

    private val sandboxedSdkCompatBuilder: SandboxedSdkCompatBuilderV1,
    private val loadSdkCompatExceptionBuilder: LoadSdkCompatExceptionBuilderV1
) : LocalSdkProvider(sdkProvider) {

    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        try {
            val rawResult = onLoadSdkMethod.invoke(sdkProvider, params)
            return sandboxedSdkCompatBuilder.build(rawResult!!)
        } catch (e: InvocationTargetException) {
            throw loadSdkCompatExceptionBuilder.tryRebuildCompatException(e.targetException)
        } catch (ex: Exception) {
            throw LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                "Failed during onLoadSdk call",
                ex
            )
        }
    }

    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    override fun beforeUnloadSdk() {
        beforeUnloadSdkMethod.invoke(sdkProvider)
    }

    internal class SandboxedSdkCompatBuilderV1 private constructor(
        private val sdkInfo: SandboxedSdkInfo?,
        private val getInterfaceMethod: Method
    ) {

        @SuppressLint("BanUncheckedReflection") // calling method on SandboxedSdkCompat class
        fun build(rawObject: Any): SandboxedSdkCompat {
            val binder = getInterfaceMethod.invoke(rawObject) as IBinder
            return SandboxedSdkCompat(binder, sdkInfo)
        }

        companion object {

            fun create(
                classLoader: ClassLoader,
                sdkConfig: LocalSdkConfig
            ): SandboxedSdkCompatBuilderV1 {
                val sandboxedSdkCompatClass = Class.forName(
                    SandboxedSdkCompat::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
                val getInterfaceMethod = sandboxedSdkCompatClass.getMethod("getInterface")
                val sdkInfo = sdkInfo(sdkConfig)
                return SandboxedSdkCompatBuilderV1(sdkInfo, getInterfaceMethod)
            }

            private fun sdkInfo(sdkConfig: LocalSdkConfig): SandboxedSdkInfo? {
                return if (sdkConfig.versionMajor == null) {
                    null
                } else {
                    SandboxedSdkInfo(sdkConfig.packageName, sdkConfig.versionMajor.toLong())
                }
            }
        }
    }

    internal class LoadSdkCompatExceptionBuilderV1 private constructor(
        private val getLoadSdkErrorCodeMethod: Method,
        private val getExtraInformationMethod: Method
    ) {
        @SuppressLint("BanUncheckedReflection") // calling method on LoadSdkCompatException class
        fun tryRebuildCompatException(rawException: Throwable): Throwable {
            if (rawException.javaClass.name != LoadSdkCompatException::class.java.name) {
                return rawException
            }

            return try {
                val loadSdkErrorCode = getLoadSdkErrorCodeMethod.invoke(rawException) as Int
                val extraInformation = getExtraInformationMethod.invoke(rawException) as Bundle
                LoadSdkCompatException(
                    loadSdkErrorCode,
                    rawException.message,
                    rawException.cause,
                    extraInformation
                )
            } catch (ex: Throwable) {
                // failed to rebuild, just wrap original
                LoadSdkCompatException(
                    LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                    "Failed to rebuild exception with error ${ex.message}",
                    rawException
                )
            }
        }

        companion object {
            fun create(classLoader: ClassLoader): LoadSdkCompatExceptionBuilderV1 {
                val loadSdkCompatExceptionClass = Class.forName(
                    LoadSdkCompatException::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
                val getLoadSdkErrorCodeMethod = loadSdkCompatExceptionClass.getMethod(
                    "getLoadSdkErrorCode"
                )
                val getExtraInformationMethod = loadSdkCompatExceptionClass.getMethod(
                    "getExtraInformation"
                )
                return LoadSdkCompatExceptionBuilderV1(
                    getLoadSdkErrorCodeMethod,
                    getExtraInformationMethod
                )
            }
        }
    }

    companion object {

        @SuppressLint("BanUncheckedReflection") // calling method of SandboxedSdkProviderCompat
        fun create(
            classLoader: ClassLoader,
            sdkConfig: LocalSdkConfig,
            appContext: Context
        ): SdkProviderV1 {
            val sdkProviderClass = Class.forName(
                sdkConfig.entryPoint,
                /* initialize = */ false,
                classLoader
            )
            val attachContextMethod =
                sdkProviderClass.getMethod("attachContext", Context::class.java)
            val onLoadSdkMethod = sdkProviderClass.getMethod("onLoadSdk", Bundle::class.java)
            val beforeUnloadSdkMethod = sdkProviderClass.getMethod("beforeUnloadSdk")
            val sandboxedSdkCompatBuilder =
                SandboxedSdkCompatBuilderV1.create(classLoader, sdkConfig)
            val loadSdkCompatExceptionBuilder =
                LoadSdkCompatExceptionBuilderV1.create(classLoader)

            val sdkProvider = sdkProviderClass.getConstructor().newInstance()
            val sandboxedSdkContextCompat = SandboxedSdkContextCompat(
                appContext,
                sdkConfig.packageName,
                classLoader
            )
            attachContextMethod.invoke(sdkProvider, sandboxedSdkContextCompat)

            return SdkProviderV1(
                sdkProvider,
                onLoadSdkMethod,
                beforeUnloadSdkMethod,
                sandboxedSdkCompatBuilder,
                loadSdkCompatExceptionBuilder
            )
        }
    }
}
