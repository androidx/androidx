/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.internal.serializableproxies

import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class AppFunctionZoneIdTest {
    @Test
    fun fromZoneId_convertsCorrectly() {
        val originalZoneId = ZoneId.of("Europe/London")
        val appFunctionZoneId = AppFunctionZoneId.fromZoneId(originalZoneId)

        assertThat(appFunctionZoneId.zoneID).isEqualTo("Europe/London")
    }

    @Test
    fun toZoneId_convertsCorrectly() {
        val originalZoneId = ZoneId.of("America/New_York")
        val appFunctionZoneId = AppFunctionZoneId(originalZoneId.id)
        val convertedZoneId = appFunctionZoneId.toZoneId()

        assertThat(convertedZoneId).isEqualTo(originalZoneId)
    }
}
