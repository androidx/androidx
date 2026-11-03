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

package androidx.compose.remote.core.operations

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
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
import org.robolectric.shadows.ShadowSystemClock

/**
 * Touch expression tests, run against both implementations of the operation:
 * - `unifiedPointerInput = false`: [TouchExpression], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [TouchExpressionOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TouchExpressionOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @Test
    fun beforeAnyTouch_hasDefaultValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(10f)
    }

    @Test
    fun dragInsideCanvas_changesValueByDragDistance() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(40f)
    }

    @Test
    fun dragAcrossExpressionAxis_doesNotChangeValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 20f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(10f)
    }

    @Test
    fun dragBeyondMaximum_clampsValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(200, 200)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 190f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(100f)
    }

    @Test
    fun dragBelowMinimum_clampsValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(200, 200)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 190f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 20f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(0f)
    }

    @Test
    fun actionUpAfterDrag_keepsValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 50f, 50f)
        drawFrame(player)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(40f)
    }

    @Test
    fun actionCancelAfterDrag_keepsValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_CANCEL, 50f, 50f)
        drawFrame(player)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(40f)
    }

    @Test
    fun secondDrag_continuesFromCurrentValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val firstDownTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, firstDownTime, firstDownTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(
            player,
            firstDownTime,
            firstDownTime + 20L,
            MotionEvent.ACTION_MOVE,
            50f,
            50f,
        )
        dispatchMotionEvent(
            player,
            firstDownTime,
            firstDownTime + 40L,
            MotionEvent.ACTION_UP,
            50f,
            50f,
        )
        val downTime = firstDownTime + 500L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 10f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 30f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(60f)
    }

    @Test
    fun dragStartingOutsideCanvas_doesNotChangeValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 120f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 150f, 150f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(10f)
    }

    @Test
    fun touchExpressionInBox_dragInsideBox_changesValueByDragDistance() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(40f)
    }

    @Test
    fun stopEnds_actionUp_animatesValueToNearestEnd() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                            touchMode = TouchExpression.STOP_ENDS,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 80f, 50f)
        // Hold before lifting so that the pointer is released without velocity.
        ShadowSystemClock.advanceBy(Duration.ofMillis(1000))
        dispatchMotionEvent(player, downTime, downTime + 1000L, MotionEvent.ACTION_MOVE, 80f, 50f)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 1020L, MotionEvent.ACTION_UP, 80f, 50f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(5000))
        drawFrame(player)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(100f)
    }

    @Test
    fun stopEnds_actionUpWithVelocityTowardsFarEnd_animatesValueToFarEnd() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                            touchMode = TouchExpression.STOP_ENDS,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()
        // The value (20) stays nearer to the start, but the pointer moves fast towards the end.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 30f, 50f)

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 30f, 50f)
        ShadowSystemClock.advanceBy(Duration.ofMillis(5000))
        drawFrame(player)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(100f)
    }

    @Test
    fun absolutePosition_valueIsTouchPosition() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                canvas(RecordingModifier().size(100, 100)) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                            touchMode = TouchExpression.STOP_ABSOLUTE_POS,
                        )
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(50f)
    }

    @Test
    fun valueSetByAction_keepsValueAcrossFrames() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                column(RecordingModifier().size(200, 200), ColumnLayout.START, ColumnLayout.TOP) {
                    canvas(RecordingModifier().size(100, 100)) {
                        valueId =
                            addTouchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                defaultValue = 10f,
                                min = 0f,
                                max = 100f,
                            )
                    }
                    box(
                        RecordingModifier().size(100, 50).onClick(ValueFloatChange(valueId, 60f)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 125f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 50f, 125f)

        // Assert.
        val state = player.document.document.remoteComposeState
        repeat(3) {
            drawFrame(player)
            assertThat(state.getFloat(valueId)).isEqualTo(60f)
        }
    }

    @Test
    fun dragAfterValueSetByAction_continuesFromSetValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                column(RecordingModifier().size(200, 200), ColumnLayout.START, ColumnLayout.TOP) {
                    canvas(RecordingModifier().size(100, 100)) {
                        valueId =
                            addTouchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                defaultValue = 10f,
                                min = 0f,
                                max = 100f,
                            )
                    }
                    box(
                        RecordingModifier().size(100, 50).onClick(ValueFloatChange(valueId, 60f)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val clickTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, clickTime, clickTime, MotionEvent.ACTION_DOWN, 50f, 125f)
        dispatchMotionEvent(player, clickTime, clickTime + 20L, MotionEvent.ACTION_UP, 50f, 125f)
        drawFrame(player)
        val downTime = clickTime + 500L

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 50f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(90f)
    }

    @Test
    fun stopNotchesSingleEven_valueSetByAction_hasSetValue() {
        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                column(RecordingModifier().size(200, 200), ColumnLayout.START, ColumnLayout.TOP) {
                    canvas(RecordingModifier().size(100, 100)) {
                        valueId =
                            addTouchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                defaultValue = 10f,
                                min = 0f,
                                max = 100f,
                                touchMode = TouchExpression.STOP_NOTCHES_SINGLE_EVEN,
                                touchSpec = floatArrayOf(10f),
                            )
                    }
                    box(
                        RecordingModifier().size(100, 50).onClick(ValueFloatChange(valueId, 60f)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 125f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 50f, 125f)
        drawFrame(player)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(60f)
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only. Skipped on the legacy path.
    // ------------------------------------------------------------------------------------------

    @Test
    fun absolutePosition_canvasNotAtOrigin_valueIsTouchPositionInCanvas() {
        // The legacy touch path feeds the touch expression with the position of the pointer
        // in the document rather than in the canvas.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                box(RecordingModifier().size(200, 200), BoxLayout.END, BoxLayout.BOTTOM) {
                    canvas(RecordingModifier().size(100, 100)) {
                        valueId =
                            addTouchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_X,
                                defaultValue = 10f,
                                min = 0f,
                                max = 100f,
                                touchMode = TouchExpression.STOP_ABSOLUTE_POS,
                            )
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 120f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 150f, 150f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(50f)
    }

    @Test
    fun dragStartingOnClickableChild_doesNotChangeValue() {
        // On the legacy touch path a touch expression also follows a pointer that a child
        // handles.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        var valueId = 0
        val player = createPlayer {
            root {
                box(RecordingModifier().size(100, 100), BoxLayout.START, BoxLayout.TOP) {
                    valueId =
                        addTouchExpression(
                            RemoteContext.FLOAT_TOUCH_POS_X,
                            defaultValue = 10f,
                            min = 0f,
                            max = 100f,
                        )
                    box(
                        RecordingModifier().size(50, 50).onClick(HostAction(42)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 20f, 20f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 40f, 20f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(10f)
    }

    @Test
    fun dragAlongScrollAxisInsideScrollContainer_changesValueAndDoesNotScroll() {
        // On the legacy touch path the scroll container scrolls as well.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        var valueId = 0
        val player = createPlayer {
            val scrollPosition = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().size(200, 200).verticalScroll(scrollPosition),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    canvas(RecordingModifier().size(200, 100)) {
                        valueId =
                            addTouchExpression(
                                RemoteContext.FLOAT_TOUCH_POS_Y,
                                defaultValue = 10f,
                                min = 0f,
                                max = 100f,
                            )
                    }
                    repeat(4) {
                        box(RecordingModifier().size(200, 100), BoxLayout.START, BoxLayout.TOP) {}
                    }
                }
            }
        }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 20f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 100f, 40f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 100f, 10f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getFloat(valueId)).isEqualTo(0f)
        assertThat(player.getNamedFloat("scrollPosition")).isEqualTo(0f)
    }

    /**
     * Adds a touch expression whose value is driven by [expression], and returns the id of that
     * value.
     */
    private fun RemoteComposeWriter.addTouchExpression(
        vararg expression: Float,
        defaultValue: Float,
        min: Float,
        max: Float,
        touchMode: Int = TouchExpression.STOP_INSTANTLY,
        touchSpec: FloatArray? = null,
    ): Int =
        Utils.idFromNan(
            addTouch(
                defaultValue,
                min,
                max,
                touchMode,
                /* velocityId = */ 0f,
                /* touchEffects = */ 0,
                touchSpec,
                /* easingSpec = */ null,
                *expression,
            )
        )

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
        // The document clock follows Robolectric's clock, so that animations started by a touch
        // expression advance with ShadowSystemClock.
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
}
