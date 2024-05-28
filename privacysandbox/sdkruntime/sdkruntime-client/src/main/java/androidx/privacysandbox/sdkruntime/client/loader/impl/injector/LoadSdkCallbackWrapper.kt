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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import android.annotation.SuppressLint
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import java.lang.reflect.Method

/**
 * Creates reflection wrapper for implementation of [LoadSdkCallback] interface loaded by SDK
 * classloader.
 */
internal class LoadSdkCallbackWrapper
private constructor(
    private val callbackOnResultMethod: Method,
    private val callbackOnErrorMethod: Method,
    private val sandboxedSdkFactory: SandboxedSdkCompatProxyFactory,
    private val loadSdkExceptionFactory: LoadSdkCompatExceptionProxyFactory
) {

    fun wrapLoadSdkCallback(originalCallback: Any): LoadSdkCallback =
        WrappedCallback(
            originalCallback,
            callbackOnResultMethod,
            callbackOnErrorMethod,
            sandboxedSdkFactory,
            loadSdkExceptionFactory
        )

    private class WrappedCallback(
        private val originalCallback: Any,
        private val callbackOnResultMethod: Method,
        private val callbackOnErrorMethod: Method,
        private val sandboxedSdkFactory: SandboxedSdkCompatProxyFactory,
        private val loadSdkExceptionFactory: LoadSdkCompatExceptionProxyFactory
    ) : LoadSdkCallback {

        @SuppressLint("BanUncheckedReflection") // using reflection on library classes
        override fun onResult(result: SandboxedSdkCompat) {
            val proxyObj = sandboxedSdkFactory.createFrom(result)
            callbackOnResultMethod.invoke(originalCallback, proxyObj)
        }

        @SuppressLint("BanUncheckedReflection") // using reflection on library classes
        override fun onError(error: LoadSdkCompatException) {
            val proxyObj = loadSdkExceptionFactory.createFrom(error)
            callbackOnErrorMethod.invoke(originalCallback, proxyObj)
        }
    }

    companion object {
        fun createFor(classLoader: ClassLoader): LoadSdkCallbackWrapper {
            val loadSdkCallbackClass =
                Class.forName(
                    LoadSdkCallback::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val sandboxedSdkCompatClass =
                Class.forName(
                    SandboxedSdkCompat::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val loadSdkCompatExceptionClass =
                Class.forName(
                    LoadSdkCompatException::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )

            val callbackOnResultMethod =
                loadSdkCallbackClass.getMethod("onResult", sandboxedSdkCompatClass)

            val callbackOnErrorMethod =
                loadSdkCallbackClass.getMethod("onError", loadSdkCompatExceptionClass)

            val sandboxedSdkFactory = SandboxedSdkCompatProxyFactory.createFor(classLoader)
            val loadSdkExceptionFactory = LoadSdkCompatExceptionProxyFactory.createFor(classLoader)

            return LoadSdkCallbackWrapper(
                callbackOnResultMethod = callbackOnResultMethod,
                callbackOnErrorMethod = callbackOnErrorMethod,
                sandboxedSdkFactory = sandboxedSdkFactory,
                loadSdkExceptionFactory = loadSdkExceptionFactory,
            )
        }
    }
}
