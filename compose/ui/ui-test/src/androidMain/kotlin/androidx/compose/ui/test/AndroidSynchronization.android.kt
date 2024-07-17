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

package androidx.compose.ui.test

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * Runs the given action on the UI thread.
 *
 * This method is blocking until the action is complete.
 *
 * @throws Throwable Any exception that is thrown on the UI thread during execution of [action]. The
 *   thrown exception contains a suppressed [ExecutionException] that contains the stacktrace on the
 *   calling side.
 */
internal fun <T> runOnUiThread(action: () -> T): T {
    if (isOnUiThread()) {
        return action()
    }

    // Note: This implementation is directly taken from ActivityTestRule
    val task: FutureTask<T> = FutureTask(action)
    InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
    try {
        return task.get()
    } catch (e: ExecutionException) {
        // Throw the original exception, but add a new ExecutionException as a suppressed error
        // to expose the caller's thread's stacktrace. We have to create a new ExecutionException
        // to be able to remove the cause, for otherwise we would create a circular reference
        // (cause --suppresses--> e --causedBy--> cause --suppresses--> e --etc-->)
        throw e.cause?.also {
            it.addSuppressed(
                ExecutionException(
                    "An Exception occurred on the UI thread during runOnUiThread()",
                    null
                )
            )
        } ?: e
    }
}

/** Returns if the call is made on the main thread. */
internal fun isOnUiThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
}
