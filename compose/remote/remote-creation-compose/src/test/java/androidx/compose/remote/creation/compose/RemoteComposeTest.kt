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

@file:OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose

import android.content.Context
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.FitBox
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteEnum
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComposeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testCaptureDocument() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                RemoteBox { RemoteText(text = "Hello V2".rs) }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testComplexComposition() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                RemoteColumn {
                    RemoteText(text = "Item 1".rs)
                    RemoteRow { RemoteText(text = "Nested Item".rs) }
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testScopeAndSpacer() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                RemoteRow {
                    RemoteSpacer(modifier = RemoteModifier.weight(1f))
                    RemoteText(text = "End".rs)
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testV1toV2Switching() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                // Using V1 components inside V2 capture
                RemoteBox {
                    RemoteColumn {
                        RemoteText(text = "V1 Text")
                        RemoteRow { RemoteText(text = "Nested V1 Text") }
                    }
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testRemoteCanvasV2() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                RemoteCanvas { drawRect(paint = RemotePaint { color = Color.Red.rc }) }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testFitBoxV2() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                FitBox { RemoteText(text = "Fit Content") }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testCollapsibleLayoutsV2() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                RemoteCollapsibleColumn {
                    RemoteText(text = "Fixed")
                    RemoteCollapsibleRow(modifier = RemoteModifier.weight(1f)) {
                        RemoteText(text = "Weighted Row Content")
                    }
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testStateLayoutV2() = runTest {
        val displayInfo = RemoteCreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocument(creationDisplayInfo = displayInfo, context = context) {
                val checked = rememberMutableRemoteEnum(ToggleState.On)
                RemoteStateLayout(state = checked) { state -> RemoteText(text = "State $state".rs) }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    private enum class ToggleState {
        Off,
        On,
    }

    @Test
    fun captureDocumentFailsIfWritesDuringComposition() = runTest {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
        try {
            captureSingleRemoteDocument(context) {
                val state = LocalRemoteComposeCreationState.current
                state.document.performHaptic(0) // This should fail!
                RemoteBox(modifier = RemoteModifier.fillMaxSize())
            }
            fail("Expected IllegalStateException to be thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message != null)
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        }
    }

    @Test
    fun flowFormFailsIfFlagIsFalse() = runTest {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        try {
            val flow =
                captureRemoteDocument(
                    creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160),
                    writerEvents = WriterEvents(),
                    context = context,
                    content = { RemoteBox(modifier = RemoteModifier.fillMaxSize()) },
                )
            flow.first()
            fail("Expected IllegalStateException to be thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                e.message!!.contains(
                    "captureRemoteDocument requires isEnforceCleanRecompositionEnabled to be true"
                )
            )
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        }
    }
}
