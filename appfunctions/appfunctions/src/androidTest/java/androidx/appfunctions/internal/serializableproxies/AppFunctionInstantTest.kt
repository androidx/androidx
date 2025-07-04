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
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O) // Instant requires API 26
@RunWith(JUnit4::class)
class AppFunctionInstantTest {

    @Test
    fun fromInstant_convertsCorrectly() {
        val originalInstant = Instant.ofEpochSecond(1000, 2000)

        val appFunctionInstant = AppFunctionInstant.fromInstant(originalInstant)

        assertThat(appFunctionInstant.epochSecond).isEqualTo(1000)
        assertThat(appFunctionInstant.nanoAdjustment).isEqualTo(2000)
    }

    @Test
    fun toInstant_convertsCorrectly() {
        val appFunctionInstant = AppFunctionInstant(epochSecond = 1000, nanoAdjustment = 2000)

        val convertedInstant = appFunctionInstant.toInstant()

        assertThat(convertedInstant).isEqualTo(Instant.ofEpochSecond(1000, 2000))
    }
}
