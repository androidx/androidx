/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // https://github.com/JetBrains/compose-jb/issues/1514

package androidx.compose.ui.input.mouse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.TestComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class MouseHoverFilterTest {
    private val window = TestComposeWindow(width = 100, height = 100, density = Density(2f))

    @Test
    fun `inside window`() {
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        window.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }
        window.onMouseEntered(
            x = 0,
            y = 0
        )
        window.onMouseMoved(
            x = 10,
            y = 20
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(1)

        window.onMouseMoved(
            x = 10,
            y = 15
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(2)

        window.onMouseMoved(
            x = 30,
            y = 30
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(2)
    }

    @Test
    fun `window enter`() {
        var moveCount = 0
        var enterCount = 0
        var exitCount = 0

        window.setContent {
            Box(
                modifier = Modifier
                    .pointerMove(
                        onMove = {
                            moveCount++
                        },
                        onEnter = {
                            enterCount++
                        },
                        onExit = {
                            exitCount++
                        }
                    )
                    .size(10.dp, 20.dp)
            )
        }

        window.onMouseEntered(
            x = 10,
            y = 20
        )
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(0)
        assertThat(moveCount).isEqualTo(0)

        window.onMouseExited()
        assertThat(enterCount).isEqualTo(1)
        assertThat(exitCount).isEqualTo(1)
        assertThat(moveCount).isEqualTo(0)
    }

    @Test
    fun `scroll should trigger enter and exit`() {
        val boxCount = 3

        val enterCounts = Array(boxCount) { 0 }
        val exitCounts = Array(boxCount) { 0 }

        window.setContent {
            Column(
                Modifier
                    .size(10.dp, 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(boxCount) { index ->
                    Box(
                        modifier = Modifier
                            .pointerMove(
                                onMove = {},
                                onEnter = {
                                    enterCounts[index] = enterCounts[index] + 1
                                },
                                onExit = {
                                    exitCounts[index] = exitCounts[index] + 1
                                }
                            )
                            .size(10.dp, 20.dp)
                    )
                }
            }
        }

        window.onMouseEntered(0, 0)
        window.onMouseScroll(
            0,
            0,
            MouseScrollEvent(MouseScrollUnit.Page(1f), MouseScrollOrientation.Vertical)
        )
        window.render() // synthetic enter/exit will trigger only on relayout
        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 0))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 0, 0))

        window.onMouseMoved(1, 1)
        window.onMouseScroll(
            2,
            2,
            MouseScrollEvent(MouseScrollUnit.Page(1f), MouseScrollOrientation.Vertical)
        )
        window.render()
        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 1))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 1, 0))

        window.onMouseExited()
        assertThat(enterCounts.toList()).isEqualTo(listOf(1, 1, 1))
        assertThat(exitCounts.toList()).isEqualTo(listOf(1, 1, 1))
    }
}

private fun Modifier.pointerMove(
    onMove: () -> Unit,
    onExit: () -> Unit,
    onEnter: () -> Unit,
): Modifier = pointerInput(onMove, onExit, onEnter) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Move -> onMove()
                PointerEventType.Enter -> onEnter()
                PointerEventType.Exit -> onExit()
            }
        }
    }
}
