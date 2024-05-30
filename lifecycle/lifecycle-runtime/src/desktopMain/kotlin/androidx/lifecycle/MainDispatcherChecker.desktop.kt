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
    @Volatile private var mainDispatcherThread: Thread? = null

    private fun updateMainDispatcherThread() {
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
            // If we know there's no main dispatcher, assume we're on it.
            return true
        }

        val currentThread = Thread.currentThread()
        // If the thread has already been retrieved,
        // we can just check whether we are currently running on the same thread
        if (currentThread === mainDispatcherThread) {
            return true
        }

        // If the current thread doesn't match the stored main dispatcher thread, is is either:
        // * The field may not have been initialized yet.
        // * The Swing Event Dispatch Thread (EDT) may have changed (if applicable).
        // * We're genuinely not executing on the main thread.
        // Let's recheck to obtain the most up-to-date dispatcher reference. The recheck can
        // be time-consuming, but should only occur in less common scenarios.
        updateMainDispatcherThread()

        return !isMainDispatcherAvailable || currentThread === mainDispatcherThread
    }
}
