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

package androidx.work.inspection.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class IdleWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    @OptIn(kotlin.time.ExperimentalTime::class)
    override fun doWork(): Result {
        runBlocking {
            delay(Duration.INFINITE)
        }
        return Result.success()
    }
}
