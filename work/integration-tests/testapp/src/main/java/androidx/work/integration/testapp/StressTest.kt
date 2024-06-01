/*
 * Copyright 2022 The Android Open Source Project
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
@file:JvmName("StressTest")

package androidx.work.integration.testapp

import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

fun queueLotsOfWorkers(workManager: WorkManager) {
    for (i in 1..1000) {
        Log.i("TestWM", "Queueing $i worker")
        val constraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val uniqueName = "Worker-$i"

        val worker = OneTimeWorkRequestBuilder<TestWorker>().setConstraints(constraint).build()

        val worker2 = OneTimeWorkRequestBuilder<TestWorker>().setConstraints(constraint).build()

        val worker3 = OneTimeWorkRequestBuilder<TestWorker>().setConstraints(constraint).build()

        workManager
            .beginUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, worker)
            .then(worker2)
            .then(worker3)
            .enqueue()
    }
}
