/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.web

import android.content.ComponentCallbacks
import android.content.Context
import android.content.MutableContextWrapper
import android.content.res.Configuration
import androidx.annotation.GuardedBy

/**
 * [MutableContextWrapper] that unregisters any remaining [ComponentCallbacks] from its original
 * base context before resetting to an application-backed configuration context. Wrapping
 * `applicationContext` via [Context.createConfigurationContext] satisfies StrictMode's
 * `detectIncorrectContextUse()` checks (such as `ViewConfiguration.get()`) without retaining the
 * original [android.app.Activity].
 */
internal class WebContentContextWrapper(base: Context) : MutableContextWrapper(base) {
    private val lock = Any()
    @GuardedBy("lock")
    private var registeredCallbacks: MutableList<ComponentCallbacks>? = mutableListOf()

    override fun registerComponentCallbacks(callback: ComponentCallbacks) {
        synchronized(lock) {
            super.registerComponentCallbacks(callback)
            registeredCallbacks?.add(callback)
        }
    }

    override fun unregisterComponentCallbacks(callback: ComponentCallbacks) {
        synchronized(lock) {
            super.unregisterComponentCallbacks(callback)
            registeredCallbacks?.remove(callback)
        }
    }

    fun cleanUp() {
        synchronized(lock) {
            val callbacks = registeredCallbacks ?: return
            registeredCallbacks = null
            try {
                callbacks.forEach { baseContext.unregisterComponentCallbacks(it) }
            } finally {
                baseContext.applicationContext?.let {
                    baseContext = it.createConfigurationContext(Configuration())
                }
            }
        }
    }
}
