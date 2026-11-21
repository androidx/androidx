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

package androidx.compose.remote.core.operations.layout.modifiers

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.remote.player.view.platform.TestViewConfiguration
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

/**
 * Scroll modifier tests, run against both implementations of the modifier:
 * - `unifiedPointerInput = false`: [ScrollModifierOperation], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [ScrollModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = [TestViewConfiguration::class])
class ScrollModifierOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @After
    fun resetPlatformGestureValues() {
        TestViewConfiguration.resetGestureValues()
    }

    @Test
    fun dragBeyondTouchSlop_scrollsContent() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isGreaterThan(0f)
    }

    @Test
    fun dragAfterScrollStarted_scrollPositionFollowsPointer() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove + 30f)
    }

    @Test
    fun dragTowardsEndOfContent_atStartOfContent_doesNotScroll() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 150f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun dragBeyondEndOfContent_stopsAtMaxScrollPosition() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 190f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, -100f)
        dispatchMotionEvent(player, downTime, downTime + 60L, MotionEvent.ACTION_MOVE, 100f, -300f)

        // Assert - 500px of content in a 200px container.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(300f)
    }

    @Test
    fun actionUpAfterFastDrag_keepsScrolling() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)
        val scrollPositionAtActionUp = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 60L, MotionEvent.ACTION_UP, 100f, 100f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(5000))
        drawFrame(player)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isGreaterThan(scrollPositionAtActionUp)
    }

    @Test
    fun dragAcrossScrollAxis_doesNotScroll() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 130f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun horizontalScroll_dragAfterScrollStarted_scrollPositionFollowsPointer() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                row(
                    RecordingModifier().size(200, 200).horizontalScroll(scrollPosition),
                    RowLayout.START,
                    RowLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(100, 200), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 130f, 100f)
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove + 30f)
    }

    @Test
    fun dragStartingOutsideScrollContainer_doesNotScroll() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(100, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 150f, 130f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 150f, 100f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun tapOnClickableChild_runsClickActions() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 100f, 150f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun dragWithinTouchSlopOnClickableChild_runsClickActions() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 145f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 100f, 145f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun dragBeyondTouchSlopOnClickableChild_scrollsAndDoesNotRunClickActions() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)
        val scrollPositionBeforeUp = player.getNamedFloat("scrollPosition")
        dispatchMotionEvent(player, downTime, downTime + 60L, MotionEvent.ACTION_UP, 100f, 100f)

        // Assert.
        assertThat(scrollPositionBeforeUp).isGreaterThan(0f)
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun tapAfterScroll_runsClickActionsOfChildScrolledUnderPointer() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        // Scroll by about one and a half children, and hold before lifting so there is no fling.
        val scrollTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, scrollTime, scrollTime, MotionEvent.ACTION_DOWN, 100f, 190f)
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 20L,
            MotionEvent.ACTION_MOVE,
            100f,
            170f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 40L,
            MotionEvent.ACTION_MOVE,
            100f,
            40f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 1000L,
            MotionEvent.ACTION_MOVE,
            100f,
            40f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 1020L,
            MotionEvent.ACTION_UP,
            100f,
            40f,
        )
        drawFrame(player)
        val downTime = scrollTime + 2000L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 10f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 100f, 10f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun scrollWithoutScrollPosition_tapAfterScroll_runsClickActionsOfChildScrolledUnderPointer() {
        // Arrange.
        val player = createPlayer {
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        // Scroll by about one and a half children, and hold before lifting so there is no fling.
        val scrollTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, scrollTime, scrollTime, MotionEvent.ACTION_DOWN, 100f, 190f)
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 20L,
            MotionEvent.ACTION_MOVE,
            100f,
            170f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 40L,
            MotionEvent.ACTION_MOVE,
            100f,
            40f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 1000L,
            MotionEvent.ACTION_MOVE,
            100f,
            40f,
        )
        dispatchMotionEvent(
            player,
            scrollTime,
            scrollTime + 1020L,
            MotionEvent.ACTION_UP,
            100f,
            40f,
        )
        drawFrame(player)
        val downTime = scrollTime + 2000L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 10f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 100f, 10f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun nestedScrollOnOtherAxis_dragAlongInnerAxis_scrollsOnlyInnerContainer() {
        // Arrange.
        val player = createPlayer {
            val outerScrollPosition = addNamedFloat("outerScrollPosition", 0f)
            val innerScrollPosition = addNamedFloat("innerScrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(outerScrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    row(
                        RecordingModifier().size(200, 100).horizontalScroll(innerScrollPosition),
                        RowLayout.START,
                        RowLayout.TOP,
                    ) {
                        repeat(5) {
                            box(
                                RecordingModifier().size(100, 100),
                                BoxLayout.START,
                                BoxLayout.TOP,
                            ) {}
                        }
                    }
                    repeat(4) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 130f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 50f)

        // Assert.
        assertThat(player.getNamedFloat("innerScrollPosition")).isGreaterThan(0f)
        assertThat(player.getNamedFloat("outerScrollPosition")).isEqualTo(0f)
    }

    @Test
    fun nestedScrollOnOtherAxis_dragAlongOuterAxis_scrollsOnlyOuterContainer() {
        // Arrange.
        val player = createPlayer {
            val outerScrollPosition = addNamedFloat("outerScrollPosition", 0f)
            val innerScrollPosition = addNamedFloat("innerScrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(outerScrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    row(
                        RecordingModifier().size(200, 100).horizontalScroll(innerScrollPosition),
                        RowLayout.START,
                        RowLayout.TOP,
                    ) {
                        repeat(5) {
                            box(
                                RecordingModifier().size(100, 100),
                                BoxLayout.START,
                                BoxLayout.TOP,
                            ) {}
                        }
                    }
                    repeat(4) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 90f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 70f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 40f)

        // Assert.
        assertThat(player.getNamedFloat("outerScrollPosition")).isGreaterThan(0f)
        assertThat(player.getNamedFloat("innerScrollPosition")).isEqualTo(0f)
    }

    @Test
    fun secondPointerDown_firstPointerKeepsScrolling() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 40L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 130f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 60L,
            MotionEvent.ACTION_MOVE,
            actionIndex = 0,
            Pointer(id = 0, x = 100f, y = 100f),
            Pointer(id = 1, x = 50f, y = 150f),
        )

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove + 30f)
    }

    @Test
    fun secondPointerMove_doesNotScroll() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 40L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 130f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 60L,
            MotionEvent.ACTION_MOVE,
            actionIndex = 0,
            Pointer(id = 0, x = 100f, y = 130f),
            Pointer(id = 1, x = 50f, y = 100f),
        )

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove)
    }

    @Test
    fun tapWithinTouchSlopAfterTwoPointerGesture_secondPointerLiftedFirst_runsClickActions() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        // A two pointer gesture in which the second pointer is lifted first.
        val gestureTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, gestureTime, gestureTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 150f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 40L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 150f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 60L,
            MotionEvent.ACTION_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 100f, y = 150f),
        )
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = gestureTime + 2000L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 47f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 100f, 47f)

        // Assert.
        assertThat(triggeredActions).containsExactly(0 to "")
    }

    @Test
    fun tapWithinTouchSlopAfterTwoPointerGesture_firstPointerLiftedFirst_runsClickActions() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) { index ->
                        box(
                            RecordingModifier().size(200, 100).onClick(HostAction(index)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        }
        // A two pointer gesture in which the first pointer is lifted first.
        val gestureTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, gestureTime, gestureTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 150f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 40L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 100f, y = 150f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 60L,
            MotionEvent.ACTION_UP,
            actionIndex = 0,
            Pointer(id = 1, x = 50f, y = 150f),
        )
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = gestureTime + 2000L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 47f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 100f, 47f)

        // Assert.
        assertThat(triggeredActions).containsExactly(0 to "")
    }

    @Test
    fun scrollByOffset_changesScrollPositionAndKeepsItAcrossFrames() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }

        // Act. The offset is negative when scrolling towards the end of the content.
        player.scrollByOffset(scrollContainer(player), -100)

        // Assert.
        repeat(3) {
            drawFrame(player)
            assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(100f)
        }
    }

    @Test
    fun dragAfterScrollByOffset_scrollPositionFollowsPointer() {
        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        player.scrollByOffset(scrollContainer(player), -100)
        drawFrame(player)
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(scrollPositionBeforeMove).isAtLeast(100f)
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove + 30f)
    }

    @Test
    fun scrollPositionSetByAction_keepsScrollPositionAcrossFrames() {
        // Arrange.
        val player =
            createPlayer(height = 300) {
                val scrollPosition = addNamedFloat("scrollPosition", 0f)
                root {
                    column(
                        RecordingModifier().size(200, 300),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        column(
                            RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                        ) {
                            repeat(5) {
                                box(
                                    RecordingModifier().size(200, 100),
                                    BoxLayout.START,
                                    BoxLayout.TOP,
                                ) {}
                            }
                        }
                        box(
                            RecordingModifier()
                                .size(200, 100)
                                .onClick(ValueFloatChange(Utils.idFromNan(scrollPosition), 200f)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 250f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 100f, 250f)

        // Assert.
        repeat(3) {
            drawFrame(player)
            assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(200f)
        }
    }

    @Test
    fun dragAfterScrollPositionSetByAction_scrollPositionFollowsPointer() {
        // Arrange.
        val player =
            createPlayer(height = 300) {
                val scrollPosition = addNamedFloat("scrollPosition", 0f)
                root {
                    column(
                        RecordingModifier().size(200, 300),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        column(
                            RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                        ) {
                            repeat(5) {
                                box(
                                    RecordingModifier().size(200, 100),
                                    BoxLayout.START,
                                    BoxLayout.TOP,
                                ) {}
                            }
                        }
                        box(
                            RecordingModifier()
                                .size(200, 100)
                                .onClick(ValueFloatChange(Utils.idFromNan(scrollPosition), 200f)),
                            BoxLayout.START,
                            BoxLayout.TOP,
                        ) {}
                    }
                }
            }
        val clickTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, clickTime, clickTime, MotionEvent.ACTION_DOWN, 100f, 250f)
        dispatchMotionEvent(player, clickTime, clickTime + 20L, MotionEvent.ACTION_UP, 100f, 250f)
        drawFrame(player)
        val downTime = clickTime + 500L
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 100f)

        // Assert.
        assertThat(scrollPositionBeforeMove).isAtLeast(200f)
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove + 30f)
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only. Skipped on the legacy path.
    // ------------------------------------------------------------------------------------------

    @Test
    fun dragWithinTouchSlop_doesNotScroll() {
        // The legacy touch path has no touch slop: it scrolls from the first move.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 145f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun nestedScrollOnSameAxis_dragScrollsOnlyInnerContainer() {
        // On the legacy touch path every scroll container under the pointer scrolls.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            val outerScrollPosition = addNamedFloat("outerScrollPosition", 0f)
            val innerScrollPosition = addNamedFloat("innerScrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(outerScrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    column(
                        RecordingModifier().size(200, 100).verticalScroll(innerScrollPosition),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        repeat(5) {
                            box(
                                RecordingModifier().size(200, 100),
                                BoxLayout.START,
                                BoxLayout.TOP,
                            ) {}
                        }
                    }
                    repeat(4) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 90f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 70f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 40f)

        // Assert.
        assertThat(player.getNamedFloat("innerScrollPosition")).isGreaterThan(0f)
        assertThat(player.getNamedFloat("outerScrollPosition")).isEqualTo(0f)
    }

    @Test
    fun firstPointerUpWhileSecondPointerIsDown_secondPointerDoesNotScroll() {
        // The legacy touch path follows whichever pointer is still down.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 130f)
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 40L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 100f, y = 130f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 1000L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 100f, y = 130f),
            Pointer(id = 1, x = 50f, y = 150f),
        )
        val scrollPositionBeforeMove = player.getNamedFloat("scrollPosition")

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 1020L,
            MotionEvent.ACTION_MOVE,
            actionIndex = 0,
            Pointer(id = 1, x = 50f, y = 100f),
        )

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(scrollPositionBeforeMove)
    }

    @Test
    fun dragWithinPlatformTouchSlop_doesNotScroll() {
        // The legacy touch path has no touch slop: it scrolls from the first move.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        TestViewConfiguration.setScaledTouchSlop(50)
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 120f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 110f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    @Test
    fun dragBeyondPlatformTouchSlop_scrollsContent() {
        // The legacy touch path has no touch slop: it scrolls from the first move.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        TestViewConfiguration.setScaledTouchSlop(2)
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    repeat(5) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 145f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 140f)

        // Assert.
        assertThat(player.getNamedFloat("scrollPosition")).isGreaterThan(0f)
    }

    private val robolectricClock: RemoteClock =
        object :
            RemoteClock by androidx.compose.remote.core.SystemClock(
                object : Clock() {
                    override fun getZone(): ZoneId = ZoneOffset.UTC

                    override fun withZone(zone: ZoneId): Clock = this

                    override fun instant(): Instant =
                        Instant.ofEpochMilli(SystemClock.uptimeMillis())
                }
            ) {
            // Animations are timed with nanoTime.
            override fun nanoTime(): Long = SystemClock.elapsedRealtimeNanos()
        }

    private fun createPlayer(
        width: Int = 200,
        height: Int = 200,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): RemoteComposePlayer {
        val writer =
            RemoteComposeWriter(
                RcPlatformProfiles.ANDROIDX,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(
                    Header.FEATURE_UNIFIED_POINTER_INPUT,
                    if (unifiedPointerInput) 1 else 0,
                ),
            )
        writer.buildDocument()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        val player = RemoteComposePlayer(activity)
        // The document clock follows Robolectric's clock, so that the scroll animation after the
        // pointer is lifted advances with ShadowSystemClock.
        player.setDocument(
            RemoteDocument(ByteArrayInputStream(writer.encodeToByteArray()), robolectricClock)
        )
        parent.addView(player, FrameLayout.LayoutParams(width, height))
        activity.setContentView(parent)

        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)
        drawFrame(parent)
        return player
    }

    /** Returns the scroll container, which must be the only child of the root of the document. */
    private fun scrollContainer(player: RemoteComposePlayer): LayoutComponent =
        player.document.document.rootLayoutComponent!!
            .list
            .filterIsInstance<LayoutComponent>()
            .single()

    private fun drawFrame(view: View) {
        view.draw(Canvas(Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)))
    }

    private fun dispatchMotionEvent(
        view: View,
        downTime: Long,
        eventTime: Long,
        action: Int,
        x: Float,
        y: Float,
    ) {
        val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    /** A pointer of a motion event with several pointers. */
    private class Pointer(val id: Int, val x: Float, val y: Float)

    /**
     * Dispatches a motion event that carries all the [pointers] that are currently down.
     *
     * @param actionIndex index in [pointers] of the pointer that went down or up, for
     *   [MotionEvent.ACTION_POINTER_DOWN] and [MotionEvent.ACTION_POINTER_UP]
     */
    private fun dispatchMotionEvent(
        view: View,
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        vararg pointers: Pointer,
    ) {
        val properties =
            Array(pointers.size) { index ->
                MotionEvent.PointerProperties().apply {
                    id = pointers[index].id
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            }
        val coords =
            Array(pointers.size) { index ->
                MotionEvent.PointerCoords().apply {
                    x = pointers[index].x
                    y = pointers[index].y
                    pressure = 1f
                    size = 1f
                }
            }
        val event =
            MotionEvent.obtain(
                downTime,
                eventTime,
                action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                pointers.size,
                properties,
                coords,
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 1f,
                /* yPrecision = */ 1f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                InputDevice.SOURCE_TOUCHSCREEN,
                /* flags = */ 0,
            )
        view.dispatchTouchEvent(event)
        event.recycle()
    }
}
