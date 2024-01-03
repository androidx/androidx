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

import android.app.Activity
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Create proxy of [ActivityHolder] that implements same interface loaded by SDK Classloader.
 */
internal class ActivityHolderProxyFactory private constructor(
    private val sdkClassLoader: ClassLoader,

    private val activityHolderClass: Class<*>,

    private val backPressedDispatcherProxyFactory: BackPressedDispatcherProxyFactory,

    private val lifecycleRegistryProxyFactory: LifecycleRegistryProxyFactory,
) {

    fun createProxyFor(activityHolder: ActivityHolder): Any {
        val dispatcherProxy = backPressedDispatcherProxyFactory.setupOnBackPressedDispatcherProxy(
            activityHolder.getOnBackPressedDispatcher()
        )

        val handler = ActivityHolderHandler(
            activityHolder.getActivity(),
            dispatcherProxy
        )

        val activityHolderProxy = Proxy.newProxyInstance(
            sdkClassLoader,
            arrayOf(activityHolderClass),
            handler
        )

        val lifecycleProxy = lifecycleRegistryProxyFactory.setupLifecycleProxy(
            activityHolderProxy,
            activityHolder.lifecycle
        )
        handler.lifecycleProxy = lifecycleProxy

        return activityHolderProxy
    }

    private class ActivityHolderHandler(
        private val activity: Activity,
        private val onBackPressedDispatcherProxy: Any,
    ) : InvocationHandler {

        var lifecycleProxy: Any? = null

        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any {
            return when (method.name) {
                "equals" -> proxy === args?.get(0)
                "hashCode" -> hashCode()
                "toString" -> toString()
                "getActivity" -> activity
                "getOnBackPressedDispatcher" -> onBackPressedDispatcherProxy
                "getLifecycle" -> lifecycleProxy!!
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$proxy, method: $method, args: $args"
                    )
                }
            }
        }
    }

    companion object {
        fun createFor(classLoader: ClassLoader): ActivityHolderProxyFactory {
            val activityHolderClass = Class.forName(
                ActivityHolder::class.java.name,
                /* initialize = */ false,
                classLoader
            )

            val lifecycleRegistryProxyFactory = LifecycleRegistryProxyFactory.createFor(classLoader)
            val backPressedDispatcherProxyFactory =
                BackPressedDispatcherProxyFactory.createFor(classLoader)

            return ActivityHolderProxyFactory(
                sdkClassLoader = classLoader,
                activityHolderClass = activityHolderClass,
                backPressedDispatcherProxyFactory = backPressedDispatcherProxyFactory,
                lifecycleRegistryProxyFactory = lifecycleRegistryProxyFactory
            )
        }
    }
}
