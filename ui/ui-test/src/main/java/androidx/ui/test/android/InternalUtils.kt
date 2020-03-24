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

package androidx.ui.test.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch

internal fun <R> Handler.runAndAwait(command: () -> R): R {
    if (looper === Looper.myLooper()) {
        return command()
    } else {
        val latch = CountDownLatch(1)
        var result = Result.failure<R>(Throwable("runAndAwait got neither a result nor a crash"))
        post {
            // Don't lift assignment out of 'try': count down must happen *after* assignment
            try {
                result = Result.success(command())
            } catch (t: Throwable) {
                result = Result.failure(t)
            } finally {
                latch.countDown()
            }
        }
        latch.await()
        return result.getOrThrow()
    }
}
