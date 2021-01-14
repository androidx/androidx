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

package androidx.hilt.integration.workerapp

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject

@HiltWorker
class SimpleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val logger: MyLogger
) : Worker(context, params) {
    override fun doWork(): Result {
        logger.log("Hi")
        return Result.success()
    }
}

@HiltWorker
class SimpleCoroutineWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val logger: MyLogger
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        logger.log("Hi from Coroutines World!")
        return Result.success()
    }
}

object TopClass {
    @HiltWorker
    class NestedWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val logger: MyLogger
    ) : Worker(context, params) {
        override fun doWork() = Result.success()
    }
}

class MyLogger @Inject constructor() {
    fun log(s: String) {
        Log.i("MyLogger", s)
    }
}