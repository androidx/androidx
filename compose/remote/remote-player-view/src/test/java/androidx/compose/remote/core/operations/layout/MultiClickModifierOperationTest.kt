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

package androidx.compose.remote.core.operations.layout

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.input.MetadataUtil
import androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
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
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/**
 * Multi click modifier tests, run against both implementations of the multi click modifier:
 * - `unifiedPointerInput = false`: [MultiClickModifier], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [MultiClickModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, shadows = [TestViewConfiguration::class])
class MultiClickModifierOperationTest(private val unifiedPointerInput: Boolean) {

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
    fun singleClickModifier_tapInsideBox_runsActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        player.document.document.setHapticEngine { type -> triggeredHaptics.add(type) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun longClickModifier_longPressInsideBox_runsActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        player.document.document.setHapticEngine { type -> triggeredHaptics.add(type) }

        // Act.
        dispatchLongPress(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
        assertThat(triggeredHaptics).containsExactly(0)
    }

    @Test
    fun longClickModifier_singleTapAndDoubleTapInsideBox_doNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        dispatchDoubleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun doubleClickModifier_doubleTapInsideBox_runsActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        player.document.document.setHapticEngine { type -> triggeredHaptics.add(type) }

        // Act.
        dispatchDoubleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(3 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun doubleClickModifier_singleTapAndLongPressInsideBox_doNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        dispatchLongPress(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun tapOutsideBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 150f, y = 150f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun tapInsideInvisibleBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            val visibilityId = addInteger(Component.Visibility.INVISIBLE).toInt()
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .visibility(visibilityId)
                        .onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun singleClickModifier_cancelledTap_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchCancelledTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun singleClickModifier_dragReleasingOutsideBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 175f, endY = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun longClickModifier_longPressWithFrameDuringHold_runsActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(player, x = 25f, y = 35f, drawFrameDuringHold = true)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    @Test
    fun doubleClickModifier_slowDoubleTap_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun doubleClickModifier_tripleTap_runsActionsOnce() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchDoubleTap(player, x = 25f, y = 35f)
        dispatchQuickTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(3 to "")
    }

    @Test
    fun doubleClickModifier_secondTapFarAway_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 10f, y = 10f)
        dispatchQuickTap(player, x = 190f, y = 190f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun combinedClick_tap_runsOnlySingleClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onSingleMultiClick(HostAction(1))
                        .onLongClick(HostAction(2))
                        .onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun combinedClick_doubleTap_runsSingleClickOnceAndDoubleClickOnce() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onSingleMultiClick(HostAction(1))
                        .onLongClick(HostAction(2))
                        .onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchDoubleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "", 3 to "").inOrder()
    }

    @Test
    fun combinedClick_longPress_runsOnlyLongClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onSingleMultiClick(HostAction(1))
                        .onLongClick(HostAction(2))
                        .onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(player, x = 25f, y = 35f, drawFrameDuringHold = true)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    @Test
    fun combinedClick_longPressWithoutFrame_runsOnlyLongClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onSingleMultiClick(HostAction(1))
                        .onLongClick(HostAction(2))
                        .onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act - No frame is drawn during the hold: the long press is detected on release.
        dispatchLongPress(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    @Test
    fun longClickModifier_pressShorterThanPlatformLongPressTimeout_doesNotRunActions() {
        // Arrange.
        TestViewConfiguration.setLongPressTimeout(1000)
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(
            player,
            x = 25f,
            y = 35f,
            holdTimeMs = 600L,
            drawFrameDuringHold = true,
        )

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun longClickModifier_pressLongerThanPlatformLongPressTimeout_runsActions() {
        // Arrange.
        TestViewConfiguration.setLongPressTimeout(200)
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(
            player,
            x = 25f,
            y = 35f,
            holdTimeMs = 300L,
            drawFrameDuringHold = true,
        )

        // Assert.
        assertThat(triggeredActions).containsExactly(2 to "")
    }

    @Test
    fun doubleClickModifier_secondTapAfterPlatformDoubleTapTimeout_doesNotRunActions() {
        // Arrange.
        TestViewConfiguration.setDoubleTapTimeout(30)
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act - The second tap starts 50ms after the first one ends.
        dispatchDoubleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun doubleClickModifier_secondTapWithinPlatformDoubleTapTimeout_runsActions() {
        // Arrange.
        TestViewConfiguration.setDoubleTapTimeout(1000)
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act - The second tap starts 500ms after the first one ends.
        dispatchSingleTap(player, x = 25f, y = 35f)
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(3 to "")
    }

    @Test
    fun doubleClickModifier_secondTapBeyondPlatformDoubleTapSlop_doesNotRunActions() {
        // Arrange.
        TestViewConfiguration.setScaledDoubleTapSlop(10)
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)
        dispatchQuickTap(player, x = 55f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun singleClickModifier_accessibilityClick_runsActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        val triggeredHaptics = mutableListOf<Int>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val document = player.document.document
        document.setHapticEngine { type -> triggeredHaptics.add(type) }
        val box = document.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()

        // Act. This is what the player does for an accessibility click.
        player.performClick(document, box, "")

        // Assert. The listener is also told which component was clicked.
        assertThat(triggeredActions).contains(1 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun combinedClick_accessibilityClick_runsOnlySingleClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onSingleMultiClick(HostAction(1))
                        .onLongClick(HostAction(2))
                        .onDoubleClick(HostAction(3)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val document = player.document.document
        val box = document.rootLayoutComponent!!.list.filterIsInstance<LayoutComponent>().single()

        // Act. This is what the player does for an accessibility click.
        player.performClick(document, box, "")

        // Assert. The listener is also told which component was clicked.
        assertThat(triggeredActions).contains(1 to "")
        assertThat(triggeredActions).doesNotContain(2 to "")
        assertThat(triggeredActions).doesNotContain(3 to "")
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only. Skipped on the legacy path.
    // ------------------------------------------------------------------------------------------

    @Test
    fun singleClickModifier_longPressInsideBox_runsActions() {
        // The legacy touch path does not click when the pointer was held past the long press
        // timeout.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun singleClickModifier_dragWithinBox_runsActions() {
        // The legacy touch path cancels the click on any drag beyond the touch slop.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 75f, endY = 65f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun singleClickModifier_downConsumedByChild_doesNotRunActions() {
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onEvent(
                                /* type = */ EVENT_TYPE_POINTER,
                                /* filter = */ MetadataUtil.pack(
                                    POINTER_PASS_MAIN,
                                    POINTER_SUBTYPE_DOWN,
                                    0,
                                ),
                                /* flags = */ POINTER_STATUS_CONSUMED,
                                /* dataIds = */ null,
                                /* condition = */ null,
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun singleClickModifier_upConsumedByChild_doesNotRunActions() {
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier()
                            .size(100, 100)
                            .onEvent(
                                /* type = */ EVENT_TYPE_POINTER,
                                /* filter = */ MetadataUtil.pack(
                                    POINTER_PASS_MAIN,
                                    POINTER_SUBTYPE_UP,
                                    0,
                                ),
                                /* flags = */ POINTER_STATUS_CONSUMED,
                                /* dataIds = */ null,
                                /* condition = */ null,
                            ),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSingleTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun singleClickModifier_moveConsumedByParent_cancelsClick() {
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(200, 200)
                        .onEvent(
                            /* type = */ EVENT_TYPE_POINTER,
                            /* filter = */ MetadataUtil.pack(
                                POINTER_PASS_MAIN,
                                POINTER_SUBTYPE_MOVE,
                                0,
                            ),
                            /* flags = */ POINTER_STATUS_CONSUMED,
                            /* dataIds = */ null,
                            /* condition = */ null,
                        ),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onSingleMultiClick(HostAction(1)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchSwipe(player, startX = 25f, startY = 35f, endX = 75f, endY = 65f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun longClickModifier_moveConsumedByParent_cancelsLongPress() {
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(200, 200)
                        .onEvent(
                            /* type = */ EVENT_TYPE_POINTER,
                            /* filter = */ MetadataUtil.pack(
                                POINTER_PASS_MAIN,
                                POINTER_SUBTYPE_MOVE,
                                0,
                            ),
                            /* flags = */ POINTER_STATUS_CONSUMED,
                            /* dataIds = */ null,
                            /* condition = */ null,
                        ),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onLongClick(HostAction(2)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchLongPress(
            player,
            x = 25f,
            y = 35f,
            moveBeforeHold = true,
            drawFrameDuringHold = true,
        )

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    private fun RecordingModifier.onSingleMultiClick(vararg actions: Action): RecordingModifier =
        then { writer ->
            writer.addClickModifierOperation(MultiClickModifier.CLICK_TYPE_SINGLE)
            actions.forEach { it.write(writer) }
            writer.addContainerEnd()
        }

    private val robolectricClock: RemoteClock =
        androidx.compose.remote.core.SystemClock(
            object : Clock() {
                override fun getZone(): ZoneId = ZoneOffset.UTC

                override fun withZone(zone: ZoneId): Clock = this

                override fun instant(): Instant = Instant.ofEpochMilli(SystemClock.uptimeMillis())
            }
        )

    private fun createPlayer(
        width: Int = 200,
        height: Int = 200,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): RemoteComposePlayer {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val unifiedPointerInputTag =
            RemoteComposeWriter.hTag(
                Header.FEATURE_UNIFIED_POINTER_INPUT,
                if (unifiedPointerInput) 1 else 0,
            )
        val profile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                profileMask,
                RcPlatformServices.None,
            ) { displayInfo, p, _ ->
                RemoteComposeWriter(
                    p,
                    RemoteComposeBuffer(),
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, displayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, displayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                    unifiedPointerInputTag,
                )
            }
        val writer =
            RemoteComposeWriter(
                profile,
                RemoteComposeBuffer(),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
                unifiedPointerInputTag,
            )
        writer.buildDocument()

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        val player = RemoteComposePlayer(activity)
        // The document clock follows Robolectric's clock, which the gestures below advance, so
        // that operations timing gestures with the document clock see the same time as the view.
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
        parent.draw(Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)))
        return player
    }

    private fun dispatchSingleTap(view: View, x: Float, y: Float) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
        val upTime = SystemClock.uptimeMillis()
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    private fun dispatchDoubleTap(view: View, x: Float, y: Float) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime1 = SystemClock.uptimeMillis()
        val downEvent1 = MotionEvent.obtain(downTime1, downTime1, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent1)
        downEvent1.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
        val upTime1 = SystemClock.uptimeMillis()
        val upEvent1 = MotionEvent.obtain(downTime1, upTime1, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent1)
        upEvent1.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(50))
        val downTime2 = SystemClock.uptimeMillis()
        val downEvent2 = MotionEvent.obtain(downTime2, downTime2, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent2)
        downEvent2.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
        val upTime2 = SystemClock.uptimeMillis()
        val upEvent2 = MotionEvent.obtain(downTime2, upTime2, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent2)
        upEvent2.recycle()
    }

    /**
     * @param moveBeforeHold moves the pointer slightly (staying inside a 100x100 box) right after
     *   the down
     * @param drawFrameDuringHold draws a frame once the hold time has elapsed and before the
     *   pointer is lifted, like the render loop does on a device
     */
    private fun dispatchLongPress(
        view: View,
        x: Float,
        y: Float,
        holdTimeMs: Long = 600L,
        moveBeforeHold: Boolean = false,
        drawFrameDuringHold: Boolean = false,
    ) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        if (moveBeforeHold) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(20))
            val moveTime = SystemClock.uptimeMillis()
            val moveEvent =
                MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, x + 20f, y, 0)
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        }

        ShadowSystemClock.advanceBy(Duration.ofMillis(holdTimeMs))
        if (drawFrameDuringHold) {
            view.draw(Canvas(Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)))
            ShadowLooper.idleMainLooper()
        }
        val upTime = SystemClock.uptimeMillis()
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    /** A tap that follows the previous one closely enough to count as a double tap. */
    private fun dispatchQuickTap(view: View, x: Float, y: Float) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(50))
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
        val upTime = SystemClock.uptimeMillis()
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    private fun dispatchCancelledTap(view: View, x: Float, y: Float) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(10))
        val cancelTime = SystemClock.uptimeMillis()
        val cancelEvent =
            MotionEvent.obtain(downTime, cancelTime, MotionEvent.ACTION_CANCEL, x, y, 0)
        view.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
    }

    private fun dispatchSwipe(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 5,
    ) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        for (step in 1..steps) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(20))
            val x = startX + (endX - startX) * step / steps
            val y = startY + (endY - startY) * step / steps
            val moveEvent =
                MotionEvent.obtain(
                    downTime,
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_MOVE,
                    x,
                    y,
                    0,
                )
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        }

        ShadowSystemClock.advanceBy(Duration.ofMillis(20))
        val upEvent =
            MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                endX,
                endY,
                0,
            )
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
