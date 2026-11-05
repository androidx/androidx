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
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.view.RemoteComposePlayer
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Touch cancel modifier tests, run against both implementations of the modifier:
 * - `unifiedPointerInput = false`: [TouchCancelModifierOperation], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [TouchCancelModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs. Behaviors that
 * only exist with unified pointer input are skipped on the legacy path.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TouchCancelModifierOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @Test
    fun actionCancelInsideBoxAfterDownInsideBox_runsTouchCancelActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(1))
                        .onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        triggeredActions.clear()

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_CANCEL, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(30 to "")
    }

    @Test
    fun actionCancelOutsideBoxAfterDownInsideBox_runsTouchCancelActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(1))
                        .onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        triggeredActions.clear()

        // Act.
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 150f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_CANCEL, 150f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(30 to "")
    }

    @Test
    fun actionDownAndUpWithoutCancel_doNotRunTouchCancelActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(1))
                        .onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 60f, 60f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 60f, 60f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    @Test
    fun touchDownAndCancelOutsideBox_doNotRunTouchCancelActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(1))
                        .onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_CANCEL, 150f, 150f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun dragReleasingOutsideBox_doesNotRunTouchCancelActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(1))
                        .onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 150f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 150f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "")
    }

    // ------------------------------------------------------------------------------------------
    // Unified pointer input only. Skipped on the legacy path.
    // ------------------------------------------------------------------------------------------

    @Test
    fun actionCancelWithoutTouchDownModifier_runsTouchCancelActions() {
        // The legacy touch path only runs touch cancel actions next to a touch down modifier.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onTouchCancel(HostAction(30)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_CANCEL, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(30 to "")
    }

    @Test
    fun dragThatScrollsParent_runsTouchCancelActions() {
        // The legacy touch path does not run touch cancel actions when a scrolling parent
        // takes over the gesture.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            val scrollPositionId = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().fillMaxSize().verticalScroll(scrollPositionId),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    box(
                        RecordingModifier()
                            .fillMaxWidth()
                            .height(200f)
                            .onTouchDown(HostAction(1))
                            .onTouchCancel(HostAction(30)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                    for (i in 1..10) {
                        box(
                            RecordingModifier().fillMaxWidth().height(100f),
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
        dispatchSwipe(player, downTime, startX = 50f, startY = 150f, endX = 50f, endY = 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "", 30 to "").inOrder()
    }

    @Test
    fun dragThatScrollsParentThenActionCancel_runsTouchCancelActionsOnce() {
        // The legacy touch path runs the touch cancel actions twice for this gesture.
        assumeTrue(unifiedPointerInput)

        // Arrange.
        val player = createPlayer {
            val scrollPositionId = addNamedFloat("scrollPosition", 0f)
            root {
                column(
                    RecordingModifier().fillMaxSize().verticalScroll(scrollPositionId),
                    ColumnLayout.START,
                    ColumnLayout.TOP,
                ) {
                    box(
                        RecordingModifier()
                            .fillMaxWidth()
                            .height(200f)
                            .onTouchDown(HostAction(1))
                            .onTouchCancel(HostAction(30)),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {}
                    for (i in 1..10) {
                        box(
                            RecordingModifier().fillMaxWidth().height(100f),
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
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 150f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 50f, 100f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_MOVE, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 60L, MotionEvent.ACTION_CANCEL, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(1 to "", 30 to "").inOrder()
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
        player.setDocument(writer.encodeToByteArray())
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

    private fun dispatchSwipe(
        view: View,
        downTime: Long,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 5,
    ) {
        var eventTime = downTime
        dispatchMotionEvent(view, downTime, eventTime, MotionEvent.ACTION_DOWN, startX, startY)
        for (step in 1..steps) {
            eventTime += 20L
            val x = startX + (endX - startX) * step / steps
            val y = startY + (endY - startY) * step / steps
            dispatchMotionEvent(view, downTime, eventTime, MotionEvent.ACTION_MOVE, x, y)
        }
        eventTime += 20L
        dispatchMotionEvent(view, downTime, eventTime, MotionEvent.ACTION_UP, endX, endY)
    }
}
