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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import kotlin.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/**
 * Tests verifying that [RemoteComposePlayer] behaves as expected when
 * [Header.FEATURE_DISALLOW_INTERCEPT_TOUCH] is disabled via document header tags (pre-fix behavior
 * where touch interception is not disallowed and unhandled clicks/long-clicks are not forwarded to
 * host parent).
 */
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class RemoteComposePlayerDisallowInterceptTouchDisabledTest {

    private var lastEventTime = SystemClock.uptimeMillis()

    @Test
    fun scrollableComponent_doesNotDisallowParentIntercept_soDragIsInterceptedByHost() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertFalse(
            "Disallow intercept should NOT be called on ACTION_DOWN when gesture propagation is disabled",
            host.disallowIntercept,
        )

        // Swipe inside the scrollable left box (75, 250 -> 75, 50)
        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Disallow intercept should NOT be called during drag when gesture propagation is disabled",
            host.disallowIntercept,
        )
        assertTrue(
            "Host parent should intercept drag gesture when gesture propagation is disabled",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun clickableWithScrollableComponent_doesNotDisallowParentIntercept_soDragIsInterceptedByHost() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = true, isScrollable = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertFalse(
            "Disallow intercept should NOT be called on ACTION_DOWN when gesture propagation is disabled",
            host.disallowIntercept,
        )

        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Disallow intercept should NOT be called during drag when gesture propagation is disabled",
            host.disallowIntercept,
        )
        assertTrue(
            "Host parent should intercept drag gesture when gesture propagation is disabled",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun clickableWithTouchExpression_doesNotDisallowParentIntercept_soDragIsInterceptedByHost() {
        val docBytes =
            createLeftBoxInteractiveDocument(isClickable = true, hasTouchExpression = true)
        val (_, host) = setupHostWithPlayer(docBytes)

        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertFalse(
            "Disallow intercept should NOT be called on ACTION_DOWN when gesture propagation is disabled",
            host.disallowIntercept,
        )

        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Disallow intercept should NOT be called during drag when gesture propagation is disabled",
            host.disallowIntercept,
        )
        assertTrue(
            "Host parent should intercept drag gesture when gesture propagation is disabled",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun unhandledClick_doesNotPropagateToParent_onScrollableComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnClickListener { parentClicked = true }

            performClick(parent, 75f, 150f)

            assertFalse(
                "Parent should not receive click event when gesture propagation is disabled",
                parentClicked,
            )
        }
    }

    @Test
    fun unhandledClick_doesNotPropagateToParent_onTouchExpressionComponent() {
        val docBytes =
            createLeftBoxInteractiveDocument(isClickable = false, hasTouchExpression = true)
        var parentClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnClickListener { parentClicked = true }

            performClick(parent, 75f, 150f)

            assertFalse(
                "Parent should not receive click event when gesture propagation is disabled",
                parentClicked,
            )
        }
    }

    @Test
    fun unhandledLongClick_doesNotPropagateToParent_onScrollableComponent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = true)
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            performLongClick(parent, 75f, 150f)

            assertFalse(
                "Parent should not receive long click event when gesture propagation is disabled",
                parentLongClicked,
            )
        }
    }

    @Test
    fun unhandledLongClick_doesNotPropagateToParent_onTouchExpressionComponent() {
        val docBytes =
            createLeftBoxInteractiveDocument(isClickable = false, hasTouchExpression = true)
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (_, parent) ->
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            performLongClick(parent, 75f, 150f)

            assertFalse(
                "Parent should not receive long click event when gesture propagation is disabled",
                parentLongClicked,
            )
        }
    }

    @Test
    fun player_performClick_doesNotPropagateToParent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = false)
        var parentClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (player, parent) ->
            parent.setOnClickListener { parentClicked = true }

            player.performClick()

            assertFalse(
                "Parent should not receive click via player.performClick() when gesture propagation is disabled",
                parentClicked,
            )
        }
    }

    @Test
    fun player_performLongClick_doesNotPropagateToParent() {
        val docBytes = createLeftBoxInteractiveDocument(isClickable = false, isScrollable = false)
        var parentLongClicked = false

        setupPlayerInParent(docBytes = docBytes).use { (player, parent) ->
            parent.setOnLongClickListener {
                parentLongClicked = true
                true
            }

            player.performLongClick()

            assertFalse(
                "Parent should not receive long click via player.performLongClick() when gesture propagation is disabled",
                parentLongClicked,
            )
        }
    }

    @Test
    fun handledClick_triggersRemoteComposeAction_withoutCallingRequestDisallowIntercept() {
        val (docBytes, actionTextId) =
            createLeftBoxInteractiveDocumentWithActionId(isClickable = true, isScrollable = true)
        var clickCount = 0

        val (player, host) = setupHostWithPlayer(docBytes = docBytes)
        player.addIdActionListener { id, _ ->
            if (id == actionTextId) {
                clickCount++
            }
        }

        performClick(host, 75f, 150f)

        assertEquals(
            "Internal RemoteCompose action should still be triggered when gesture propagation is disabled",
            1,
            clickCount,
        )
        assertFalse(
            "requestDisallowInterceptTouchEvent should NOT be called during click when gesture propagation is disabled",
            host.disallowInterceptCalled,
        )
    }

    @Test
    fun documentHeaderTag_explicitlyDisabling_overridesDefaultEnabled() {
        val docBytes =
            createLeftBoxInteractiveDocument(
                isClickable = false,
                isScrollable = true,
                gesturePropagation = 0,
            )
        val (_, host) = setupHostWithPlayer(docBytes)

        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertFalse(
            "Disallow intercept should NOT be called when document explicitly sets gesturePropagation=0",
            host.disallowIntercept,
        )

        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Disallow intercept should NOT be called during drag when document sets gesturePropagation=0",
            host.disallowIntercept,
        )
        assertTrue(
            "Host parent should intercept drag gesture when document sets gesturePropagation=0",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun documentHeaderTag_explicitlyEnabling_enablesGesturePropagation() {
        val docBytes =
            createLeftBoxInteractiveDocument(
                isClickable = false,
                isScrollable = true,
                gesturePropagation = 1,
            )
        val (_, host) = setupHostWithPlayer(docBytes)

        val downTime = SystemClock.uptimeMillis()
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 75f, 150f, 0)
        host.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        assertTrue(
            "Disallow intercept should be true when document explicitly sets gesturePropagation=1",
            host.disallowIntercept,
        )

        performSwipe(host, 75f, 250f, 75f, 50f)

        assertFalse(
            "Host parent should not intercept drag when document explicitly sets gesturePropagation=1",
            host.hostInterceptedDrag,
        )
    }

    @Test
    fun ancestorScroller_doesNotMaintainDisallowInterceptOnScrollerParent() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val rootParent = HostViewGroup(activity)
        val scroller = HorizontalScrollView(activity)
        val linearLayout = LinearLayout(activity)
        val player = RemoteComposePlayer(activity)
        val docBytes = createLeftBoxInteractiveDocument(gesturePropagation = 0)
        player.setDocument(docBytes)

        linearLayout.addView(player, FrameLayout.LayoutParams(900, 300))
        scroller.addView(linearLayout, FrameLayout.LayoutParams(900, 300))
        rootParent.addView(scroller, FrameLayout.LayoutParams(300, 300))
        activity.setContentView(rootParent)

        rootParent.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
        )
        rootParent.layout(0, 0, 300, 300)

        rootParent.disallowIntercept = false

        // In disabled mode, player.requestDisallowInterceptTouchEvent(false) does not walk up
        // ancestors
        player.requestDisallowInterceptTouchEvent(false)

        assertFalse(
            "Root parent should not have disallowIntercept set when gesture propagation is disabled",
            rootParent.disallowIntercept,
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
        var disallowInterceptCalled = false
        var hostInterceptedDrag = false

        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            this.disallowIntercept = disallowIntercept
            this.disallowInterceptCalled = true
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            if (ev.action == MotionEvent.ACTION_DOWN) {
                disallowIntercept = false
                disallowInterceptCalled = false
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

    private fun performLongPressHold(view: View, x: Float, y: Float, holdTimeMs: Long = 600) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        ShadowSystemClock.advanceBy(Duration.ofMillis(holdTimeMs))
        lastEventTime = SystemClock.uptimeMillis()

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
            hasTouchExpression: Boolean = false,
            gesturePropagation: Int? = 0,
        ): ByteArray {
            return createLeftBoxInteractiveDocumentWithActionId(
                    isClickable,
                    isScrollable,
                    isTouchUp,
                    hasTouchExpression,
                    gesturePropagation,
                )
                .first
        }

        private fun createLeftBoxInteractiveDocumentWithActionId(
            isClickable: Boolean = false,
            isScrollable: Boolean = false,
            isTouchUp: Boolean = false,
            hasTouchExpression: Boolean = false,
            gesturePropagation: Int? = 0,
        ): Pair<ByteArray, Int> {
            val rcDoc =
                if (gesturePropagation != null) {
                    RemoteComposeWriter(
                        RcPlatformProfiles.ANDROIDX,
                        RemoteComposeWriter.hTag(Header.DOC_WIDTH, 300),
                        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 300),
                        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Test"),
                        RemoteComposeWriter.hTag(
                            Header.FEATURE_DISALLOW_INTERCEPT_TOUCH,
                            gesturePropagation,
                        ),
                    )
                } else {
                    RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)
                }
            val actionTextId = rcDoc.addText("myActionName")
            val scrollPositionId = rcDoc.addNamedFloat("scrollPosition", 0f)
            rcDoc.root {
                rcDoc.row(RecordingModifier().fillMaxSize(), RowLayout.START, RowLayout.CENTER) {
                    var leftModifier = RecordingModifier().width(150f).fillMaxHeight()
                    if (isTouchUp) {
                        leftModifier =
                            leftModifier
                                .onTouchDown(HostAction(0))
                                .onTouchUp(HostAction(actionTextId))
                    }
                    if (isClickable) {
                        leftModifier = leftModifier.onClick(HostAction(actionTextId))
                    }
                    if (isScrollable) {
                        leftModifier = leftModifier.verticalScroll(scrollPositionId)
                    }
                    rcDoc.box(leftModifier, BoxLayout.CENTER, BoxLayout.CENTER) {
                        if (hasTouchExpression) {
                            rcDoc.addTouch(
                                0f,
                                0f,
                                100f,
                                TouchExpression.STOP_GENTLY,
                                0f,
                                0,
                                null,
                                null,
                                RemoteContext.FLOAT_TOUCH_POS_Y,
                                1f,
                                Rc.FloatExpression.MUL,
                            )
                        }
                    }
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
