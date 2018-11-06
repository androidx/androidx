/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.integration.testapp

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import androidx.work.coroutines.CoroutineWorker
import kotlinx.coroutines.delay

class CoroutineSleepWorker(context: Context, params: WorkerParameters)
    : CoroutineWorker(context, params) {

    companion object {
        val sleepTimeKey = "sleep_time"
        val tag = "CoroutineWorker"
    }

    override suspend fun doWork(): Payload {
        val sleepTime = inputData.getLong(sleepTimeKey, 0L)
        Log.e(tag, "sleeping for $sleepTime")
        delay(sleepTime)
        Log.e(tag, "finished sleep; stopped=$isStopped")
        return Payload(Result.SUCCESS)
    }
}