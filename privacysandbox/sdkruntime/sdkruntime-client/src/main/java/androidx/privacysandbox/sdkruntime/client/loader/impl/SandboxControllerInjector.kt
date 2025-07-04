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
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.ClientImportanceListenerWrapper
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.LoadSdkCallbackWrapper
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.SandboxedSdkCompatProxyFactory
import androidx.privacysandbox.sdkruntime.client.loader.impl.injector.SdkActivityHandlerWrapper
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackend
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackendHolder
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Injects local implementation of [SdkSandboxControllerBackend] to
 * [SdkSandboxControllerBackendHolder] loaded by SDK Classloader. Using [Proxy] to allow interaction
 * between classes loaded by different classloaders.
 */
internal object SandboxControllerInjector {

    /**
     * Injects local implementation to SDK instance of [SdkSandboxControllerBackendHolder].
     * 1) Retrieve [SdkSandboxControllerBackend] class loaded by [sdkClassLoader]
     * 2) Retrieve [SdkSandboxControllerBackendHolder] class loaded by [sdkClassLoader]
     * 3) Create proxy that implements class from (1) and delegate to [controller]
     * 4) Call [SdkSandboxControllerBackendHolder.injectLocalBackend] on (2) with proxy from (3).
     * 4) For legacy versions calls
     *    [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat.injectLocalImpl]
     *    instead.
     */
    @SuppressLint("BanUncheckedReflection") // using reflection on library classes
    fun inject(
        sdkClassLoader: ClassLoader,
        sdkVersion: Int,
        controller: SdkSandboxControllerBackend,
    ) {
        val backendClassName: String
        val holderClassName: String
        val holderInjectMethodName: String

        if (ClientFeature.SDK_SANDBOX_CONTROLLER_BACKEND_HOLDER.isAvailable(sdkVersion)) {
            backendClassName =
                "androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackend"
            holderClassName =
                "androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerBackendHolder"
            holderInjectMethodName = "injectLocalBackend"
        } else {
            backendClassName =
                "androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat\$SandboxControllerImpl"
            holderClassName =
                "androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat"
            holderInjectMethodName = "injectLocalImpl"
        }

        val backendClass = Class.forName(backendClassName, /* initialize= */ false, sdkClassLoader)
        val backendHolderClass =
            Class.forName(holderClassName, /* initialize= */ false, sdkClassLoader)
        val injectMethod = backendHolderClass.getMethod(holderInjectMethodName, backendClass)
        val proxy =
            Proxy.newProxyInstance(
                sdkClassLoader,
                arrayOf(backendClass),
                buildInvocationHandler(controller, sdkClassLoader, sdkVersion),
            )

        injectMethod.invoke(null, proxy)
    }

