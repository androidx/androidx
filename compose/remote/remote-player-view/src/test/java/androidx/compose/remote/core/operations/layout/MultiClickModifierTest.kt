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
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.Action
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class MultiClickModifierTest {

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
    fun singleClickModifier_longPressInsideBox_doesNotRunActions() {
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
        assertThat(triggeredActions).isEmpty()
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

    private fun RecordingModifier.onSingleMultiClick(vararg actions: Action): RecordingModifier =
        then { writer ->
            writer.addClickModifierOperation(MultiClickModifier.CLICK_TYPE_SINGLE)
            actions.forEach { it.write(writer) }
            writer.addContainerEnd()
        }

    private fun createPlayer(
        width: Int = 200,
        height: Int = 200,
        buildDocument: RemoteComposeWriter.() -> Unit,
    ): RemoteComposePlayer {
        val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
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
                )
            }
        val writer =
            RemoteComposeWriter(
                profile,
                RemoteComposeBuffer(),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
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

    private fun dispatchLongPress(view: View, x: Float, y: Float, holdTimeMs: Long = 600L) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(500))
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(holdTimeMs))
        val upTime = SystemClock.uptimeMillis()
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
