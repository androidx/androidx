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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.jetbrains.annotations.TestOnly

/**
 * Singleton class to store instances of [SdkSandboxClientImportanceListenerCompat] registered by
 * locally loaded SDKs.
 */
internal object LocalClientImportanceListenerRegistry {

    private val listeners = CopyOnWriteArrayList<ListenerInfo>()

    private val initialized = AtomicBoolean(false)

    private val observer =
        object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                notifyOnForegroundImportanceChanged(false)
            }

            override fun onResume(owner: LifecycleOwner) {
                notifyOnForegroundImportanceChanged(true)
            }
        }

    fun register(
        sdkPackageName: String,
        executor: Executor,
        listener: SdkSandboxClientImportanceListenerCompat,
    ) {
        initializeIfNeeded()
        listeners.add(ListenerInfo(sdkPackageName, executor, listener))
    }

    fun unregister(listener: SdkSandboxClientImportanceListenerCompat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Optimised for bulk removal, no need in synchronization
            Api24.removeListener(listeners, listener)
        } else {
            // Synchronized to ensure that no one else trying to remove same indexes
            synchronized(listeners) {
                for (i in listeners.lastIndex downTo 0) {
                    val listenerInfo = listeners[i]
                    if (listenerInfo.listener == listener) {
                        listeners.removeAt(i)
                    }
                }
            }
        }
    }

    fun unregisterAllListenersForSdk(sdkPackageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Optimised for bulk removal, no need in synchronization
            Api24.removeAllListenersForSdk(listeners, sdkPackageName)
        } else {
            // Synchronized to ensure that no one else trying to remove same indexes
            synchronized(listeners) {
                for (i in listeners.lastIndex downTo 0) {
                    val listenerInfo = listeners[i]
                    if (listenerInfo.sdkPackageName == sdkPackageName) {
                        listeners.removeAt(i)
                    }
                }
            }
        }
    }

    @TestOnly
    fun isRegistered(
        sdkPackageName: String,
        executor: Executor,
        listener: SdkSandboxClientImportanceListenerCompat,
    ): Boolean = listeners.contains(ListenerInfo(sdkPackageName, executor, listener))

    @TestOnly
    fun hasListenersForSdk(sdkPackageName: String): Boolean =
        listeners.find { it.sdkPackageName == sdkPackageName } != null

    private fun initializeIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            MainThreadExecutor.execute {
                ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            }
        }
    }

    private fun notifyOnForegroundImportanceChanged(isForeground: Boolean) {
        listeners.forEach { listenerInfo ->
            listenerInfo.executor.execute {
                listenerInfo.listener.onForegroundImportanceChanged(isForeground)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private object Api24 {
        fun removeListener(
            from: CopyOnWriteArrayList<ListenerInfo>,
            listener: SdkSandboxClientImportanceListenerCompat,
        ) {
            from.removeIf { it.listener == listener }
        }

        fun removeAllListenersForSdk(
            from: CopyOnWriteArrayList<ListenerInfo>,
            sdkPackageName: String,
        ) {
            from.removeIf { it.sdkPackageName == sdkPackageName }
        }
    }

    private data class ListenerInfo(
        val sdkPackageName: String,
        val executor: Executor,
        val listener: SdkSandboxClientImportanceListenerCompat,
    )
}
