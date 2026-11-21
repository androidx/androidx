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
import android.graphics.Paint
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RippleModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.RemoteComposePlayer
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock

/**
 * Ripple modifier tests, run against both implementations of the modifier:
 * - `unifiedPointerInput = false`: [RippleModifierOperation], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [RippleModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RippleModifierOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @Test
    fun beforeAnyTouch_drawsNoRipple() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun actionDownInsideBox_drawsRippleAtTouchPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun actionDownOutsideBox_drawsNoRipple() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 150f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun dragInsideBox_keepsDrawingRippleAtDownPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_MOVE, 75f, 65f)

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun actionUp_fadesRippleOut() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        drawRipples(player)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_UP, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(2000))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun secondActionDown_drawsRippleAtNewTouchPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val firstDownTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, firstDownTime, firstDownTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(
            player,
            firstDownTime,
            firstDownTime + 20L,
            MotionEvent.ACTION_UP,
            25f,
            35f,
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(2000))
        drawRipples(player)
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 65f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 75f, y = 65f))
    }

    @Test
    fun actionDownInsideInvisibleBox_drawsNoRipple() {
        // Arrange.
        val player = createPlayer {
            val visibilityId = addInteger(Component.Visibility.INVISIBLE).toInt()
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .visibility(visibilityId)
                        .then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun rippleAndClickOnSameBox_tapDrawsRippleAndRunsClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .then(RippleModifier())
                        .onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        val ripplesWhilePressed = drawRipples(player)
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_UP, 25f, 35f)

        // Assert.
        assertThat(ripplesWhilePressed).containsExactly(Ripple(x = 25f, y = 35f))
        assertThat(triggeredActions).containsExactly(42 to "")
    }

    @Test
    fun actionDownInsideClickableChild_drawsRippleOnParent() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onClick(HostAction(42)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun rippleOnChild_doesNotPreventParentClick() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).then(RippleModifier()),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        val ripplesWhilePressed = drawRipples(player)
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_UP, 25f, 35f)

        // Assert.
        assertThat(ripplesWhilePressed).containsExactly(Ripple(x = 25f, y = 35f))
        assertThat(triggeredActions).containsExactly(42 to "")
    }

    @Test
    fun nestedRipples_actionDownInsideChild_drawsRippleOnParentAndChild() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).then(RippleModifier()),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player))
            .containsExactly(Ripple(x = 25f, y = 35f), Ripple(x = 25f, y = 35f))
    }

    @Test
    fun nestedRipples_actionDownOutsideChild_drawsRippleOnParentOnly() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).then(RippleModifier()),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 150f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 150f, y = 150f))
    }

    @Test
    fun secondPointerDown_keepsDrawingRippleAtFirstTouchPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun secondPointerUp_keepsDrawingRipple() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 40L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun actionDownAfterTwoPointerGesture_secondPointerLiftedFirst_drawsRippleAtNewTouchPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        // A two pointer gesture in which the second pointer is lifted first.
        val gestureTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, gestureTime, gestureTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 40L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 60L,
            MotionEvent.ACTION_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 25f, y = 35f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(2000))
        drawRipples(player)
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 50f, y = 50f))
    }

    @Test
    fun actionDownAfterTwoPointerGesture_firstPointerLiftedFirst_drawsRippleAtNewTouchPosition() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        // A two pointer gesture in which the first pointer is lifted first.
        val gestureTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, gestureTime, gestureTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 40L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        dispatchMotionEvent(
            player,
            gestureTime,
            gestureTime + 60L,
            MotionEvent.ACTION_UP,
            actionIndex = 0,
            Pointer(id = 1, x = 75f, y = 65f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(2000))
        drawRipples(player)
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 50f, y = 50f))
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only. Skipped on the legacy path.
    // ------------------------------------------------------------------------------------------

    @Test
    fun actionDownInsideBoxNotAtOrigin_drawsRippleAtTouchPosition() {
        // The legacy ripple is drawn at the wrong position when its box is not at the origin
        // of the document.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(RecordingModifier().size(200, 200), BoxLayout.END, BoxLayout.BOTTOM) {
                    box(
                        RecordingModifier().size(100, 100).then(RippleModifier()),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 125f, 135f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 125f, y = 135f))
    }

    @Test
    fun pointerHeld_keepsDrawingRipple() {
        // The legacy ripple is a one second animation started by the down, however long the
        // pointer is held.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(5000))

        // Assert.
        assertThat(drawRipples(player)).containsExactly(Ripple(x = 25f, y = 35f))
    }

    @Test
    fun actionUpAfterLongHold_fadesRippleOut() {
        // The legacy ripple is a one second animation started by the down, however long the
        // pointer is held.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(5000))
        val ripplesWhileHeld = drawRipples(player)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 5000L, MotionEvent.ACTION_UP, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        val ripplesWhileFading = drawRipples(player)
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
        val ripplesAfterFade = drawRipples(player)

        // Assert.
        assertThat(ripplesWhileHeld).containsExactly(Ripple(x = 25f, y = 35f))
        assertThat(ripplesWhileFading).containsExactly(Ripple(x = 25f, y = 35f))
        assertThat(ripplesAfterFade).isEmpty()
    }

    @Test
    fun dragOutsideBox_fadesRippleOut() {
        // The legacy ripple ignores the pointer once it is down.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        drawRipples(player)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_MOVE, 150f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun actionCancel_fadesRippleOut() {
        // The legacy ripple ignores the pointer once it is down.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        drawRipples(player)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 100L, MotionEvent.ACTION_CANCEL, 25f, 35f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    @Test
    fun rippleAndClickOnSameBox_tapPerformsSingleHaptic() {
        // The legacy ripple performs a haptic of its own on down, in addition to the one of
        // the click.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .then(RippleModifier())
                        .onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredHaptics = mutableListOf<Int>()
        player.document.document.setHapticEngine { type -> triggeredHaptics.add(type) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 25f, 35f)

        // Assert.
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun firstPointerUpWhileSecondPointerIsDown_fadesRippleOut() {
        // The legacy ripple ignores the pointer once it is down.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).then(RippleModifier()),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 25f, 35f)
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 20L,
            MotionEvent.ACTION_POINTER_DOWN,
            actionIndex = 1,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(100))
        drawRipples(player)

        // Act.
        dispatchMotionEvent(
            player,
            downTime,
            downTime + 100L,
            MotionEvent.ACTION_POINTER_UP,
            actionIndex = 0,
            Pointer(id = 0, x = 25f, y = 35f),
            Pointer(id = 1, x = 75f, y = 65f),
        )
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))

        // Assert.
        assertThat(drawRipples(player)).isEmpty()
    }

    /** The center of a ripple drawn in a frame, in the coordinates of the player. */
    private data class Ripple(val x: Float, val y: Float)

    /** Draws a frame of [player] and returns the ripples visible in it. */
    private fun drawRipples(player: RemoteComposePlayer): List<Ripple> {
        val ripples = mutableListOf<Ripple>()
        val bitmap = Bitmap.createBitmap(player.width, player.height, Bitmap.Config.ARGB_8888)
        val canvas =
            object : Canvas(bitmap) {
                override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) {
                    super.drawCircle(cx, cy, radius, paint)
                    // A ripple that has not started to expand or that has faded out is not
                    // visible.
                    if (radius > 0f && paint.alpha > 0) {
                        val center = floatArrayOf(cx, cy)
                        @Suppress("DEPRECATION") matrix.mapPoints(center)
                        ripples.add(Ripple(center[0], center[1]))
                    }
                }
            }
        player.draw(canvas)
        return ripples
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
        // The document clock follows Robolectric's clock, so that the ripple animation advances
        // with ShadowSystemClock.
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
