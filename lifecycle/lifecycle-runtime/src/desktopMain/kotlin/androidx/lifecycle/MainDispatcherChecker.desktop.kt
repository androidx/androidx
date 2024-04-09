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
package androidx.lifecycle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal object MainDispatcherChecker {
    private val isMainDispatcherAvailable: Boolean
    private var isMainDispatcherThread = ThreadLocal.withInitial { false }

    init {
        isMainDispatcherAvailable = try {
            runBlocking {
                launch(Dispatchers.Main.immediate) {
                    isMainDispatcherThread.set(true)
                }
            }
            true
        } catch (_: IllegalStateException) {
            // No main dispatchers are present in the classpath
            false
        }
    }

    fun isMainDispatcherThread(): Boolean = if (isMainDispatcherAvailable) {
        isMainDispatcherThread.get()
    } else {
        true
    }
}
