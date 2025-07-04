/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.slidingpanelayout.widget

import android.content.Context
import android.os.Build
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.accessibility.AccessibilityNodeProvider.HOST_VIEW_ID
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.slidingpanelayout.widget.SlidingPaneLayout.Companion.SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_LEFT
import androidx.slidingpanelayout.widget.SlidingPaneLayout.Companion.SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_RIGHT
import androidx.slidingpanelayout.widget.SlidingPaneLayout.Companion.SPLIT_DIVIDER_POSITION_AUTO
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class SlidingPaneLayoutA11yTest {
    @Test
    fun testCreateAccessibilityNodeInfo_notResizeable_doesNotPopulateDividerNodeInfo() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = false
        }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(HOST_VIEW_ID)!!
        assertThat(node.childCount).isEqualTo(2)
    }

    @Test
    fun testCreateAccessibilityNodeInfo_resizeable_doesPopulateDividerNodeInfo() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
        }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(HOST_VIEW_ID)!!
        assertThat(node.childCount).isEqualTo(3)
    }

    @Test
    fun testDividerNodeInfoActions_defaults() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
        }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)
        assertNode(node).containsAction(AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)
        // The divider node only supports scroll actions when it's a11y focused.
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_LEFT)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_RIGHT)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)

        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_CLICK)

        assertThat(node!!.className).isEqualTo(Button::class.java.name)
        assertThat(node.isFocusable).isTrue()
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtLeftEdge_doesNotContainScrollLeft() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 0f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_LEFT)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_RIGHT)
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtRightEdge_doesNotContainScrollRight() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 1f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_LEFT)
        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_RIGHT)
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtLeftEdgeLTR_doesNotContainScrollBackward() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 0f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtLeftEdgeRtl_doesNotContainScrollForward() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 0f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtRightEdgeLTR_doesNotContainScrollForward() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 1f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun testDividerNodeInfoActions_dividerAtRightEdgeRtl_doesNotContainScrollBackward() {
        val spl =
            createSlidingPaneLayout(dividerPosition = 1f) {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).containsAction(AccessibilityAction.ACTION_SCROLL_BACKWARD)
        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_SCROLL_FORWARD)
    }

    @Test
    fun testDividerNodeInfoActions_dividerIsClickable_containClick() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
            setOnUserResizingDividerClickListener {}
        }

        val node =
            spl.accessibilityDelegate
                .getAccessibilityNodeProvider(spl)
                ?.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)

        assertNode(node).containsAction(AccessibilityAction.ACTION_CLICK)
    }

    @Test
    fun testPerformActionOnDivider_performFocus_hasAccessibilityFocus() {
        val (spl, parent) =
            createSlidingPaneLayoutWithParent {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
            }

        val provider = spl.accessibilityDelegate.getAccessibilityNodeProvider(spl)!!
        spl.isAccessibilityEnabledForTesting = true
        // Request Accessibility focus for divider
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
            null,
        )

        // Verify that it send accessibility TYPE_VIEW_ACCESSIBILITY_FOCUSED event
        assertThat(parent.accessibilityEvents[0]!!.eventType)
            .isEqualTo(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)

        val node = provider.createAccessibilityNodeInfo(DIVIDER_VIRTUAL_VIEW_ID)!!

        // Divider is focused, it now supports ACTION_CLEAR_ACCESSIBILITY_FOCUS.
        assertNode(node).containsAction(AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
        assertNode(node).doesNotContainsAction(AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)
    }

    @Test
    fun testPerformActionOnDivider_performClick_doesClick() {
        var clicked = false
        val (spl, parent) =
            createSlidingPaneLayoutWithParent {
                isOverlappingEnabled = false
                isUserResizingEnabled = true
                setOnUserResizingDividerClickListener { clicked = true }
            }

        // Gut check, make sure OnClickListener is not triggered during initialization.
        assertThat(clicked).isFalse()
        spl.isAccessibilityEnabledForTesting = true
        val provider = spl.accessibilityDelegate.getAccessibilityNodeProvider(spl)!!

        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            AccessibilityNodeInfoCompat.ACTION_CLICK,
            null,
        )

        assertThat(clicked).isTrue()

        assertThat(parent.accessibilityEvents[0]!!.eventType)
            .isEqualTo(AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    @Test
    fun testPerformActionOnDivider_performScrollLeft_moveLeft() {
        testPerformScrollAction(
            android.R.id.accessibilityActionScrollLeft,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_LEFT,
        )
    }

    @Test
    fun testPerformActionOnDivider_performScrollRight_moveRight() {
        testPerformScrollAction(
            android.R.id.accessibilityActionScrollRight,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_RIGHT,
        )
    }

    @Test
    fun testPerformActionOnDivider_performScrollForwardLtr_moveRight() {
        testPerformScrollAction(
            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_RIGHT,
        )
    }

    @Test
    fun testPerformActionOnDivider_performScrollForwardRtl_moveLeft() {
        testPerformScrollAction(
            AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_LEFT,
            View.LAYOUT_DIRECTION_RTL,
        )
    }

    @Test
    fun testPerformActionOnDivider_performScrollBackwardLtr_moveLeft() {
        testPerformScrollAction(
            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_LEFT,
        )
    }

    @Test
    fun testPerformActionOnDivider_performScrollBackwardRtl_moveRight() {
        testPerformScrollAction(
            AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD,
            SPLIT_DIVIDER_ACCESSIBILITY_RESIZE_RIGHT,
            View.LAYOUT_DIRECTION_RTL,
        )
    }

    @Test
    fun testPerformActionOnDivider_defaultBehavior_movesBetweenLeftAutoAndRight() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
            this.layoutDirection = layoutDirection
        }
        val provider = spl.accessibilityDelegate.getAccessibilityNodeProvider(spl)!!
        // The divider won't scroll unless it's focused.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
            null,
        )

        // By default the splitDividerPosition is auto.
        assertThat(spl.splitDividerPosition).isEqualTo(SPLIT_DIVIDER_POSITION_AUTO)

        // Scroll left from auto, divider is moved to left edge.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollLeft,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(0)

        // Scroll right from left edge, divider is set to auto.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollRight,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(SPLIT_DIVIDER_POSITION_AUTO)

        // Scroll right from auto, divider is moved to right edge.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollRight,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(spl.width)

        // Scroll left from right edge, divider is set to auto.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollLeft,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(SPLIT_DIVIDER_POSITION_AUTO)
    }

    @Test
    fun testPerformActionOnDivider_defaultBehaviorWithDeveloperSetPosition_moveToEdge() {
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
            this.layoutDirection = layoutDirection
        }
        val provider = spl.accessibilityDelegate.getAccessibilityNodeProvider(spl)!!
        // The divider won't scroll unless it's focused.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
            null,
        )

        val dividerPosition = spl.width / 2
        spl.splitDividerPosition = dividerPosition
        // Gut check.
        assertThat(spl.splitDividerPosition).isEqualTo(dividerPosition)

        // Scroll left from developer set position, divider is moved to left edge.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollLeft,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(0)

        spl.splitDividerPosition = dividerPosition
        // Gut check.
        assertThat(spl.splitDividerPosition).isEqualTo(dividerPosition)

        // Scroll right from developer set position, divider is set to right edge.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            android.R.id.accessibilityActionScrollRight,
            null,
        )
        assertThat(spl.splitDividerPosition).isEqualTo(spl.width)
    }

    @Test
    fun testAccessibilityEvent_sendWindowContentChanged_afterLayout() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val parent = TestViewParent(context)
        // The accessibility event sending is postDelayed.
        // This is hack to cancel the delay so that we can easily test it.
        val spl =
            object : SlidingPaneLayout(context) {
                override fun postDelayed(action: Runnable?, delayMillis: Long): Boolean {
                    action?.run()
                    return true
                }
            }
        spl.isAccessibilityEnabledForTesting = true
        spl.isOverlappingEnabled = false
        spl.isUserResizingEnabled = true

        val listWidth = 30
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(listWidth, MATCH_PARENT))
        }
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        parent.addView(spl)

        spl.measure(
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
        )

        spl.layout(0, 0, spl.measuredWidth, spl.measuredHeight)

        // It's expected to send only 1 event, that's window content changed.
        assertThat(parent.accessibilityEvents.size).isEqualTo(1)
        assertThat(parent.accessibilityEvents[0]!!.eventType)
            .isEqualTo(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    @Test
    fun testAccessibilityEvent_sendAnnouncement_whenDividerAtLeftEdge() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val parent = TestViewParent(context)
        // The accessibility event sending is postDelayed.
        // This is hack to cancel the delay so that we can easily test it.
        val spl =
            object : SlidingPaneLayout(context) {
                override fun postDelayed(action: Runnable?, delayMillis: Long): Boolean {
                    action?.run()
                    return true
                }
            }
        spl.isAccessibilityEnabledForTesting = true
        spl.isOverlappingEnabled = false
        spl.isUserResizingEnabled = true
        spl.splitDividerPosition = 0

        val listWidth = 30
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(listWidth, MATCH_PARENT))
        }
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        parent.addView(spl)

        spl.measure(
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
        )

        spl.layout(0, 0, spl.measuredWidth, spl.measuredHeight)

        assertThat(
                parent.accessibilityEvents.any {
                    it!!.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT
                }
            )
            .isTrue()
        assertThat(
                parent.accessibilityEvents.any {
                    it!!.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                }
            )
            .isTrue()
    }

    @Test
    fun testAccessibilityEvent_sendAnnouncement_whenDividerAtRightEdge() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val parent = TestViewParent(context)
        // The accessibility event sending is postDelayed.
        // This is hack to cancel the delay so that we can easily test it.
        val spl =
            object : SlidingPaneLayout(context) {
                override fun postDelayed(action: Runnable?, delayMillis: Long): Boolean {
                    action?.run()
                    return true
                }
            }
        spl.isAccessibilityEnabledForTesting = true
        spl.isOverlappingEnabled = false
        spl.isUserResizingEnabled = true
        spl.splitDividerPosition = 100

        val listWidth = 30
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(listWidth, MATCH_PARENT))
        }
        View(context).also {
            spl.addView(it, SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }

        parent.addView(spl)

        spl.measure(
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
        )

        spl.layout(0, 0, spl.measuredWidth, spl.measuredHeight)

        assertThat(
                parent.accessibilityEvents.any {
                    it!!.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT
                }
            )
            .isTrue()
        assertThat(
                parent.accessibilityEvents.any {
                    it!!.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                }
            )
            .isTrue()
    }

    private fun testPerformScrollAction(
        action: Int,
        expectDirection: Int,
        layoutDirection: Int = View.LAYOUT_DIRECTION_LTR,
    ) {
        val userResizeBehavior = TestUserResizeBehavior()
        val spl = createSlidingPaneLayout {
            isOverlappingEnabled = false
            isUserResizingEnabled = true
            this.layoutDirection = layoutDirection
            setUserResizeBehavior(userResizeBehavior)
        }

        val provider = spl.accessibilityDelegate.getAccessibilityNodeProvider(spl)!!

        // Focus the divider first so that it can be scrolled.
        provider.performAction(
            DIVIDER_VIRTUAL_VIEW_ID,
            AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
            null,
        )
        provider.performAction(DIVIDER_VIRTUAL_VIEW_ID, action, null)
        userResizeBehavior.expectCall("onAccessibilityResize", expectDirection)
        userResizeBehavior.expectNoMoreCall()
    }
}

