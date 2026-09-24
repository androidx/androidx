/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTimeDefaultsTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun defaultTimeString_12HourFormat_singleDigitHour_omitsLeadingZeroAndAmPm() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = false, isoInstant = "2025-01-01T13:48:00Z")
            assertThat(result).isEqualTo("1:48")
        }

    @Test
    fun defaultTimeString_12HourFormat_morningSingleDigitHourAndMinute() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = false, isoInstant = "2025-01-01T01:05:00Z")
            assertThat(result).isEqualTo("1:05")
        }

    @Test
    fun defaultTimeString_12HourFormat_midnight_formatsAs12() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = false, isoInstant = "2025-01-01T00:00:00Z")
            assertThat(result).isEqualTo("12:00")
        }

    @Test
    fun defaultTimeString_12HourFormat_noon_formatsAs12() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = false, isoInstant = "2025-01-01T12:30:00Z")
            assertThat(result).isEqualTo("12:30")
        }

    @Test
    fun defaultTimeString_24HourFormat_singleDigitHour_includesLeadingZero() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = true, isoInstant = "2025-01-01T01:48:00Z")
            assertThat(result).isEqualTo("01:48")
        }

    @Test
    fun defaultTimeString_24HourFormat_afternoon() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = true, isoInstant = "2025-01-01T13:48:00Z")
            assertThat(result).isEqualTo("13:48")
        }

    @Test
    fun defaultTimeString_24HourFormat_midnight() =
        runTest(UnconfinedTestDispatcher()) {
            val result =
                evaluateDefaultTimeString(is24Hour = true, isoInstant = "2025-01-01T00:05:00Z")
            assertThat(result).isEqualTo("00:05")
        }

    private suspend fun evaluateDefaultTimeString(
        is24Hour: Boolean,
        isoInstant: String,
    ): String {
        var timeTextId = -1
        val captured =
            captureSingleRemoteDocument(appContext) {
                val timeString: RemoteString =
                    RemoteTimeDefaults.defaultTimeString(is24HourFormat = RemoteBoolean(is24Hour))
                val creationState = LocalRemoteComposeCreationState.current
                RemoteCanvas(RemoteModifier.fillMaxSize()) {
                    timeTextId = timeString.getIdForCreationState(creationState)
                }
            }

        val remoteContext =
            AndroidRemoteContext().apply {
                useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
            }
        val fixedClock = SystemClock(Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC))
        CoreDocument(fixedClock).apply {
            val buffer = RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(captured.bytes))
            initFromBuffer(buffer)
            paint(remoteContext, 0)
        }
        return remoteContext.getText(timeTextId)!!
    }
}
