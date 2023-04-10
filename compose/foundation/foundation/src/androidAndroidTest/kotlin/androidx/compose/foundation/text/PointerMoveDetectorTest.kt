/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import com.google.common.truth.Correspondence
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TargetTag = "TargetLayout"

@RunWith(JUnit4::class)
class PointerMoveDetectorTest {

    @get:Rule
    val rule = createComposeRule()

    private val actualMoves = mutableListOf<Offset>()

    private val util = layoutWithGestureDetector {
        detectMoves { actualMoves.add(it) }
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        actualMoves.clear()
    }

    private fun layoutWithGestureDetector(
        gestureDetector: suspend PointerInputScope.() -> Unit,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalDensity provides Density(1f),
            LocalViewConfiguration provides TestViewConfiguration(
                minimumTouchTargetSize = DpSize.Zero
            )
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // Some tests execute a lambda before the initial and final passes
                        // so they are called here, higher up the chain, so that the
                        // calls happen prior to the gestureDetector below. The lambdas
                        // do things like consume events on the initial pass or validate
                        // consumption on the final pass.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach {
                                        initialPass(it)
                                    }
                                    awaitPointerEvent(PointerEventPass.Final)
                                    event.changes.forEach {
                                        finalPass(it)
                                    }
                                }
                            }
                        }
                        .wrapContentSize(AbsoluteAlignment.TopLeft)
                        .size(100.toDp())
                        .pointerInput(gestureDetector, gestureDetector)
                        .testTag(TargetTag)
                )
            }
        }
    }

    private fun performTouch(
        initialPass: PointerInputChange.() -> Unit = nothingHandler,
        finalPass: PointerInputChange.() -> Unit = nothingHandler,
        block: TouchInjectionScope.() -> Unit
    ) {
        this.initialPass = initialPass
        this.finalPass = finalPass
        rule.onNodeWithTag(TargetTag).performTouchInput(block)
        rule.waitForIdle()
        this.initialPass = nothingHandler
        this.finalPass = nothingHandler
    }

    @Test
    fun whenSimpleMovement_allMovesAreReported() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))

            moveTo(0, Offset(4f, 4f))
            moveTo(0, Offset(3f, 3f))
            moveTo(0, Offset(2f, 2f))
            moveTo(0, Offset(1f, 1f))

            up(0)
        }

        assertThat(actualMoves).hasEqualOffsets(
            listOf(
                Offset(4f, 4f),
                Offset(3f, 3f),
                Offset(2f, 2f),
                Offset(1f, 1f),
            )
        )
    }

    @Test
    fun whenMultiplePointers_onlyUseFirst() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
            down(1, Offset(6f, 6f))

            moveTo(0, Offset(4f, 4f))
            moveTo(1, Offset(7f, 7f))

            moveTo(0, Offset(3f, 3f))
            moveTo(1, Offset(8f, 8f))

            moveTo(0, Offset(2f, 2f))
            moveTo(1, Offset(9f, 9f))

            moveTo(0, Offset(1f, 1f))
            moveTo(1, Offset(10f, 10f))

            up(0)
            up(1)
        }

        assertThat(actualMoves).hasEqualOffsets(
            listOf(
                Offset(4f, 4f),
                Offset(3f, 3f),
                Offset(2f, 2f),
                Offset(1f, 1f),
            )
        )
    }

    @Test
    fun whenMultiplePointers_thenFirstReleases_handOffToNextPointer() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f)) // ignored because not a move

            moveTo(0, Offset(4f, 4f)) // used
            moveTo(0, Offset(3f, 3f)) // used

            down(1, Offset(4f, 4f)) // ignored because still tracking pointer id 0

            moveTo(0, Offset(2f, 2f)) // used
            moveTo(1, Offset(3f, 3f)) // ignored because still tracking pointer id 0

            up(0) // ignored because not a move

            moveTo(1, Offset(2f, 2f)) // ignored b/c equal to the previous used move
            moveTo(1, Offset(1f, 1f)) // used

            up(1) // ignored because not a move
        }

        assertThat(actualMoves).hasEqualOffsets(
            listOf(
                Offset(4f, 4f),
                Offset(3f, 3f),
                Offset(2f, 2f),
                Offset(1f, 1f),
            )
        )
    }

    private fun IterableSubject.hasEqualOffsets(expectedMoves: List<Offset>) {
        comparingElementsUsing(offsetCorrespondence)
            .containsExactly(*expectedMoves.toTypedArray())
            .inOrder()
    }

    private val offsetCorrespondence: Correspondence<Offset, Offset> = Correspondence.from(
        { o1, o2 -> o1!!.x == o2!!.x && o1.y == o2.y },
        "has the offset of",
    )
}
