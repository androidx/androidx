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
import kotlinx.coroutines.runBlocking

internal object MainDispatcherChecker {
    private var isMainDispatcherAvailable: Boolean = true
    @Volatile
    private var mainDispatcherThread: Thread? = null

    private fun storeMainDispatcherThread() {
        try {
            runBlocking(Dispatchers.Main.immediate) {
                mainDispatcherThread = Thread.currentThread()
            }
        } catch (_: IllegalStateException) {
            // No main dispatchers are present in the classpath
            isMainDispatcherAvailable = false
        }
    }

    fun isMainDispatcherThread(): Boolean {
        if (!isMainDispatcherAvailable) {
            return true
        }
        val currentThread = Thread.currentThread()
        // if the thread has already been retrieved,
        // we can just check whether we are currently running on the same thread
        if (currentThread === mainDispatcherThread) {
            return true
        }
        // if threads do not match, it is either:
        // * field is not initialized yet
        // * Swing's EDT may have changed
        // * it is not the main thread indeed
        // let's recheck to make sure the field has an actual value
        // it is potentially a long operation, but it happens only not on the happy path
        storeMainDispatcherThread()
        return !isMainDispatcherAvailable || currentThread === mainDispatcherThread
    }
}
