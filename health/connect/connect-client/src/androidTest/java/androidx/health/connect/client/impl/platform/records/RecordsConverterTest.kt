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

import android.annotation.TargetApi
import android.healthconnect.datatypes.StepsRecord
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@SmallTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators thats lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU, codeName = "UpsideDownCake")
class RecordsConverterTest {

    @Test
    fun stepsRecord_convert() {
        val steps = androidx.health.connect.client.records.StepsRecord(
            count = 100,
            startTime = Instant.ofEpochMilli(1234L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(5678L),
            endZoneOffset = null
        )
        val platformSteps = steps.toPlatformRecord() as StepsRecord
        assertThat(platformSteps.count).isEqualTo(100)
        assertThat(platformSteps.startTime).isEqualTo(Instant.ofEpochMilli(1234L))
        assertThat(platformSteps.endTime).isEqualTo(Instant.ofEpochMilli(5678L))
    }
}