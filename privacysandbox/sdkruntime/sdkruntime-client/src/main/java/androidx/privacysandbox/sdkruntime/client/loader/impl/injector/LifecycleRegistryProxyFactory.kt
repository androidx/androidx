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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * Create proxy of [Lifecycle] that implements same interface loaded by SDK Classloader.
 * Proxy [Lifecycle.Event] from original object to proxy.
 */
internal class LifecycleRegistryProxyFactory private constructor(
    private val lifecycleRegistryConstructor: Constructor<out Any>,
    private val lifecycleEventInstances: Map<String, Any>,
    private val handleLifecycleEventMethod: Method
) {
    fun setupLifecycleProxy(
        activityHolderProxy: Any,
        sourceLifecycle: Lifecycle
    ): Any {
        val registry = lifecycleRegistryConstructor.newInstance(activityHolderProxy)
        sourceLifecycle.addObserver(object : LifecycleEventObserver {
            @SuppressLint("BanUncheckedReflection") // using reflection on library classes
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                val enumInstance = lifecycleEventInstances[event.name]
                if (enumInstance != null) {
                    handleLifecycleEventMethod.invoke(registry, enumInstance)
                }
            }
        })
        return registry
    }

    companion object {
        fun createFor(classLoader: ClassLoader): LifecycleRegistryProxyFactory {
            val lifecycleOwnerClass = Class.forName(
                "androidx.lifecycle.LifecycleOwner",
                /* initialize = */ false,
                classLoader
            )
            val lifecycleRegistryClass = Class.forName(
                "androidx.lifecycle.LifecycleRegistry",
                /* initialize = */ false,
                classLoader
            )
            val lifecycleRegistryConstructor =
                lifecycleRegistryClass.getConstructor(
                    /* parameter1 */ lifecycleOwnerClass
                )

            val lifecycleEventEnum =
                Class.forName(
                    Lifecycle.Event::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val lifecycleEventInstances = lifecycleEventEnum
                .enumConstants
                .filterIsInstance(Enum::class.java)
                .associateBy({ it.name }, { it })

            val handleLifecycleEventMethod = lifecycleRegistryClass.getMethod(
                "handleLifecycleEvent",
                /* parameter1 */ lifecycleEventEnum
            )

            return LifecycleRegistryProxyFactory(
                lifecycleRegistryConstructor,
                lifecycleEventInstances,
                handleLifecycleEventMethod
            )
        }
    }
}
