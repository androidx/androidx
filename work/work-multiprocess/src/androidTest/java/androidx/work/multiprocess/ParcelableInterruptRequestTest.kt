/*
 * Copyright 2023 The Android Open Source Project
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

import android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CHARGING
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableInterruptRequest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ParcelableInterruptRequestTest {

    @Test
    fun testInterruptRequest() {
        val request = ParcelableInterruptRequest("id1", STOP_REASON_CONSTRAINT_CHARGING)
        val parcelled = ParcelConverters.unmarshall(
            ParcelConverters.marshall(request),
            ParcelableInterruptRequest.CREATOR
        )
        Truth.assertThat(parcelled).isEqualTo(request)
    }
}
