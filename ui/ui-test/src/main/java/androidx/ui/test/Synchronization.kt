/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import android.annotation.SuppressLint
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.test.android.SynchronizedTreeCollector
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * Runs the given action on the UI thread.
 *
 * This method is blocking until the action is complete.
 */
@SuppressLint("DocumentExceptions")
fun <T> runOnUiThread(action: () -> T): T {
    if (isOnUiThread()) {
        return action()
    }

    // Note: This implementation is directly taken from ActivityTestRule
    val task: FutureTask<T> = FutureTask(action)
    InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
    try {
        return task.get()
    } catch (e: ExecutionException) { // Expose the original exception
        throw e.cause!!
    }
}

/**
 * Returns if the call is made on the main thread.
 */
internal fun isOnUiThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
}

/**
 * Executes the given action in the same way as [runOnUiThread] but also makes sure Compose
 * is idle before executing it. This is great place for doing your assertions on shared
 * variables.
 *
 * This method is blocking until the action is complete.
 */
fun <T> runOnIdleCompose(action: () -> T): T {
    // Method below make sure that compose is idle.
    waitForIdle()
    // Execute the action on ui thread in a blocking way.
    return runOnUiThread(action)
}

/**
 * Waits for compose to be idle.
 *
 * This is a blocking call. Returns only after compose is idle.
 *
 * Can crash in case Espresso hits time out. This is not supposed to be handled as it
 * surfaces only in incorrect tests.
 */
fun waitForIdle() {
    SynchronizedTreeCollector.waitForIdle()
}
