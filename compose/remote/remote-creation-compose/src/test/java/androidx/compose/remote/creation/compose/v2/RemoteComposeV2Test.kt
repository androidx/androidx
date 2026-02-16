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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.v2

import android.content.Context
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.FitBox
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.layout.StateLayout
import androidx.compose.remote.creation.compose.layout.rememberStateMachine
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rememberRemoteIntValue
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComposeV2Test {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        RemoteComposeCreationComposeFlags.isRemoteApplierEnabled = true
    }

    @Test
    fun testCaptureDocument() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                RemoteBoxV2 { RemoteTextV2(text = "Hello V2".rs) }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testComplexComposition() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                RemoteColumnV2 {
                    RemoteTextV2(text = "Item 1".rs)
                    RemoteRowV2 { RemoteTextV2(text = "Nested Item".rs) }
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testScopeAndSpacer() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                RemoteRowV2 {
                    RemoteSpacerV2(modifier = RemoteModifier.weight(1f))
                    RemoteTextV2(text = "End".rs)
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testV1toV2Switching() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
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
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                RemoteCanvas {
                    drawRect(
                        paint =
                            RemotePaint().apply { remoteColor = RemoteColor(Color.Red.toArgb()) }
                    )
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testFitBoxV2() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                FitBox { RemoteText(text = "Fit Content") }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }

    @Test
    fun testCollapsibleLayoutsV2() = runTest {
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
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
        val displayInfo = CreationDisplayInfo(500, 500, 1)
        val document =
            captureSingleRemoteDocumentV2(creationDisplayInfo = displayInfo, context = context) {
                val checked = rememberRemoteIntValue { 1 }
                val off = 0
                val on = 1
                val stateMachine = rememberStateMachine(checked, off, on)
                StateLayout(stateMachine = stateMachine) { state ->
                    RemoteText(text = "State $state")
                }
            }

        assertNotNull(document)
        assertTrue(document.bytes.isNotEmpty())
    }
}
