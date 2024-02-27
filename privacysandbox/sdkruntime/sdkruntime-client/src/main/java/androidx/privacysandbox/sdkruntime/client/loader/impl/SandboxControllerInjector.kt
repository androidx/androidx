/*
 * Copyright 2023 The Android Open Source Project
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
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.AppOwnedSdkInterfaceProxyFactory
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.LoadSdkCallbackWrapper
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.SandboxedSdkCompatProxyFactory
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.SdkActivityHandlerWrapper
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Injects local implementation of [SdkSandboxControllerCompat.SandboxControllerImpl]
 * to [SdkSandboxControllerCompat] loaded by SDK Classloader.
 * Using [Proxy] to allow interaction between classes loaded by different classloaders.
 */
internal object SandboxControllerInjector {

    /**
     * Injects local implementation to SDK instance of [SdkSandboxControllerCompat].
     * 1) Retrieve [SdkSandboxControllerCompat] loaded by [sdkClassLoader]
     * 2) Create proxy that implements class from (1) and delegate to [controller]
     * 3) Call (via reflection) [SdkSandboxControllerCompat.injectLocalImpl] with proxy from (2)
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    fun inject(
        sdkClassLoader: ClassLoader,
        sdkVersion: Int,
        controller: SdkSandboxControllerCompat.SandboxControllerImpl
    ) {
        val controllerClass = Class.forName(
            SdkSandboxControllerCompat::class.java.name,
            false,
            sdkClassLoader
        )

        val controllerImplClass = Class.forName(
            SdkSandboxControllerCompat.SandboxControllerImpl::class.java.name,
            false,
            sdkClassLoader
        )

        val injectMethod = controllerClass.getMethod("injectLocalImpl", controllerImplClass)

        val sandboxedSdkCompatProxyFactory =
            SandboxedSdkCompatProxyFactory.createFor(sdkClassLoader)

        val sdkActivityHandlerWrapper = if (sdkVersion >= 3)
            SdkActivityHandlerWrapper.createFor(sdkClassLoader)
        else
            null

        val appOwnedSdkInterfaceProxyFactory = if (sdkVersion >= 4)
            AppOwnedSdkInterfaceProxyFactory.createFor(sdkClassLoader)
        else
            null

        val loadSdkCallbackWrapper = if (sdkVersion >= 5)
            LoadSdkCallbackWrapper.createFor(sdkClassLoader)
        else
            null

        val proxy = Proxy.newProxyInstance(
            sdkClassLoader,
            arrayOf(controllerImplClass),
            Handler(
                controller,
                sandboxedSdkCompatProxyFactory,
                appOwnedSdkInterfaceProxyFactory,
                sdkActivityHandlerWrapper,
                loadSdkCallbackWrapper
            )
        )

        injectMethod.invoke(null, proxy)
    }

    private class Handler(
        private val controller: SdkSandboxControllerCompat.SandboxControllerImpl,
        private val sandboxedSdkCompatProxyFactory: SandboxedSdkCompatProxyFactory,
        private val appOwnedSdkInterfaceProxyFactory: AppOwnedSdkInterfaceProxyFactory?,
        private val sdkActivityHandlerWrapper: SdkActivityHandlerWrapper?,
        private val loadSdkCallbackWrapper: LoadSdkCallbackWrapper?
    ) : InvocationHandler {

        private val sdkToAppHandlerMap =
            hashMapOf<Any, SdkSandboxActivityHandlerCompat>()

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any {
            return when (method.name) {
                "loadSdk" -> loadSdk(args!![0]!!, args[1]!!, args[2]!!, args[3]!!)

                "getSandboxedSdks" -> getSandboxedSdks()

                "getAppOwnedSdkSandboxInterfaces" -> getAppOwnedSdkSandboxInterfaces()

                "registerSdkSandboxActivityHandler" ->
                    registerSdkSandboxActivityHandler(args!![0]!!)

                "unregisterSdkSandboxActivityHandler" ->
                    unregisterSdkSandboxActivityHandler(args!![0]!!)

                "equals" -> proxy === args?.get(0)

                "hashCode" -> hashCode()

                "toString" -> toString()

                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$proxy, method: $method, args: $args"
                    )
                }
            }
        }

        private fun loadSdk(
            sdkName: Any,
            params: Any,
            executor: Any,
            originalCallback: Any
        ) {
            if (loadSdkCallbackWrapper == null) {
                throw IllegalStateException(
                    "Unexpected call from SDK without LoadSdk support"
                )
            }

            val callback = loadSdkCallbackWrapper.wrapLoadSdkCallback(originalCallback)
            controller
                .loadSdk(sdkName as String, params as Bundle, executor as Executor, callback)
        }

        private fun getSandboxedSdks(): List<Any> {
            return controller
                .getSandboxedSdks()
                .map { sandboxedSdkCompatProxyFactory.createFrom(it) }
        }

        private fun getAppOwnedSdkSandboxInterfaces(): List<Any> {
            if (appOwnedSdkInterfaceProxyFactory == null) {
                throw IllegalStateException(
                    "Unexpected call from SDK without AppOwnedInterfaces support"
                )
            }

            return controller
                .getAppOwnedSdkSandboxInterfaces()
                .map { appOwnedSdkInterfaceProxyFactory.createFrom(it) }
        }

        private fun registerSdkSandboxActivityHandler(sdkSideHandler: Any): Any {
            val handlerToRegister = wrapSdkActivityHandler(sdkSideHandler)
            return controller
                .registerSdkSandboxActivityHandler(handlerToRegister)
        }

        private fun unregisterSdkSandboxActivityHandler(sdkSideHandler: Any) {
            val appSideHandler = synchronized(sdkToAppHandlerMap) {
                sdkToAppHandlerMap.remove(sdkSideHandler)
            }
            if (appSideHandler != null) {
                controller
                    .unregisterSdkSandboxActivityHandler(appSideHandler)
            }
        }

        private fun wrapSdkActivityHandler(sdkSideHandler: Any): SdkSandboxActivityHandlerCompat =
            synchronized(sdkToAppHandlerMap) {
                if (sdkActivityHandlerWrapper == null) {
                    throw IllegalStateException(
                        "Unexpected call from SDK without Activity support"
                    )
                }

                val existingAppSideHandler = sdkToAppHandlerMap[sdkSideHandler]
                if (existingAppSideHandler != null) {
                    return existingAppSideHandler
                }

                val appSideHandler =
                    sdkActivityHandlerWrapper.wrapSdkSandboxActivityHandlerCompat(sdkSideHandler)

                sdkToAppHandlerMap[sdkSideHandler] = appSideHandler

                return appSideHandler
            }
    }
}
