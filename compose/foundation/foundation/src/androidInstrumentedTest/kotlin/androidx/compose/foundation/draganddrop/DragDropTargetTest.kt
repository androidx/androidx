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

package androidx.compose.foundation.draganddrop

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.content.testDragAndDrop
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class DragDropTargetTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun dragAndDropTarget_changingTarget_updatesModifier() {
        var targetKey by mutableIntStateOf(0)
        val dropEvents = mutableListOf<Int>()
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.size(60.dp)
                    .background(Color.Blue)
                    .testTag("target")
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { true },
                        target =
                            remember(targetKey) {
                                DragAndDropTarget {
                                    dropEvents.add(targetKey)
                                    return@DragAndDropTarget true
                                }
                            }
                    )
            )
        }

        assertThat(dropEvents).isEmpty()
        val targetPosition = rule.onNodeWithTag("target").fetchSemanticsNode().boundsInRoot.center

        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            drop()
            cancelDrag()
        }
        rule.waitForIdle()

        assertThat(dropEvents[0]).isEqualTo(0)

        targetKey++
        rule.waitForIdle()

        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            drop()
            cancelDrag()
        }

        assertThat(dropEvents[1]).isEqualTo(1)

        targetKey--
        rule.waitForIdle()

        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            drop()
            cancelDrag()
        }

        assertThat(dropEvents[2]).isEqualTo(0)
    }

    // todo(jossiwolf) b/330481832 Investigate behavior and update
    @Test
    fun dragAndDropTarget_changingTarget_whileDragging_doesntReceiveEvent() {
        var targetKey by mutableIntStateOf(0)
        val dropEvents = mutableListOf<Int>()
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.size(60.dp)
                    .background(Color.Blue)
                    .testTag("target")
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { true },
                        target =
                            remember(targetKey) {
                                DragAndDropTarget {
                                    dropEvents.add(targetKey)
                                    return@DragAndDropTarget true
                                }
                            }
                    )
            )
        }

        assertThat(dropEvents).isEmpty()
        val targetPosition = rule.onNodeWithTag("target").fetchSemanticsNode().boundsInRoot.center

        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            targetKey++
            rule.waitForIdle()
            drop()
            cancelDrag()
        }
        rule.waitForIdle()
        assertThat(dropEvents).isEmpty()
    }

    @Test
    fun dragAndDropTarget_shouldStartDragAndDrop_referencesLatestInstance() {
        var shouldStartDragAndDropKey by mutableIntStateOf(0)
        val shouldStartDragAndDropInvocations = mutableListOf<Int>()
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box(
                Modifier.size(60.dp)
                    .background(Color.Blue)
                    .testTag("target")
                    .dragAndDropTarget(
                        shouldStartDragAndDrop =
                            remember(shouldStartDragAndDropKey) {
                                {
                                    shouldStartDragAndDropInvocations.add(shouldStartDragAndDropKey)
                                    true
                                }
                            },
                        target =
                            remember {
                                DragAndDropTarget {
                                    return@DragAndDropTarget true
                                }
                            }
                    )
            )
        }

        val targetPosition = rule.onNodeWithTag("target").fetchSemanticsNode().boundsInRoot.center

        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            drop()
            cancelDrag()
        }
        rule.waitForIdle()

        assertThat(shouldStartDragAndDropInvocations[0]).isEqualTo(0)

        shouldStartDragAndDropKey++
        rule.waitForIdle()
        testDragAndDrop(view, rule.density) {
            drag(targetPosition, "FAKE EVENT")
            drop()
            cancelDrag()
        }
        rule.waitForIdle()

        assertThat(shouldStartDragAndDropInvocations[1]).isEqualTo(1)
    }

    private fun DragAndDropTarget(
        onEvent: (Boolean) -> Unit = {},
        onDrop: (DragAndDropEvent) -> Boolean
    ) =
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = onEvent(true)

            override fun onExited(event: DragAndDropEvent) = onEvent(false)

            override fun onEnded(event: DragAndDropEvent) = onEvent(false)

            override fun onDrop(event: DragAndDropEvent) = onDrop(event)
        }
}
