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

package androidx.health.connect.client.impl.platform.records

import android.healthconnect.datatypes.DataOrigin as PlatformDataOrigin
import android.healthconnect.datatypes.StepsRecord as PlatformStepsRecord
import android.annotation.TargetApi
import android.os.Build
import androidx.health.connect.client.impl.platform.time.SystemDefaultTimeSource
import androidx.health.connect.client.impl.platform.time.FakeTimeSource
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import android.healthconnect.datatypes.HeartRateRecord as PlatformHeartRateRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class RequestConvertersTest {

    @Test
    fun readRecordsRequest_fromSdkToPlatform() {
        val sdkRequest = ReadRecordsRequest(
            StepsRecord::class,
            TimeRangeFilter.between(Instant.ofEpochMilli(123L), Instant.ofEpochMilli(456L)),
            setOf(DataOrigin("package1"), DataOrigin("package2"))
        )

        with(sdkRequest.toPlatformReadRecordsRequestUsingFilters(SystemDefaultTimeSource)) {
            assertThat(recordType).isAssignableTo(PlatformStepsRecord::class.java)
            assertThat(dataOrigins).containsExactly(
                PlatformDataOrigin.Builder().setPackageName("package1").build(),
                PlatformDataOrigin.Builder().setPackageName("package2").build()
            )
        }
    }

    @Test
    fun changesTokenRequest_fromSdkToPlatform() {
        val sdkRequest = ChangesTokenRequest(
            setOf(StepsRecord::class, HeartRateRecord::class),
            setOf(DataOrigin("package1"), DataOrigin("package2"))
        )

        with(sdkRequest.toPlatformChangeLogTokenRequest()) {
            assertThat(recordTypes).containsExactly(
                PlatformStepsRecord::class.java,
                PlatformHeartRateRecord::class.java
            )
            assertThat(dataOriginFilters).containsExactly(
                PlatformDataOrigin.Builder().setPackageName("package1").build(),
                PlatformDataOrigin.Builder().setPackageName("package2").build()
            )
        }
    }

    @Test
    fun timeRangeFilter_fromSdkToPlatform() {
        val sdkFilter =
            TimeRangeFilter.between(Instant.ofEpochMilli(123L), Instant.ofEpochMilli(456L))

        with(sdkFilter.toPlatformTimeRangeFilter(SystemDefaultTimeSource)) {
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(123L))
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(456L))
        }
    }

    @Test
    fun timeRangeFilter_fromSdkToPlatform_none() {
        val timeSource = FakeTimeSource()
        timeSource.now = Instant.ofEpochMilli(123L)

        val sdkFilter = TimeRangeFilter.none()

        with(sdkFilter.toPlatformTimeRangeFilter(timeSource)) {
            assertThat(startTime).isEqualTo(Instant.EPOCH)
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(123L))
        }
    }
}