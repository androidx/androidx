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

package androidx.work.benchmark

import android.net.Uri
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableConstraints
import androidx.work.multiprocess.parcelable.ParcelableWorkRequest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class MarshallingBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    lateinit var constraints: Constraints
    lateinit var parcelledConstraints: ParcelableConstraints

    lateinit var request: WorkRequest
    lateinit var parcelledWorkRequest: ParcelableWorkRequest

    @Before
    fun setUp() {
        val uri = Uri.parse("test://foo")
        // TODO(b/191892569): Figure out why this is only needed on Playground builds.
        //  .addContentUriTrigger requires minSdk 24, but for some reason this @SdkSuppress is only
        //  on Playground builds to get lintDebug to pass.
        @SdkSuppress(minSdkVersion = 24)
        constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(true)
            .setRequiresStorageNotLow(true)
            .addContentUriTrigger(uri, true)
            .build()

        request = OneTimeWorkRequest.Builder(NoOpWorker::class.java)
            .setInitialDelay(1, TimeUnit.HOURS)
            .setInputData(
                Data.Builder()
                    .put("test", 1L)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        parcelledConstraints = ParcelableConstraints(constraints)
        parcelledWorkRequest = ParcelableWorkRequest(request)
    }

    @Test
    fun parcelledConstraintBenchmark() {
        benchmarkRule.measureRepeated {
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelledConstraints),
                ParcelableConstraints.CREATOR
            )
        }
    }

    @Test
    fun parcelledWorkRequestBenchmark() {
        benchmarkRule.measureRepeated {
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelledWorkRequest),
                ParcelableWorkRequest.CREATOR
            )
        }
    }
}
