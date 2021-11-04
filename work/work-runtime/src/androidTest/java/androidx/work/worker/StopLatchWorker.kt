/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StopLatchWorker(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {

    private val latch = CountDownLatch(1)

    override fun doWork(): Result {
        while (latch.count > 0) {
            // do nothing.
        }
        return Result.success()
    }

    fun countDown() {
        latch.countDown()
    }

    override fun onStopped() {
        latch.await(10, TimeUnit.SECONDS)
    }
}
