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
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ClickModifierOperationTest {

    @Test
    fun tapInsideBox_runsAllClickActions() {
        // Arrange.
        var intId = 0
        var floatId = 0
        val player = createPlayer {
            intId = addInteger(0).toInt()
            floatId = Utils.idFromNan(addFloatConstant(0f))
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .onClick(
                            ValueIntegerChange(intId, 5),
                            ValueFloatChange(floatId, 7.5f),
                            HostAction(42),
                        ),
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
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        val state = player.document.document.remoteComposeState
        assertThat(state.getInteger(intId)).isEqualTo(5)
        assertThat(state.getFloat(floatId)).isWithin(1e-5f).of(7.5f)
        assertThat(triggeredActions).containsExactly(42 to "")
        assertThat(triggeredHaptics).containsExactly(3)
    }

    @Test
    fun tapOutsideBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 150f, y = 150f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    @Test
    fun dragReleasingOutsideBox_doesNotRunClickActions() {
        // Arrange.
        val player = createPlayer {
            root {
                box(
                    RecordingModifier().size(100, 100).onClick(HostAction(42)),
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
    fun tapInsideInvisibleBox_doesNotRunActions() {
        // Arrange.
        val player = createPlayer {
            val visibilityId = addInteger(Component.Visibility.INVISIBLE).toInt()
            root {
                box(
                    RecordingModifier()
                        .size(100, 100)
                        .visibility(visibilityId)
                        .onClick(HostAction(42)),
                    BoxLayout.START,
                    BoxLayout.TOP,
                ) {}
            }
        }
        val triggeredActions = mutableListOf<Pair<Int, String?>>()
        player.addIdActionListener { id, metadata -> triggeredActions.add(id to metadata) }

        // Act.
        dispatchTap(player, x = 25f, y = 35f)

        // Assert.
        assertThat(triggeredActions).isEmpty()
    }

    private fun createPlayer(
        width: Int = 200,
        height: Int = 200,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): RemoteComposePlayer {
        val writer = RemoteComposeWriter.obtain(width, height, RcPlatformProfiles.ANDROIDX)
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

    private fun dispatchTap(view: View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        val upTime = downTime + 10L
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    private fun dispatchSwipe(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 5,
    ) {
        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val downEvent =
            MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        for (step in 1..steps) {
            eventTime += 20L
            val x = startX + (endX - startX) * step / steps
            val y = startY + (endY - startY) * step / steps
            val moveEvent =
                MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0)
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        }

        eventTime += 20L
        val upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, endX, endY, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
