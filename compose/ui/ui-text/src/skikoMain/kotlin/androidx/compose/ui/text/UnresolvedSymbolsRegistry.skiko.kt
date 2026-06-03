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

package androidx.compose.ui.text

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.text.platform.SynchronizedObject
import androidx.compose.ui.text.platform.synchronized

@InternalComposeApi
class UnresolvedSymbolsRegistry {

    @InternalComposeApi
    interface Listener {
        fun onUnresolvedCodepoints(codepoints: Set<Int>) {}
        fun onNewFontInstalled() {}
    }

    private val lock = SynchronizedObject()
    private val unresolvedCodepoints = mutableSetOf<Int>()
    private val listeners = mutableListOf<WeakReference<Listener>>()

    fun addListener(listener: Listener) {
        synchronized(lock) {
            listeners.add(WeakReference(listener))
        }
    }

    fun removeListener(listener: Listener) {
        synchronized(lock) {
            listeners.removeAll { it.get() == listener }
        }
    }

    fun addUnresolvedCodepoints(codepoints: IntArray) {
        synchronized(lock) {
            val new = codepoints.filter { it !in unresolvedCodepoints }
            if (new.isEmpty()) return
            unresolvedCodepoints.addAll(new)
            listeners.removeAll { it.get() == null }
            listeners.forEach { it.get()?.onUnresolvedCodepoints(unresolvedCodepoints) }
        }
    }

    fun onNewFontInstalled() {
        synchronized(lock) {
            unresolvedCodepoints.clear()
            listeners.removeAll { it.get() == null }
            listeners.forEach { it.get()?.onNewFontInstalled() }
        }
    }
}