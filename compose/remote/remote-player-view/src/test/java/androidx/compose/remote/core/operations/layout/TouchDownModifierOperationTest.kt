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
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.view.RemoteComposePlayer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Touch down modifier tests, run against both implementations of the modifier:
 * - `unifiedPointerInput = false`: [TouchDownModifierOperation], driven by the legacy touch path.
 * - `unifiedPointerInput = true`: [TouchDownModifierOperation2], driven by the
 *   [androidx.compose.remote.core.input.PointerInputRouter].
 *
 * The documents are identical; only [Header.FEATURE_UNIFIED_POINTER_INPUT] differs.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TouchDownModifierOperationTest(private val unifiedPointerInput: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "unifiedPointerInput = {0}")
        fun data() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @Test
    fun actionDownInsideBox_runsActionsImmediately() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onTouchDown(HostAction(10)),
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

        // Assert.
        assertThat(triggeredActions).containsExactly(10 to "")
    }

    @Test
    fun actionDownOutsideBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onTouchDown(HostAction(10)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 150f, 50f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun actionDownInsideInvisibleBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            val visibilityId = addInteger(Component.Visibility.INVISIBLE).toInt()
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .visibility(visibilityId)
                        .onTouchDown(HostAction(10)),
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

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun actionDownInsideBoxInStateLayout_runsActions() {
        // Arrange.
        val player = createPlayer {
            val stateIndex = addInteger(0).toInt()
            root {
                stateLayout(RecordingModifier().size(200, 200), stateIndex) {
                    box(
                        RecordingModifier().size(100, 100).onTouchDown(HostAction(10)),
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
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(10 to "")
    }

    @Test
    fun actionMoveAndActionUpAfterDown_doNotReRunTouchDownActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onTouchDown(HostAction(10)),
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
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_MOVE, 60f, 60f)
        dispatchMotionEvent(player, downTime, downTime + 40L, MotionEvent.ACTION_UP, 60f, 60f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun touchDownAndClickOnSameBox_bothRun() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onTouchDown(HostAction(10))
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
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(10 to "", 42 to "")
    }

    @Test
    fun actionDownInsideClickableChild_runsParentTouchDownActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onTouchDown(HostAction(10)),
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
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }
        val downTime = SystemClock.uptimeMillis()

        // Act.
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(10 to "")
    }

    @Test
    fun touchDownOnChild_doesNotPreventParentClick() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(200, 200).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {
                    box(
                        RecordingModifier().size(100, 100).onTouchDown(HostAction(10)),
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
        dispatchMotionEvent(player, downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f)
        dispatchMotionEvent(player, downTime, downTime + 20L, MotionEvent.ACTION_UP, 50f, 50f)

        // Assert.
        assertThat(triggeredActions).containsExactly(10 to "", 42 to "")
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
}
