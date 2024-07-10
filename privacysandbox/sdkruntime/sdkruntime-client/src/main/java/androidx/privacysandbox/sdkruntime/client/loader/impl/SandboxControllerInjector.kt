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
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Injects local implementation of [SdkSandboxControllerCompat.SandboxControllerImpl] to
 * [SdkSandboxControllerCompat] loaded by SDK Classloader. Using [Proxy] to allow interaction
 * between classes loaded by different classloaders.
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
        val controllerClass =
            Class.forName(SdkSandboxControllerCompat::class.java.name, false, sdkClassLoader)

        val controllerImplClass =
            Class.forName(
                SdkSandboxControllerCompat.SandboxControllerImpl::class.java.name,
                false,
                sdkClassLoader
            )

        val injectMethod = controllerClass.getMethod("injectLocalImpl", controllerImplClass)
        val proxy =
            Proxy.newProxyInstance(
                sdkClassLoader,
                arrayOf(controllerImplClass),
                buildInvocationHandler(controller, sdkClassLoader, sdkVersion)
            )

        injectMethod.invoke(null, proxy)
    }

    /**
     * Creates [InvocationHandler] for SDK side proxy of [SdkSandboxControllerCompat].
     * 1) Convert SDK side arguments to App side arguments
     * 2) Calling App side [controller]
     * 3) Convert App side result object to SDK side result object.
     */
    private fun buildInvocationHandler(
        controller: SdkSandboxControllerCompat.SandboxControllerImpl,
        sdkClassLoader: ClassLoader,
        sdkVersion: Int
    ): InvocationHandler {
        val handlerBuilder = HandlerBuilder()

        val sandboxedSdkFactory = SandboxedSdkCompatProxyFactory.createFor(sdkClassLoader)
        handlerBuilder.addHandlerFor("getSandboxedSdks") {
            controller.getSandboxedSdks().map(sandboxedSdkFactory::createFrom)
        }

        if (ClientFeature.APP_OWNED_INTERFACES.isAvailable(sdkVersion)) {
            val sdkInterfaceFactory = AppOwnedSdkInterfaceProxyFactory.createFor(sdkClassLoader)
            handlerBuilder.addHandlerFor("getAppOwnedSdkSandboxInterfaces") {
                controller.getAppOwnedSdkSandboxInterfaces().map(sdkInterfaceFactory::createFrom)
            }
        }

        if (ClientFeature.SDK_ACTIVITY_HANDLER.isAvailable(sdkVersion)) {
            val sdkHandlerWrapper = SdkActivityHandlerWrapper.createFor(sdkClassLoader)
            val activityMethodsHandler = ActivityMethodsHandler(controller, sdkHandlerWrapper)
            handlerBuilder.addHandlerFor(
                "registerSdkSandboxActivityHandler",
                activityMethodsHandler.registerMethodHandler
            )
            handlerBuilder.addHandlerFor(
                "unregisterSdkSandboxActivityHandler",
                activityMethodsHandler.unregisterMethodHandler
            )
        }

        if (ClientFeature.LOAD_SDK.isAvailable(sdkVersion)) {
            val loadSdkCallbackWrapper = LoadSdkCallbackWrapper.createFor(sdkClassLoader)
            handlerBuilder.addHandlerFor("loadSdk") { args ->
                controller.loadSdk(
                    sdkName = args!![0] as String,
                    params = args[1] as Bundle,
                    executor = args[2] as Executor,
                    callback = loadSdkCallbackWrapper.wrapLoadSdkCallback(args[3]!!)
                )
            }
        }

        if (ClientFeature.GET_CLIENT_PACKAGE_NAME.isAvailable(sdkVersion)) {
            handlerBuilder.addHandlerFor("getClientPackageName") {
                controller.getClientPackageName()
            }
        }

        return handlerBuilder.build()
    }

    fun interface MethodHandler {
        fun onMethodCall(args: Array<out Any?>?): Any?
    }

    private class HandlerBuilder {
        private val methodHandlers = hashMapOf<String, MethodHandler>()

        fun addHandlerFor(methodName: String, handler: MethodHandler) {
            methodHandlers[methodName] = handler
        }

        fun build(): InvocationHandler {
            return Handler(methodHandlers)
        }
    }

    private class Handler(private val methodHandlers: Map<String, MethodHandler>) :
        InvocationHandler {

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
            val handler = methodHandlers[method.name]
            if (handler != null) {
                return handler.onMethodCall(args)
            }
            return when (method.name) {
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
    }

    private class ActivityMethodsHandler(
        private val controller: SdkSandboxControllerCompat.SandboxControllerImpl,
        private val sdkActivityHandlerWrapper: SdkActivityHandlerWrapper
    ) {
        val registerMethodHandler = MethodHandler { args ->
            registerSdkSandboxActivityHandler(sdkSideHandler = args!![0]!!)
        }
        val unregisterMethodHandler = MethodHandler { args ->
            unregisterSdkSandboxActivityHandler(sdkSideHandler = args!![0]!!)
        }

        private val sdkToAppHandlerMap = hashMapOf<Any, SdkSandboxActivityHandlerCompat>()

        private fun registerSdkSandboxActivityHandler(sdkSideHandler: Any): Any {
            val handlerToRegister = wrapSdkActivityHandler(sdkSideHandler)
            return controller.registerSdkSandboxActivityHandler(handlerToRegister)
        }

        private fun unregisterSdkSandboxActivityHandler(sdkSideHandler: Any) {
            val appSideHandler =
                synchronized(sdkToAppHandlerMap) { sdkToAppHandlerMap.remove(sdkSideHandler) }
            if (appSideHandler != null) {
                controller.unregisterSdkSandboxActivityHandler(appSideHandler)
            }
        }

        private fun wrapSdkActivityHandler(sdkSideHandler: Any): SdkSandboxActivityHandlerCompat =
            synchronized(sdkToAppHandlerMap) {
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
