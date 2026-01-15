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

package androidx.compose.remote.creation.compose.v2

import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComposeV2Test {

    @Test
    fun testCaptureDocument() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val clock = BroadcastFrameClock()
        val flow =
            captureRemoteDocumentV2(displayInfo, context = coroutineContext + clock) {
                RemoteBoxV2 { RemoteTextV2(text = "Hello V2") }
            }

        launch {
            yield()
            clock.sendFrame(0L)
            yield()
            clock.sendFrame(16_000_000L)
        } // Trigger recomposition

        val document = flow.first()
        assertNotNull(document)
        assertTrue(document.isNotEmpty())
    }

    @Test
    fun testComplexComposition() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val clock = BroadcastFrameClock()
        val flow =
            captureRemoteDocumentV2(displayInfo, context = coroutineContext + clock) {
                RemoteColumnV2 {
                    RemoteTextV2(text = "Item 1")
                    RemoteRowV2 { RemoteTextV2(text = "Nested Item") }
                }
            }

        launch {
            yield()
            clock.sendFrame(0L)
            yield()
            clock.sendFrame(16_000_000L)
        } // Trigger recomposition

        val document = flow.first()
        assertNotNull(document)
        assertTrue(document.isNotEmpty())
    }

    @Test
    fun testScopeAndSpacer() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val clock = BroadcastFrameClock()
        val flow =
            captureRemoteDocumentV2(displayInfo, context = coroutineContext + clock) {
                RemoteRowV2 {
                    RemoteSpacerV2(modifier = RemoteModifier.weight(1f))
                    RemoteTextV2(text = "End")
                }
            }

        launch {
            yield()
            clock.sendFrame(0L)
            yield()
            clock.sendFrame(16_000_000L)
        }

        val document = flow.first()
        assertNotNull(document)
        assertTrue(document.isNotEmpty())
    }

    @Test
    fun testV1toV2Switching() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val clock = BroadcastFrameClock()
        val flow =
            captureRemoteDocumentV2(displayInfo, context = coroutineContext + clock) {
                // Using V1 components inside V2 capture
                RemoteBox {
                    RemoteColumn {
                        RemoteText(text = "V1 Text")
                        RemoteRow { RemoteText(text = "Nested V1 Text") }
                    }
                }
            }

        launch {
            yield()
            clock.sendFrame(0L)
            yield()
            clock.sendFrame(16_000_000L)
        }

        val document = flow.first()
        assertNotNull(document)
        assertTrue(document.isNotEmpty())
    }

    @Test
    fun testRemoteCanvasV2() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val clock = BroadcastFrameClock()
        val flow =
            captureRemoteDocumentV2(displayInfo, context = coroutineContext + clock) {
                RemoteCanvas {
                    drawRect(
                        paint =
                            RemotePaint().apply { remoteColor = RemoteColor(Color.Red.toArgb()) }
                    )
                }
            }

        launch {
            yield()
            clock.sendFrame(0L)
            yield()
            clock.sendFrame(16_000_000L)
        }

        val document = flow.first()
        assertNotNull(document)
        assertTrue(document.isNotEmpty())
    }
}
