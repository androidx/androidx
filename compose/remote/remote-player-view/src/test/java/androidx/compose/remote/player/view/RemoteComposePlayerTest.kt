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

package androidx.compose.remote.player.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteComposePlayerTest {

    private var lastEventTime = SystemClock.uptimeMillis()

    @Test
    fun init_inEditMode_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val playerInEditMode =
            object : RemoteComposePlayer(context) {
                override fun isInEditMode(): Boolean = true
            }
        assertTrue("player should be in edit mode", playerInEditMode.isInEditMode)
    }

    @Test
    fun scrollableComponent_disallowsParentIntercept_soDragIsNotPropagatedToHost() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val host = HostViewGroup(context)
        val player = RemoteComposePlayer(context)
        player.setDocument(docBytes)
        host.addView(player, FrameLayout.LayoutParams(300, 300))

        host.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, 300, 300)
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))

        // Swipe inside the scrollable left box (75, 250 -> 75, 50)
        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Host parent should not intercept drag gesture when RemoteComposePlayer has a scrollable component",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun nonInteractiveComponent_allowsParentIntercept_soDragIsPropagatedToHost() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = false)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val host = HostViewGroup(context)
        val player = RemoteComposePlayer(context)
        player.setDocument(docBytes)
        host.addView(player, FrameLayout.LayoutParams(300, 300))

        host.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, 300, 300)
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))

        // Swipe inside the non-interactive box (75, 250 -> 75, 50)
        performSwipe(host, 75f, 250f, 75f, 50f)

        assertTrue(
            "Host parent should intercept drag gesture when RemoteComposePlayer has no interactive components",
            host.hostInterceptedDrag,
        )
        assertFalse("Host parent disallowIntercept should remain false", host.disallowIntercept)
    }

    private class HostViewGroup(context: Context) : FrameLayout(context) {
        var disallowIntercept = false
        var hostInterceptedDrag = false

        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            this.disallowIntercept = disallowIntercept
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                disallowIntercept = false
            }
            return super.dispatchTouchEvent(ev)
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (disallowIntercept) {
                return false
            }
            if (ev.action == MotionEvent.ACTION_MOVE) {
                hostInterceptedDrag = true
                return true
            }
            return false
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (ev.action == MotionEvent.ACTION_MOVE) {
                hostInterceptedDrag = true
                return true
            }
            return super.onTouchEvent(ev)
        }
    }

    private fun performSwipe(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        steps: Int = 5,
    ) {
        lastEventTime += 500
        val downTime = lastEventTime

        val downEvent =
            MotionEvent.obtain(downTime, lastEventTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        for (i in 1..steps) {
            lastEventTime += 20
            val currentX = startX + (endX - startX) * i / steps
            val currentY = startY + (endY - startY) * i / steps
            val moveEvent =
                MotionEvent.obtain(
                    downTime,
                    lastEventTime,
                    MotionEvent.ACTION_MOVE,
                    currentX,
                    currentY,
                    0,
                )
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        }

        lastEventTime += 20
        val upEvent =
            MotionEvent.obtain(downTime, lastEventTime, MotionEvent.ACTION_UP, endX, endY, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    companion object {
        private fun createLeftBoxInteractiveDocument(
            isClickable: Boolean = false,
            isScrollable: Boolean = false,
            isTouchUp: Boolean = false,
        ): ByteArray {
            return createLeftBoxInteractiveDocumentWithActionId(
                    isClickable,
                    isScrollable,
                    isTouchUp,
                )
                .first
        }

        private fun createLeftBoxInteractiveDocumentWithActionId(
            isClickable: Boolean = false,
            isScrollable: Boolean = false,
            isTouchUp: Boolean = false,
        ): Pair<ByteArray, Int> {
            val rcDoc = RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)
            val actionTextId = rcDoc.addText("myActionName")
            val scrollPositionId = rcDoc.addNamedFloat("scrollPosition", 0f)
            rcDoc.root {
                rcDoc.row(RecordingModifier().fillMaxSize(), RowLayout.START, RowLayout.CENTER) {
                    val leftModifier = RecordingModifier().width(150f).fillMaxHeight()
                    val finalLeftModifier =
                        when {
                            isTouchUp ->
                                leftModifier
                                    .onTouchDown(HostAction(0))
                                    .onTouchUp(HostAction(actionTextId))
                            isClickable && isScrollable ->
                                leftModifier
                                    .onClick(HostAction(actionTextId))
                                    .verticalScroll(scrollPositionId)
                            isClickable -> leftModifier.onClick(HostAction(actionTextId))
                            isScrollable -> leftModifier.verticalScroll(scrollPositionId)
                            else -> leftModifier
                        }
                    rcDoc.box(finalLeftModifier, BoxLayout.CENTER, BoxLayout.CENTER) {}
                    rcDoc.box(
                        RecordingModifier().width(150f).fillMaxHeight(),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {}
                }
            }
            return Pair(rcDoc.encodeToByteArray(), actionTextId)
        }
    }
}
