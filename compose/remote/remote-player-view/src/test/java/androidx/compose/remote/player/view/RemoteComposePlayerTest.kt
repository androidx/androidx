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
import android.content.ContextWrapper
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
import androidx.compose.remote.player.view.platform.SoundSupport
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
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
    fun init_unimplementedAttributionContext_doesNotCrash() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val contextWithUnimplementedAttribution =
            object : ContextWrapper(baseContext) {
                override fun getApplicationContext(): Context = this

                override fun createAttributionContext(attributionTag: String?): Context {
                    throw RuntimeException("Not implemented. Must override in a subclass.")
                }
            }
        val soundSupport = SoundSupport()
        soundSupport.init(contextWithUnimplementedAttribution)

        val player = RemoteComposePlayer(contextWithUnimplementedAttribution)
        assertNotNull(player)
    }

    @Test
    fun clickableComponent_consumesWhenClickingInteractiveComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = true, isScrollable = false)
        var parentReceivedDown = false

        setupPlayerInParent(docBytes = docBytes, onParentDown = { parentReceivedDown = true })
            .use { (_, parent) ->
                // Click on the left side (x=75, y=150) -> inside the clickable box.
                performClick(parent, 75f, 150f)

                assertFalse(
                    "Parent should not receive down event for interactive component click",
                    parentReceivedDown,
                )
            }
    }

    @Test
    fun clickableComponent_consecutiveClicks_dispatchesEachClick() {
        val (docBytes, actionTextId) =
            createLeftBoxInteractiveDocumentWithActionId(isClickable = true, isScrollable = false)
        var clickCount = 0

        val (player, parent) = setupPlayerInParent(docBytes = docBytes)
        player.addIdActionListener { id, _ ->
            if (id == actionTextId) {
                clickCount++
            }
        }

        // First click on the clickable box (75, 150)
        performClick(parent, 75f, 150f)
        assertEquals(1, clickCount)

        // Second click on the clickable box (75, 150)
        performClick(parent, 75f, 150f)
        assertEquals(2, clickCount)
    }

    @Test
    fun clickableComponent_doesNotConsumeWhenClickingNonInteractiveComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = true, isScrollable = false)
        var parentReceivedDown = false

        setupPlayerInParent(docBytes = docBytes, onParentDown = { parentReceivedDown = true })
            .use { (_, parent) ->
                // Click on the right side (x=225, y=150) -> inside the non-clickable box.
                performClick(parent, 225f, 150f)

                assertTrue(
                    "Parent should receive down event for non-interactive component click",
                    parentReceivedDown,
                )
            }
    }

    @Test
    fun clickableComponent_doesNotClickWhenReleasingDragGesture() {
        val (docBytes, actionTextId) =
            createLeftBoxInteractiveDocumentWithActionId(isClickable = true, isScrollable = false)
        var parentReceivedDown = false
        var actionTriggered = false

        val (player, parent) =
            setupPlayerInParent(docBytes = docBytes, onParentDown = { parentReceivedDown = true })
        player.addIdActionListener { id, _ ->
            if (id == actionTextId) {
                actionTriggered = true
            }
        }

        // Swipe starting inside the clickable box (75, 150) to (250, 150)
        performSwipe(parent, 75f, 150f, 250f, 150f)

        assertFalse("Action should not trigger on drag gesture release", actionTriggered)
        assertFalse(
            "Parent should not receive down event when touch starts in interactive component",
            parentReceivedDown,
        )
    }

    @Ignore("b/539940564")
    @Test
    fun touchUpComponent_doesNotTriggerActionWhenReleasingDragGesture() {
        val (docBytes, actionTextId) =
            createLeftBoxInteractiveDocumentWithActionId(
                isClickable = false,
                isScrollable = false,
                isTouchUp = true,
            )
        var actionTriggered = false

        val (player, parent) = setupPlayerInParent(docBytes = docBytes)
        player.addIdActionListener { id, _ ->
            if (id == actionTextId) {
                actionTriggered = true
            }
        }

        // Swipe starting inside the box (75, 150) to (250, 150)
        performSwipe(parent, 75f, 150f, 250f, 150f)

        assertFalse("TouchUp action should not trigger on drag gesture release", actionTriggered)
    }

    @Test
    fun scrollableComponent_onlyConsumesWhenScrollingInteractiveComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentReceivedDown = false
        var parentReceivedMove = false

        val (_, parent) =
            setupPlayerInParent(
                docBytes = docBytes,
                onParentTouch = { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> parentReceivedDown = true
                        MotionEvent.ACTION_MOVE -> parentReceivedMove = true
                    }
                },
            )

        // 1. Swipe on the left side (start at 75, 250, swipe up to 75, 50) -> inside the scrollable
        // box.
        performSwipe(parent, 75f, 250f, 75f, 50f)

        assertFalse(
            "Parent should not receive down for scrollable component swipe",
            parentReceivedDown,
        )
        assertFalse(
            "Parent should not receive move for scrollable component swipe",
            parentReceivedMove,
        )

        parentReceivedDown = false
        parentReceivedMove = false

        // 2. Swipe on the right side (start at 225, 250, swipe up to 225, 50) -> inside the
        // non-scrollable box.
        performSwipe(parent, 225f, 250f, 225f, 50f)

        assertTrue(
            "Parent should receive down for non-scrollable component swipe",
            parentReceivedDown,
        )
        assertTrue(
            "Parent should receive move for non-scrollable component swipe",
            parentReceivedMove,
        )
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
    fun player_limiterConfig_andTouchBoost_updatesProperly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = RemoteComposePlayer(context)
        player.maxFps = 90
        org.junit.Assert.assertEquals(90, player.maxFps)
        player.maxAvgFps = 12
        org.junit.Assert.assertEquals(12, player.maxAvgFps)
        player.fpsWindow = 8
        org.junit.Assert.assertEquals(8, player.fpsWindow)
        player.touchBoost()
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

    @Ignore("b/514549600")
    @Test
    fun clickingScrollableComponent_withoutClickable_doesNotConsume() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentReceivedDown = false

        setupPlayerInParent(docBytes = docBytes, onParentDown = { parentReceivedDown = true })
            .use { (_, parent) ->
                // Click on the left side (x=75, y=150) -> inside the scrollable box that is not
                // clickable.
                performClick(parent, 75f, 150f)

                assertTrue(
                    "Parent should receive down event for scrollable component click when component is not clickable",
                    parentReceivedDown,
                )
            }
    }

    private fun setupPlayerInParent(
        docBytes: ByteArray,
        onParentDown: () -> Unit = {},
        onParentTouch: (MotionEvent) -> Unit = {},
        width: Int = 300,
        height: Int = 300,
    ): TestFixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)
        val player = RemoteComposePlayer(context)
        player.setDocument(docBytes)

        parent.addView(player, FrameLayout.LayoutParams(width, height))
        parent.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onParentDown()
            }
            onParentTouch(event)
            true
        }

        // Force draw to initialize layout component bounds
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        parent.draw(canvas)

        return TestFixture(player, parent)
    }

    private class TestFixture(val player: RemoteComposePlayer, val parent: FrameLayout) :
        AutoCloseable {
        operator fun component1(): RemoteComposePlayer = player

        operator fun component2(): FrameLayout = parent

        override fun close() {}
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

    private fun performClick(view: View, x: Float, y: Float) {
        lastEventTime += 500
        val downEvent =
            MotionEvent.obtain(lastEventTime, lastEventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        lastEventTime += 10
        val upEvent =
            MotionEvent.obtain(lastEventTime - 10, lastEventTime, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
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
