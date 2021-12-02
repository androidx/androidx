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

package androidx.benchmark.perfetto

import androidx.benchmark.createTempFileFromAsset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import okio.ByteString
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.UiState
import java.io.File
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@SmallTest
public class UiStateTest {
    @Test
    public fun uiStateConstructor() {
        assertEquals(
            UiState(
                timeline_start_ts = 1,
                timeline_end_ts = 100,
                UiState.HighlightProcess(cmdline = "test.package")
            ),
            UiState(
                timelineStart = 1,
                timelineEnd = 100,
                highlightPackage = "test.package"
            )
        )
    }

    @Test
    public fun uiStateCheck() {
        val uiState = UiState(
            timelineStart = 1,
            timelineEnd = 100,
            highlightPackage = "test.package"
        )

        val bytes = UiState.ADAPTER.encode(uiState)
        val uiStateParse = UiState.ADAPTER.decode(bytes)
        assertEquals(uiState, uiStateParse)
    }

    @Test
    public fun append() {
        val initial = Trace(
            packet = listOf(
                TracePacket(
                    compressed_packets = ByteString.of(0, 1, 3)
                )
            )
        )
        val file = File.createTempFile("append", ".trace")
        file.writeBytes(Trace.ADAPTER.encode(initial))
        file.appendUiState(
            UiState(
                timelineStart = 0,
                timelineEnd = 1,
                highlightPackage = "test.package"
            )
        )

        val final = Trace.ADAPTER.decode(file.readBytes())

        val expected = Trace(
            packet = listOf(
                TracePacket(
                    compressed_packets = ByteString.of(0, 1, 3)
                ),
                TracePacket(
                    ui_state = UiState(
                        timeline_start_ts = 0,
                        timeline_end_ts = 1,
                        UiState.HighlightProcess(cmdline = "test.package")
                    )
                )
            )
        )
        assertEquals(expected, final)
    }

    @Test
    public fun actualTraceAppend() {
        val traceFile = createTempFileFromAsset("api31_startup_warm", ".perfetto-trace")
        val initialSize = traceFile.readBytes().size
        traceFile.appendUiState(
            UiState(
                timelineStart = 2,
                timelineEnd = 4,
                highlightPackage = "test.package"
            )
        )

        val finalSize = traceFile.readBytes().size

        // file may shrink slightly due to re-encode, but shouldn't be significant
        assertTrue(finalSize > initialSize * 0.95f)
    }
}
