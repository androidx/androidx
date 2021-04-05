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

package androidx.work.multiprocess

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableWorkRequest
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
public class ParcelableWorkRequestConvertersTest {
    @Test
    @SmallTest
    public fun converterTest1() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
            .addTag("Test Worker")
            .keepResultsForAtLeast(1, TimeUnit.DAYS)
            .build()
        assertOn(request)
    }

    @Test
    @SmallTest
    public fun converterTest2() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
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
        assertOn(request)
    }

    @Test
    @SmallTest
    public fun converterTest3() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val uri = Uri.parse("test://foo")
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java)
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
                    .setRequiresCharging(true)
                    .setRequiresStorageNotLow(true)
                    .addContentUriTrigger(uri, true)
                    .build()
            )
            .build()
        assertOn(request)
    }

    @Test
    @SmallTest
    public fun converterTest4() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val requests = mutableListOf<WorkRequest>()
        repeat(10) {
            requests += OneTimeWorkRequest.Builder(TestWorker::class.java)
                .addTag("Test Worker")
                .keepResultsForAtLeast(1, TimeUnit.DAYS)
                .build()
        }
        assertOn(requests)
    }

    private fun assertOn(workRequest: WorkRequest) {
        val parcelable = ParcelableWorkRequest(workRequest)
        val parcelled: ParcelableWorkRequest =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableWorkRequest.CREATOR
            )
        assertRequest(workRequest, parcelled.workRequest)
    }

    private fun assertOn(workRequests: List<WorkRequest>) {
        val parcelable = ParcelableWorkRequests(workRequests)
        val parcelled: ParcelableWorkRequests =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelable),
                ParcelableWorkRequests.CREATOR
            )
        assertRequests(workRequests, parcelled.requests)
    }

    private fun assertRequest(first: WorkRequest, second: WorkRequest) {
        assertEquals(first.id, second.id)
        assertEquals(first.workSpec, second.workSpec)
        assertEquals(first.tags, second.tags)
    }

    private fun assertRequests(listOne: List<WorkRequest>, listTwo: List<WorkRequest>) {
        listOne.forEachIndexed { i, workRequest ->
            assertRequest(workRequest, listTwo[i])
        }
    }
}