    /**
     * Creates [InvocationHandler] for SDK side proxy of [SdkSandboxControllerBackend].
     * 1) Convert SDK side arguments to App side arguments
     * 2) Calling App side [controller]
     * 3) Convert App side result object to SDK side result object.
     */
    private fun buildInvocationHandler(
        controller: SdkSandboxControllerBackend,
        sdkClassLoader: ClassLoader,
        sdkVersion: Int,
    ): InvocationHandler {
        val handlerBuilder = HandlerBuilder()

        val sandboxedSdkFactory = SandboxedSdkCompatProxyFactory.createFor(sdkClassLoader)
        handlerBuilder.addHandlerFor("getSandboxedSdks") {
            controller.getSandboxedSdks().map(sandboxedSdkFactory::createFrom)
        }

        val sdkInterfaceFactory = AppOwnedSdkInterfaceProxyFactory.createFor(sdkClassLoader)
        handlerBuilder.addHandlerFor("getAppOwnedSdkSandboxInterfaces") {
            controller.getAppOwnedSdkSandboxInterfaces().map(sdkInterfaceFactory::createFrom)
        }

        val sdkHandlerWrapper = SdkActivityHandlerWrapper.createFor(sdkClassLoader)
        val activityMethodsHandler = ActivityMethodsHandler(controller, sdkHandlerWrapper)
        handlerBuilder.addHandlerFor(
            "registerSdkSandboxActivityHandler",
            activityMethodsHandler.registerMethodHandler,
        )
        handlerBuilder.addHandlerFor(
            "unregisterSdkSandboxActivityHandler",
            activityMethodsHandler.unregisterMethodHandler,
        )

        val loadSdkCallbackWrapper = LoadSdkCallbackWrapper.createFor(sdkClassLoader)
        handlerBuilder.addHandlerFor("loadSdk") { args ->
            controller.loadSdk(
                sdkName = args!![0] as String,
                params = args[1] as Bundle,
                executor = args[2] as Executor,
                callback = loadSdkCallbackWrapper.wrapLoadSdkCallback(args[3]!!),
            )
        }

        if (ClientFeature.GET_CLIENT_PACKAGE_NAME.isAvailable(sdkVersion)) {
            handlerBuilder.addHandlerFor("getClientPackageName") {
                controller.getClientPackageName()
            }
        }

        if (ClientFeature.CLIENT_IMPORTANCE_LISTENER.isAvailable(sdkVersion)) {
            val sdkListenerWrapper = ClientImportanceListenerWrapper.createFor(sdkClassLoader)
            val clientImportanceListenerMethodsHandler =
                ClientImportanceListenerMethodsHandler(controller, sdkListenerWrapper)
            handlerBuilder.addHandlerFor(
                "registerSdkSandboxClientImportanceListener",
                clientImportanceListenerMethodsHandler.registerMethodHandler,
            )
            handlerBuilder.addHandlerFor(
                "unregisterSdkSandboxClientImportanceListener",
                clientImportanceListenerMethodsHandler.unregisterMethodHandler,
            )
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
        private val controller: SdkSandboxControllerBackend,
        private val sdkActivityHandlerWrapper: SdkActivityHandlerWrapper,
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

    private class ClientImportanceListenerMethodsHandler(
        private val controller: SdkSandboxControllerBackend,
        private val clientImportanceListenerWrapper: ClientImportanceListenerWrapper,
    ) {
        val registerMethodHandler = MethodHandler { args ->
            registerSdkSandboxClientImportanceListener(
                sdkSideExecutor = args!![0]!!,
                sdkSideListener = args[1]!!,
            )
        }
        val unregisterMethodHandler = MethodHandler { args ->
            unregisterSdkSandboxClientImportanceListener(sdkSideListener = args!![0]!!)
        }

        private val sdkToAppListenerMap = hashMapOf<Any, SdkSandboxClientImportanceListenerCompat>()

        private fun registerSdkSandboxClientImportanceListener(
            sdkSideExecutor: Any,
            sdkSideListener: Any,
        ): Any {
            val listenerToRegister = wrapSdkClientImportanceListener(sdkSideListener)
            return controller.registerSdkSandboxClientImportanceListener(
                sdkSideExecutor as Executor,
                listenerToRegister,
            )
        }

        private fun unregisterSdkSandboxClientImportanceListener(sdkSideListener: Any) {
            val appSideListener =
                synchronized(sdkToAppListenerMap) { sdkToAppListenerMap.remove(sdkSideListener) }
            if (appSideListener != null) {
                controller.unregisterSdkSandboxClientImportanceListener(appSideListener)
            }
        }

        private fun wrapSdkClientImportanceListener(
            sdkSideListener: Any
        ): SdkSandboxClientImportanceListenerCompat =
            synchronized(sdkToAppListenerMap) {
                val existingAppSideListener = sdkToAppListenerMap[sdkSideListener]
                if (existingAppSideListener != null) {
                    return existingAppSideListener
                }

                val appSideListener =
                    clientImportanceListenerWrapper.wrapSdkSandboxClientImportanceListenerCompat(
                        sdkSideListener
                    )

                sdkToAppListenerMap[sdkSideListener] = appSideListener

                return appSideListener
            }
    }
}
