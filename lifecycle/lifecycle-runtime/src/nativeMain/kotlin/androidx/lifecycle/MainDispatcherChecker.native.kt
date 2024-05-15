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

import kotlin.native.concurrent.ThreadLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@ThreadLocal
private var isMainDispatcherThread = false

internal object MainDispatcherChecker {
    private val isMainDispatcherAvailable: Boolean

    init {
        isMainDispatcherAvailable = try {
            runBlocking {
                launch(Dispatchers.Main.immediate) {
                    isMainDispatcherThread = true
                }
            }
            true
        } catch (_: NotImplementedError) {
            // Isn't available on some targets (for example, Linux)
            false
        }
    }

    fun isMainDispatcherThread(): Boolean = if (isMainDispatcherAvailable) {
        isMainDispatcherThread
    } else {
        true
    }
}
