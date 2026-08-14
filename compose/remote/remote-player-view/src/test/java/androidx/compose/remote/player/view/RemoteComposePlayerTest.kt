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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import java.time.Duration
import kotlin.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

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
    fun scrollableComponent_propagatesClickToParentWhenClickingNonClickableScrollableComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnClickListener { parentClicked = true }

            // Click on the left side (x=75, y=150) -> inside the scrollable box that is not
            // clickable.
            performClick(parent, 75f, 150f)

            assertTrue(
                "Parent should receive click event for scrollable component click when component is not clickable",
                parentClicked,
            )
        }
    }

    @Test
    fun scrollableComponent_propagatesDoubleTapToParentWhenClickingNonClickableScrollableComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var singleClickCount = 0
        var doubleClickCount = 0
        var lastClickTime = 0L
        val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnClickListener {
                val currentTime = SystemClock.uptimeMillis()
                if (lastClickTime != 0L && currentTime - lastClickTime <= doubleTapTimeout) {
                    doubleClickCount++
                    lastClickTime = 0L
                } else {
                    singleClickCount++
                    lastClickTime = currentTime
                }
            }

            // Double click on the left side (x=75, y=150) -> inside the scrollable box that is not
            // clickable.
            performDoubleClick(parent, 75f, 150f)

            assertEquals("Parent should detect exactly 1 double click", 1, doubleClickCount)
            assertEquals(
                "First tap was registered as a single click before the second tap completed the double click",
                1,
                singleClickCount,
            )
        }
    }

    @Test
    fun scrollableComponent_propagatesLongClickToParentWhenClickingNonClickableScrollableComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            // Long click on the left side (x=75, y=150) -> inside the scrollable box that is not
            // clickable.
            performLongClick(parent, 75f, 150f)

            assertTrue(
                "Parent should receive long click event for scrollable component long click when component is not clickable",
                parentLongClicked,
            )
        }
    }

    @Test
    fun scrollableComponent_propagatesLongClickDuringHoldBeforeTouchUp() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            // Perform only the hold phase (without sending ACTION_UP)
            performLongPressHold(parent, 75f, 150f)

            assertTrue(
                "Parent should receive long click during hold phase before finger is released",
                parentLongClicked,
            )
        }
    }

    @Test
    fun scrollableComponent_releasingLongPressDoesNotTriggerRegularClick() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentClicked = false
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnClickListener { parentClicked = true }
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            performLongClick(parent, 75f, 150f)

            assertTrue("Parent should receive long click event", parentLongClicked)
            assertFalse(
                "Releasing touch after long press should not trigger regular click event",
                parentClicked,
            )
        }
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

    @Test
    fun scrollableComponent_resetsDisallowIntercept_duringLongPressHoldBeforeTouchUp() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        var parentLongClicked = false
        host.setOnLongClickListener {
            parentLongClicked = true
            true
        }

        // 1. Touch down on scrollable component (75, 150)
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertTrue(
            "Disallow intercept should be true initially upon ACTION_DOWN on scrollable component",
            host.disallowIntercept,
        )

        // 2. Advance time past long-press timeout and simulate Choreographer render loop
        ShadowSystemClock.advanceBy(Duration.ofMillis(600))
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))
        ShadowLooper.idleMainLooper()

        assertTrue("Parent should have received long click during hold", parentLongClicked)
        assertFalse(
            "Disallow intercept should be reset to false when long-click is performed during hold phase",
            host.disallowIntercept,
        )

        // 3. Release finger (ACTION_UP)
        val upTime = SystemClock.uptimeMillis()
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, 75f, 150f, 0)
        host.dispatchTouchEvent(upEvent)
        upEvent.recycle()

        assertFalse(
            "Disallow intercept should remain false after releasing touch (ACTION_UP)",
            host.disallowIntercept,
        )
    }

    @Test
    fun scrollableComponent_resetsDisallowIntercept_onSingleClick() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        // 1. Touch down on scrollable component
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertTrue(
            "Disallow intercept should be true on ACTION_DOWN for scrollable component",
            host.disallowIntercept,
        )

        // 2. Touch up without long press or drag
        val upTime = downTime + 50
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, 75f, 150f, 0)
        host.dispatchTouchEvent(upEvent)
        upEvent.recycle()

        assertFalse(
            "Disallow intercept should be reset to false after click (ACTION_UP)",
            host.disallowIntercept,
        )
    }

    @Test
    fun scrollableComponent_resetsDisallowIntercept_onActionCancel() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        // 1. Touch down on scrollable component
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertTrue(
            "Disallow intercept should be true on ACTION_DOWN for scrollable component",
            host.disallowIntercept,
        )

        // 2. Cancel gesture
        val cancelTime = downTime + 50
        val cancelEvent =
            MotionEvent.obtain(downTime, cancelTime, MotionEvent.ACTION_CANCEL, 75f, 150f, 0)
        host.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()

        assertFalse(
            "Disallow intercept should be reset to false after ACTION_CANCEL",
            host.disallowIntercept,
        )
    }

    @Test
    fun scrollableComponent_resetsDisallowIntercept_onDragRelease() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        // 1. Touch down on scrollable component (75, 250)
        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 250f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertTrue(
            "Disallow intercept should be true during touch down on scrollable component",
            host.disallowIntercept,
        )

        // 2. Drag movement
        val moveEvent =
            MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_MOVE, 75f, 200f, 0)
        host.dispatchTouchEvent(moveEvent)
        moveEvent.recycle()

        assertTrue(
            "Disallow intercept should remain true during drag on scrollable component",
            host.disallowIntercept,
        )

        // 3. Release drag gesture (ACTION_UP)
        val upEvent =
            MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, 75f, 200f, 0)
        host.dispatchTouchEvent(upEvent)
        upEvent.recycle()

        assertFalse(
            "Disallow intercept should be reset to false when drag gesture is released",
            host.disallowIntercept,
        )
    }

    private fun setupHostWithPlayer(
        docBytes: ByteArray,
        width: Int = 300,
        height: Int = 300,
    ): Pair<RemoteComposePlayer, HostViewGroup> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val host = HostViewGroup(activity)
        val player = RemoteComposePlayer(activity)
        player.setDocument(docBytes)
        host.addView(player, FrameLayout.LayoutParams(width, height))
        activity.setContentView(host)

        host.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))

        return Pair(player, host)
    }

    private fun setupPlayerInParent(
        docBytes: ByteArray,
        onParentDown: () -> Unit = {},
        onParentTouch: (MotionEvent) -> Unit = {},
        width: Int = 300,
        height: Int = 300,
    ): TestFixture {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        val player = RemoteComposePlayer(activity)
        player.setDocument(docBytes)

        parent.addView(player, FrameLayout.LayoutParams(width, height))
        parent.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onParentDown()
            }
            onParentTouch(event)
            true
        }
        activity.setContentView(parent)

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

    private fun performDoubleClick(view: View, x: Float, y: Float) {
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

        lastEventTime = SystemClock.uptimeMillis()
    }

    private fun performLongPressHold(view: View, x: Float, y: Float, holdTimeMs: Long = 600) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        // Advance system clock past the long-press timeout
        ShadowSystemClock.advanceBy(Duration.ofMillis(holdTimeMs))
        lastEventTime = SystemClock.uptimeMillis()

        // Trigger draw pass during touch hold (simulating Choreographer render loop on live device)
        val bitmap =
            Bitmap.createBitmap(
                view.width.coerceAtLeast(1),
                view.height.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
        view.draw(Canvas(bitmap))
        ShadowLooper.idleMainLooper()
    }

    private fun performLongClick(view: View, x: Float, y: Float) {
        performLongPressHold(view, x, y)
        val upTime = SystemClock.uptimeMillis()
        val downTime = upTime - 600
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0)
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
