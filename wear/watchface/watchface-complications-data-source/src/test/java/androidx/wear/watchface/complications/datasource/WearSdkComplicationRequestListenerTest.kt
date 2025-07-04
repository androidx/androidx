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

package androidx.wear.watchface.complications.datasource

import androidx.test.filters.SdkSuppress
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NotConfiguredComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [WearSdkComplicationRequestListenerTest]. */
@RunWith(ComplicationsTestRunner::class)
@SdkSuppress(minSdkVersion = 36, codeName = "Baklava")
class WearSdkComplicationRequestListenerTest {
    @Test
    fun onComplicationData_emptyDefaultData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(EmptyComplicationData())
        }
    }

    @Test
    fun onComplicationData_notConfiguredData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(NotConfiguredComplicationData())
        }
    }

    @Test
    fun onComplicationData_invalidComplicationData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(
                ShortTextComplicationData.Builder(plainText("text"), plainText("description"))
                    .build()
            )
        }
    }

    @Test
    fun onComplicationData_success() {
        assertListenerThrows(RuntimeException::class.java) {
            it.onComplicationData(
                LongTextComplicationData.Builder(plainText("text"), plainText("description"))
                    .build()
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_emptyDefaultData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(EmptyComplicationData(), listOf())
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_notConfiguredData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(NotConfiguredComplicationData(), listOf())
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_invalidComplicationData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    ShortTextComplicationData.Builder(plainText("text"), plainText("description"))
                        .build(),
                    listOf(),
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_notConfiguredComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description"),
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            NotConfiguredComplicationData(),
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_emptyComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description"),
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            EmptyComplicationData(),
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_invalidComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description"),
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            ShortTextComplicationData.Builder(
                                    plainText("text"),
                                    plainText("description"),
                                )
                                .build(),
                        )
                    ),
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_success() {
        assertListenerThrows(RuntimeException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description"),
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            LongTextComplicationData.Builder(
                                    plainText("text"),
                                    plainText("description"),
                                )
                                .build(),
                        )
                    ),
                )
            )
        }
    }

    private fun plainText(text: String) = PlainComplicationText.Builder(text).build()

    private fun <T : Throwable> assertListenerThrows(
        expectedThrowable: Class<T>,
        block: (WearSdkComplicationRequestListener) -> Unit,
    ) {
        Assert.assertThrows(expectedThrowable) {
            runBlocking {
                suspendCancellableCoroutine<Pair<Int, WearSdkComplicationData>> { continuation ->
                    block(
                        WearSdkComplicationRequestListener(
                            1,
                            ComplicationType.LONG_TEXT.toWireComplicationType(),
                            continuation,
                        )
                    )
                }
            }
        }
    }
}
