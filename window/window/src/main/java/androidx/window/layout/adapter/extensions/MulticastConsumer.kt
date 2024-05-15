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

package androidx.window.layout.adapter.extensions

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import androidx.window.extensions.core.util.function.Consumer as OEMConsumer
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import androidx.window.layout.WindowLayoutInfo
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A [Consumer] that handles multicasting to multiple [Consumer]s downstream.
 */
internal class MulticastConsumer(
    private val context: Context
) : Consumer<OEMWindowLayoutInfo>, OEMConsumer<OEMWindowLayoutInfo> {
    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock")
    private var lastKnownValue: WindowLayoutInfo? = null
    @GuardedBy("globalLock")
    private val registeredListeners = mutableSetOf<Consumer<WindowLayoutInfo>>()

    override fun accept(value: OEMWindowLayoutInfo) {
        globalLock.withLock {
            val newValue = ExtensionsWindowLayoutInfoAdapter.translate(context, value)
            lastKnownValue = newValue
            registeredListeners.forEach { consumer -> consumer.accept(newValue) }
        }
    }

    fun addListener(listener: Consumer<WindowLayoutInfo>) {
        globalLock.withLock {
            lastKnownValue?.let { value -> listener.accept(value) }
            registeredListeners.add(listener)
        }
    }

    fun removeListener(listener: Consumer<WindowLayoutInfo>) {
        globalLock.withLock {
            registeredListeners.remove(listener)
        }
    }

    fun isEmpty(): Boolean {
        return registeredListeners.isEmpty()
    }
}
