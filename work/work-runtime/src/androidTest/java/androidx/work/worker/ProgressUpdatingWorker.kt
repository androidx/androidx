/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class ProgressUpdatingWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    companion object {
        const val Expected = "Expected"
        const val Progress = "Progress"
        private const val delayDuration = 1L
    }

    override suspend fun doWork(): Result {
        val firstUpdate = Data.Builder().putInt(Progress, 10).build()
        val lastUpdate = Data.Builder().putInt(Progress, 100).build()
        val expected = inputData.getString(Expected)
        setProgress(firstUpdate)
        delay(delayDuration)
        setProgress(lastUpdate)
        return when (expected) {
            "Failure" -> Result.failure()
            "Retry" -> Result.retry()
            else -> Result.success()
        }
    }
}