private fun createSlidingPaneLayout(
    dividerPosition: Float = 0.3f,
    initialization: SlidingPaneLayout.() -> Unit,
): SlidingPaneLayout {
    return createSlidingPaneLayoutWithParent(dividerPosition, initialization).first
}

private fun createSlidingPaneLayoutWithParent(
    dividerPosition: Float = 0.3f,
    initialization: SlidingPaneLayout.() -> Unit,
): Pair<SlidingPaneLayout, TestViewParent> {
    val context = InstrumentationRegistry.getInstrumentation().context
    val parent = TestViewParent(context)
    val spl = SlidingPaneLayout(context)
    initialization.invoke(spl)

    val width = 100
    val listWidth = (width * dividerPosition).toInt()

    View(context).also { spl.addView(it, SlidingPaneLayout.LayoutParams(listWidth, MATCH_PARENT)) }
    View(context).also {
        spl.addView(it, SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }

    parent.addView(spl)

    spl.measure(
        MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
    )

    spl.layout(0, 0, spl.measuredWidth, spl.measuredHeight)

    return Pair(spl, parent)
}

private class TestViewParent(context: Context) : FrameLayout(context) {
    val accessibilityEvents = mutableListOf<AccessibilityEvent?>()

    override fun requestSendAccessibilityEvent(child: View, event: AccessibilityEvent?): Boolean {
        accessibilityEvents.add(event)
        return true
    }
}

private class TestUserResizeBehavior : SlidingPaneLayout.UserResizeBehavior {
    private val calls = mutableListOf<Pair<String, Any>>()

    fun expectCall(name: String, parameter: Any) {
        assertThat(calls.first()).isEqualTo(Pair(name, parameter))
        calls.removeAt(0)
    }

    fun expectNoMoreCall() {
        assertThat(calls).isEmpty()
    }

    override fun onUserResizeStarted(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int) {
        calls.add(Pair("onUserResizeStarted", dividerPositionX))
    }

    override fun onUserResizeProgress(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int) {
        calls.add(Pair("onUserResizeProgress", dividerPositionX))
    }

    override fun onUserResizeComplete(slidingPaneLayout: SlidingPaneLayout, dividerPositionX: Int) {
        calls.add(Pair("onUserResizeComplete", dividerPositionX))
    }

    override fun onUserResizeCancelled(
        slidingPaneLayout: SlidingPaneLayout,
        dividerPositionX: Int,
    ) {
        calls.add(Pair("onUserResizeCancelled", dividerPositionX))
    }

    override fun onAccessibilityResize(slidingPaneLayout: SlidingPaneLayout, direction: Int) {
        calls.add(Pair("onAccessibilityResize", direction))
    }
}

private const val DIVIDER_VIRTUAL_VIEW_ID = 0

private fun assertNode(actual: AccessibilityNodeInfo?): AccessibilityNodeSubject {
    return assertAbout(AccessibilityNodeSubject.SUBJECT_FACTORY).that(actual)!!
}

private class AccessibilityNodeSubject
private constructor(metadata: FailureMetadata, private val actual: AccessibilityNodeInfo?) :
    Subject(metadata, actual) {
    companion object {
        internal val SUBJECT_FACTORY: Factory<AccessibilityNodeSubject?, AccessibilityNodeInfo?> =
            Factory { failureMetadata, subject ->
                AccessibilityNodeSubject(failureMetadata, subject)
            }
    }

    fun containsAction(action: AccessibilityAction) {
        assertWithMessage("is not null").that(actual).isNotNull()
        assertWithMessage("contains $action").that(actual!!.actionList).contains(action)
    }

    fun doesNotContainsAction(action: AccessibilityAction) {
        assertWithMessage("doesn't contain $action").that(actual?.actionList).doesNotContain(action)
    }
}
