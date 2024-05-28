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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.toLoadCompatSdkException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import java.util.concurrent.Executor

/**
 * Trying to load SDK using [SdkSandboxController]. Throws [LoadSdkCompatException] if loading SDK
 * not supported in [SdkSandboxController].
 */
@RequiresApi(34)
internal class PlatformSdkLoader private constructor(private val loaderImpl: LoaderImpl) {

    fun loadSdk(sdkName: String, params: Bundle, executor: Executor, receiver: LoadSdkCallback) {
        loaderImpl.loadSdk(sdkName, params, executor, receiver)
    }

    private interface LoaderImpl {
        fun loadSdk(sdkName: String, params: Bundle, executor: Executor, callback: LoadSdkCallback)
    }

    /** Implementation for cases when API not supported by [SdkSandboxController] */
    private object FailImpl : LoaderImpl {
        override fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback
        ) {
            executor.execute {
                callback.onError(
                    LoadSdkCompatException(
                        LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                        "Loading SDK not supported on this device"
                    )
                )
            }
        }
    }

    /** Implementation for AdServices V10. */
    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    private class ApiAdServicesV10Impl(private val controller: SdkSandboxController) : LoaderImpl {
        @DoNotInline
        override fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback
        ) {
            controller.loadSdk(
                sdkName,
                params,
                executor,
                object : OutcomeReceiver<SandboxedSdk, LoadSdkException> {
                    override fun onResult(result: SandboxedSdk) {
                        callback.onResult(SandboxedSdkCompat(result))
                    }

                    override fun onError(error: LoadSdkException) {
                        callback.onError(toLoadCompatSdkException(error))
                    }
                }
            )
        }
    }

    companion object {
        fun create(controller: SdkSandboxController): PlatformSdkLoader {
            return if (BuildCompat.AD_SERVICES_EXTENSION_INT >= 10) {
                PlatformSdkLoader(ApiAdServicesV10Impl(controller))
            } else {
                PlatformSdkLoader(FailImpl)
            }
        }
    }
}
