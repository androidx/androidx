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

package androidx.work

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.worker.TestWorker
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WorkRequestTest {
    @Test
    @SmallTest
    public fun expeditedRequest_withInitialDelay_throwsException() {
        var error: Throwable? = null
        try {
            OneTimeWorkRequest.Builder(TestWorker::class.java)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        } catch (exception: Throwable) {
            error = exception
        }
        // Delayed work cannot be expedited
        assertNotNull(error)
    }

    @Test
    @SmallTest
    public fun periodicWorkRequest_expedited_throwsException() {
        var error: Throwable? = null
        try {
            PeriodicWorkRequest.Builder(TestWorker::class.java, 15, TimeUnit.MINUTES)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        } catch (exception: Throwable) {
            error = exception
        }
        // Periodic work cannot be expedited
        assertNotNull(error)
    }
}
