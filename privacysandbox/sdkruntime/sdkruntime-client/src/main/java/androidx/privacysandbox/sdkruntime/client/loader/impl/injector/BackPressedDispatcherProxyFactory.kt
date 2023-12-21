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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Create instance of [OnBackPressedDispatcher] class loaded by SDK Classloader.
 * Set callback for [OnBackPressedDispatcher.onHasEnabledCallbacksChanged] and enable/disable
 * callback in original [OnBackPressedDispatcher].
 * Proxy [OnBackPressedDispatcher.onBackPressed] from original dispatcher to proxy.
 */
internal class BackPressedDispatcherProxyFactory(
    private val onBackPressedDispatcherConstructor: Constructor<out Any>,
    private val consumerClass: Class<*>,
    private val dispatcherOnBackPressedMethod: Method,
    private val sdkClassLoader: ClassLoader
) {
    fun setupOnBackPressedDispatcherProxy(
        sourceDispatcher: OnBackPressedDispatcher
    ): Any {
        val enabledChangedHandler = OnHasEnabledCallbacksChangedHandler()

        val onHasEnabledCallbacksChangedCallback = Proxy.newProxyInstance(
            sdkClassLoader,
            arrayOf(consumerClass),
            enabledChangedHandler
        )
        val dispatcherProxy = onBackPressedDispatcherConstructor.newInstance(
            /* parameter1 */ null,
            /* parameter2 */ onHasEnabledCallbacksChangedCallback
        )

        val sourceDispatcherCallback =
            SourceDispatcherCallback(dispatcherProxy, dispatcherOnBackPressedMethod)

        enabledChangedHandler.sourceDispatcherCallback = sourceDispatcherCallback

        sourceDispatcher.addCallback(sourceDispatcherCallback)

        return dispatcherProxy
    }

    private class OnHasEnabledCallbacksChangedHandler : InvocationHandler {

        var sourceDispatcherCallback: OnBackPressedCallback? = null

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any {
            return when (method.name) {
                "equals" -> proxy === args?.get(0)
                "hashCode" -> hashCode()
                "toString" -> toString()
                "accept" -> sourceDispatcherCallback?.isEnabled = args!![0] as Boolean
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$proxy, method: $method, args: $args"
                    )
                }
            }
        }
    }

    private class SourceDispatcherCallback(
        private val dispatcherProxy: Any,
        private val dispatcherOnBackPressedMethod: Method,
    ) : OnBackPressedCallback(false) {

        @SuppressLint("BanUncheckedReflection") // using reflection on library classes
        override fun handleOnBackPressed() {
            dispatcherOnBackPressedMethod.invoke(dispatcherProxy)
        }
    }

    companion object {
        fun createFor(classLoader: ClassLoader): BackPressedDispatcherProxyFactory {
            val onBackPressedDispatcherClass = Class.forName(
                "androidx.activity.OnBackPressedDispatcher",
                /* initialize = */ false,
                classLoader
            )

            val consumerClass = Class.forName(
                "androidx.core.util.Consumer",
                /* initialize = */ false,
                classLoader
            )

            val onBackPressedDispatcherConstructor = onBackPressedDispatcherClass.getConstructor(
                /* parameter1 */ Runnable::class.java,
                /* parameter2 */ consumerClass
            )

            val dispatcherOnBackPressedMethod = onBackPressedDispatcherClass
                .getMethod("onBackPressed")

            return BackPressedDispatcherProxyFactory(
                onBackPressedDispatcherConstructor,
                consumerClass,
                dispatcherOnBackPressedMethod,
                classLoader
            )
        }
    }
}
