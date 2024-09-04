/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.compose.foundation.gestures

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TargetTag = "TargetLayout"

/*
 * Moving Composable UI to a Popup changes the top-level container which previously caused issues
 * when done during an event stream and when the Composable UI contained a lower-level view
 * (see b/327245338).
 *
 * This tests both moving a pure Composable and a Composable containing a View between a Popup
 * during an event stream.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class DragGestureDetectorWhileMovingUIToPopupTest {
    @get:Rule val rule = createComposeRule()

    private val dragAmount = Offset(0f, 50f)

    private var onDragStartCount = 0
    private var onDragEndCount = 0
    private var onDragCancelCount = 0
    private var onDragCount = 0

    private var popUpContainsContent = false

    @Before
    fun setup() {
        onDragStartCount = 0
        onDragEndCount = 0
        onDragCancelCount = 0
        onDragCount = 0

        popUpContainsContent = false
    }

    @Test
    fun dragGesture_dragStartMovesAndroidViewContentToPopup_shouldNotCrash() {
        rule.setContent {
            ContainerMovesContentToPopupOnDrag(
                modifier = Modifier.fillMaxSize(0.9f).background(Color.Green),
                testTag = TargetTag,
                onDragStart = { onDragStartCount++ },
                onDragCancel = { onDragCancelCount++ },
                onDrag = { onDragCount++ },
                onDragEnd = { onDragEndCount++ },
            ) {
                AndroidView(factory = ::View, modifier = Modifier.size(200.dp).aspectRatio(1f)) {
                    it.setBackgroundColor(Color.Red.toArgb())
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, onDragStartCount)
            assertEquals(0, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(0, onDragEndCount)
            assertEquals(false, popUpContainsContent)
        }

        rule.onNodeWithTag(TargetTag).performTouchInput {
            down(Offset.Zero)
            moveBy(dragAmount)
        }

        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(1, onDragStartCount)
            assertEquals(1, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(0, onDragEndCount)
            assertEquals(true, popUpContainsContent)
        }

        rule.onNodeWithTag(TargetTag).performTouchInput { up() }

        rule.runOnIdle {
            assertEquals(1, onDragStartCount)
            assertEquals(1, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(1, onDragEndCount)
            assertEquals(true, popUpContainsContent)
        }
    }

    @Test
    fun dragGesture_dragStartMovesComposeContentToPopup_shouldNotCrash() {
        rule.setContent {
            ContainerMovesContentToPopupOnDrag(
                modifier = Modifier.fillMaxSize(0.9f).background(Color.Green),
                testTag = TargetTag,
                onDragStart = { onDragStartCount++ },
                onDragCancel = { onDragCancelCount++ },
                onDrag = { onDragCount++ },
                onDragEnd = { onDragEndCount++ },
            ) {
                Box(modifier = Modifier.size(200.dp).background(Color.Red)) {}
            }
        }

        rule.runOnIdle {
            assertEquals(0, onDragStartCount)
            assertEquals(0, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(0, onDragEndCount)
            assertEquals(false, popUpContainsContent)
        }

        rule.onNodeWithTag(TargetTag).performTouchInput {
            down(Offset.Zero)
            moveBy(dragAmount)
        }

        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(1, onDragStartCount)
            assertEquals(1, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(0, onDragEndCount)
            assertEquals(true, popUpContainsContent)
        }

        rule.onNodeWithTag(TargetTag).performTouchInput { up() }

        rule.runOnIdle {
            assertEquals(1, onDragStartCount)
            assertEquals(1, onDragCount)
            assertEquals(0, onDragCancelCount)
            assertEquals(1, onDragEndCount)
            assertEquals(true, popUpContainsContent)
        }
    }

    @Composable
    private fun ContainerMovesContentToPopupOnDrag(
        modifier: Modifier = Modifier,
        testTag: String,
        onDragStart: () -> Unit = {},
        onDragCancel: () -> Unit = {},
        onDrag: () -> Unit = {},
        onDragEnd: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        val movableContent = remember { movableContentOf(content) }
        var showPopup by remember { mutableStateOf(false) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier =
                modifier.testTag(testTag).pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            onDragStart()
                            showPopup = true
                            offset = Offset.Zero
                        },
                        onDragCancel = {
                            onDragCancel()
                            showPopup = false
                            offset = Offset.Zero
                        },
                        onDrag = { _, deltaOffset ->
                            onDrag()
                            offset += deltaOffset
                        },
                        onDragEnd = {
                            onDragEnd()
                            showPopup = false
                            offset = Offset.Zero
                        }
                    )
                }
        ) {
            if (showPopup) {
                popUpContainsContent = true
                Popup { Box(Modifier.offset { offset.round() }) { movableContent() } }
            } else {
                movableContent()
            }
        }
    }
}
