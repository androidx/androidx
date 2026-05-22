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

package androidx.camera.camera2.pipe.core

import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Timestamps.formatNs
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class TimestampsTest {

    @Test
    fun testDurationFormatting() {
        val duration = 500.milliseconds
        assertThat(duration.formatMs()).isEqualTo("500.000 ms")
        assertThat(duration.formatNs()).isEqualTo("500000000 ns")
    }

    @Test
    fun testTimestampNsOperations() {
        val t1 = TimestampNs(100L)
        val t2 = TimestampNs(200L)

        val diff = t2 - t1
        assertThat(diff).isEqualTo(100.nanoseconds)

        val t3 = t1 + 100.nanoseconds
        assertThat(t3.value).isEqualTo(200L)
    }
}
