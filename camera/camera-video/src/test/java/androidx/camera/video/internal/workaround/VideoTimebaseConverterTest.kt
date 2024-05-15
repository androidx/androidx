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

package androidx.camera.video.internal.workaround

import android.os.Build
import androidx.camera.core.impl.Timebase
import androidx.camera.video.internal.compat.quirk.CameraUseInconsistentTimebaseQuirk
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoTimebaseConverterTest {

    private val systemTimeProvider =
        FakeTimeProvider(TimeUnit.MICROSECONDS.toNanos(1000L), TimeUnit.MICROSECONDS.toNanos(2000L))

    @Test
    fun uptimeTimebase_noConversion() {
        // Arrange.
        val videoTimebaseConverter =
            VideoTimebaseConverter(systemTimeProvider, Timebase.UPTIME, null)

        // Act.
        val outputTime1 = videoTimebaseConverter.convertToUptimeUs(800L)
        val outputTime2 = videoTimebaseConverter.convertToUptimeUs(900L)

        // Assert.
        assertThat(outputTime1).isEqualTo(800L)
        assertThat(outputTime2).isEqualTo(900L)
    }

    @Test
    fun realtimeTimebase_doConversion() {
        // Arrange.
        val videoTimebaseConverter =
            VideoTimebaseConverter(systemTimeProvider, Timebase.REALTIME, null)

        // Act.
        val outputTime1 = videoTimebaseConverter.convertToUptimeUs(1800L)
        val outputTime2 = videoTimebaseConverter.convertToUptimeUs(1900L)

        // Assert.
        assertThat(outputTime1).isEqualTo(800L)
        assertThat(outputTime2).isEqualTo(900L)
    }

    @Test
    fun hasQuirk_closeToUptime_noConversion() {
        // Arrange.
        val videoTimebaseConverter = VideoTimebaseConverter(
            systemTimeProvider,
            Timebase.REALTIME,
            CameraUseInconsistentTimebaseQuirk()
        )

        // Act.
        val outputTime1 = videoTimebaseConverter.convertToUptimeUs(800L)
        val outputTime2 = videoTimebaseConverter.convertToUptimeUs(900L)

        // Assert.
        assertThat(outputTime1).isEqualTo(800L)
        assertThat(outputTime2).isEqualTo(900L)
    }

    @Test
    fun hasQuirk_closeToRealtime_doConversion() {
        // Arrange.
        val videoTimebaseConverter = VideoTimebaseConverter(
            systemTimeProvider,
            Timebase.UPTIME,
            CameraUseInconsistentTimebaseQuirk()
        )

        // Act.
        val outputTime1 = videoTimebaseConverter.convertToUptimeUs(1800L)
        val outputTime2 = videoTimebaseConverter.convertToUptimeUs(1900L)

        // Assert.
        assertThat(outputTime1).isEqualTo(800L)
        assertThat(outputTime2).isEqualTo(900L)
    }

    @Test
    fun systemTimeDiverged_closeToUptime_noConversion() {
        // Arrange.
        val systemTimeProvider = FakeTimeProvider(
            TimeUnit.SECONDS.toNanos(3),
            TimeUnit.SECONDS.toNanos(8)
        )
        val videoTimebaseConverter =
            VideoTimebaseConverter(systemTimeProvider, Timebase.REALTIME, null)

        // Act.
        val outputTimeUs1 = videoTimebaseConverter.convertToUptimeUs(TimeUnit.SECONDS.toMicros(2))
        val outputTimeUs2 = videoTimebaseConverter.convertToUptimeUs(TimeUnit.SECONDS.toMicros(3))

        // Assert.
        assertThat(outputTimeUs1).isEqualTo(TimeUnit.SECONDS.toMicros(2))
        assertThat(outputTimeUs2).isEqualTo(TimeUnit.SECONDS.toMicros(3))
    }

    @Test
    fun systemTimeDiverged_closeToRealtime_doConversion() {
        // Arrange.
        val systemTimeProvider = FakeTimeProvider(
            TimeUnit.SECONDS.toNanos(3),
            TimeUnit.SECONDS.toNanos(8)
        )
        val videoTimebaseConverter =
            VideoTimebaseConverter(systemTimeProvider, Timebase.UPTIME, null)

        // Act.
        val outputTimeUs1 = videoTimebaseConverter.convertToUptimeUs(TimeUnit.SECONDS.toMicros(7))
        val outputTimeUs2 = videoTimebaseConverter.convertToUptimeUs(TimeUnit.SECONDS.toMicros(8))

        // Assert.
        assertThat(outputTimeUs1).isEqualTo(TimeUnit.SECONDS.toMicros(2))
        assertThat(outputTimeUs2).isEqualTo(TimeUnit.SECONDS.toMicros(3))
    }
}
