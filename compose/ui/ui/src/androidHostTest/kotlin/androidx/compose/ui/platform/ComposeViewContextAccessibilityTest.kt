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

package androidx.compose.ui.platform

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ComposeViewContextAccessibilityTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun SimpleText(text: String, modifier: Modifier = Modifier) {
        Layout(content = {}, modifier = modifier.semantics { contentDescription = text }) { _, _ ->
            layout(10, 10) {}
        }
    }

    private fun assertTalkBackCanFocusNode(
        androidComposeView: AndroidComposeView,
        expectedDescription: String,
    ) {
        val nodeId =
            androidComposeView.semanticsOwner.unmergedRootSemanticsNode.replacedChildren
                .first {
                    it.config
                        .getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                        .contains(expectedDescription)
                }
                .id
        val provider = checkNotNull(androidComposeView.accessibilityNodeProvider)
        val info = checkNotNull(provider.createAccessibilityNodeInfo(nodeId))
        assertThat(info.contentDescription?.toString()).isEqualTo(expectedDescription)

        // Clear any prior focus first so ACTION_ACCESSIBILITY_FOCUS exercises
        // requestAccessibilityFocus.
        provider.performAction(
            nodeId,
            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
            null,
        )
        val focused =
            provider.performAction(
                nodeId,
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                null,
            )
        assertThat(focused).isTrue()
        val focusedInfo = checkNotNull(provider.createAccessibilityNodeInfo(nodeId))
        assertThat(focusedInfo.isAccessibilityFocused).isTrue()
    }

    private fun assertTalkBackCannotFocusNode(
        androidComposeView: AndroidComposeView,
        expectedDescription: String,
    ) {
        val nodeId =
            androidComposeView.semanticsOwner.unmergedRootSemanticsNode.replacedChildren
                .first {
                    it.config
                        .getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                        .contains(expectedDescription)
                }
                .id
        val provider = checkNotNull(androidComposeView.accessibilityNodeProvider)
        provider.performAction(
            nodeId,
            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
            null,
        )
        val focused =
            provider.performAction(
                nodeId,
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                null,
            )
        assertThat(focused).isFalse()
    }

    @Test
    fun testDetachedComposeView_setContentAndMeasuredWhileDetached_registersListenersOnAttach() {
        val activity = rule.activity
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setEnabled(false)
        shadowAm.setTouchExplorationEnabled(false)

        val dummyView = View(activity)
        val rootLayout =
            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
            }
        rootLayout.addView(dummyView)
        val recomposer = dummyView.windowRecomposer

        val composeView = ComposeView(activity)
        composeView.setParentCompositionContext(recomposer)
        composeView.setViewTreeLifecycleOwner(activity)
        composeView.setViewTreeSavedStateRegistryOwner(activity)

        assertThat(composeView.isAttachedToWindow).isFalse()

        // Set content while detached and measure to force composition creation prior to attach.
        composeView.setContent { SimpleText("Detached text", Modifier.testTag("detachedNode")) }
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)

        val androidComposeView = composeView.getChildAt(0) as AndroidComposeView
        assertThat(androidComposeView.composeViewContext.isAccessibilityEnabled).isFalse()

        // Enable TalkBack before attaching to verify synchronous first-frame state on attach
        // (before the posted ComposeViewContextCallback runnable executes), as well as listener
        // registration after Looper.idle().
        shadowAm.setEnabled(true)
        shadowAm.setTouchExplorationEnabled(true)

        rootLayout.addView(composeView)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)
        assertThat(androidComposeView.composeViewContext.isAccessibilityEnabled).isTrue()
        assertThat(androidComposeView.composeViewContext.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView, "Detached text")

        // Idle the main looper so the posted callback registers AccessibilityManager listeners.
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Toggling accessibility & touch exploration off and back on dynamically must update
        // ComposeViewContext and gate TalkBack ACTION_ACCESSIBILITY_FOCUS.
        shadowAm.setTouchExplorationEnabled(false)
        assertThat(androidComposeView.composeViewContext.isTouchExplorationEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView, "Detached text")

        shadowAm.setTouchExplorationEnabled(true)
        assertThat(androidComposeView.composeViewContext.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView, "Detached text")
    }

    @Test
    fun testDetachedComposeView_prewarmWithComposeViewContext_attachesAndFocusesInTalkBack() {
        val activity = rule.activity
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setEnabled(true)
        shadowAm.setTouchExplorationEnabled(true)

        val rootLayout =
            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
            }
        val prewarmContext = ComposeViewContext(rootLayout)

        val composeView = ComposeView(activity)
        composeView.setContent { SimpleText("Prewarm text") }
        assertThat(composeView.isAttachedToWindow).isFalse()

        // Exercise AbstractComposeView.createComposition(composeViewContext) while detached, which
        // sets AbstractComposeView.composeViewContext and calls incrementViewCount() during init.
        composeView.createComposition(prewarmContext)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)

        val androidComposeView = composeView.getChildAt(0) as AndroidComposeView
        assertThat(androidComposeView.composeViewContext).isSameInstanceAs(prewarmContext)
        assertThat(prewarmContext.viewCount).isEqualTo(1)
        assertThat(prewarmContext.isAccessibilityEnabled).isTrue()
        assertThat(prewarmContext.isTouchExplorationEnabled).isTrue()

        // Attach the prewarmed ComposeView to the hierarchy; viewCount must remain 1 (not double
        // incremented) and TalkBack must be able to focus the node immediately.
        rootLayout.addView(composeView)
        assertThat(prewarmContext.viewCount).isEqualTo(1)
        assertTalkBackCanFocusNode(androidComposeView, "Prewarm text")

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        shadowAm.setTouchExplorationEnabled(false)
        assertThat(prewarmContext.isTouchExplorationEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView, "Prewarm text")

        shadowAm.setTouchExplorationEnabled(true)
        assertThat(prewarmContext.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView, "Prewarm text")
    }

    @Test
    fun testComposeView_firstFrameAccessibilityEnabled_beforeHandlerCallbackRuns() {
        val activity = rule.activity
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setEnabled(true)
        shadowAm.setTouchExplorationEnabled(true)

        val rootLayout =
            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
            }
        val composeView = ComposeView(activity)

        // Set content while detached.
        composeView.setContent { SimpleText("First frame text") }
        assertThat(composeView.isAttachedToWindow).isFalse()

        // Attach to window WITHOUT idling the main looper yet (simulating first-frame query
        // before the posted ComposeViewContextCallback runnable executes).
        rootLayout.addView(composeView)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)

        val androidComposeView = composeView.getChildAt(0) as AndroidComposeView
        assertThat(androidComposeView.composeViewContext.isAccessibilityEnabled).isTrue()
        assertThat(androidComposeView.composeViewContext.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView, "First frame text")

        // Idle the main looper and verify dynamic state changes also work afterward.
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        shadowAm.setTouchExplorationEnabled(false)
        assertThat(androidComposeView.composeViewContext.isTouchExplorationEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView, "First frame text")
    }

    @Test
    fun testComposeView_detachAndReattach_refreshesStateAndReregistersListeners() {
        val activity = rule.activity
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setEnabled(false)
        shadowAm.setTouchExplorationEnabled(false)

        val rootLayout =
            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
            }
        val composeView = ComposeView(activity)
        composeView.setContent { SimpleText("Reattach text") }

        // 1. Attach initially with accessibility disabled.
        rootLayout.addView(composeView)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val androidComposeView = composeView.getChildAt(0) as AndroidComposeView
        val context = androidComposeView.composeViewContext
        assertThat(context.viewCount).isEqualTo(1)
        assertThat(context.isAccessibilityEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView, "Reattach text")

        // 2. Detach from window and idle looper so listeners are unregistered.
        rootLayout.removeView(composeView)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(context.viewCount).isEqualTo(0)

        // 3. Enable accessibility while the ComposeView is detached.
        shadowAm.setEnabled(true)
        shadowAm.setTouchExplorationEnabled(true)

        // 4. Re-attach the ComposeView: it must synchronously pick up the enabled state, allow
        // TalkBack focus on the first frame, and re-register listeners for subsequent changes.
        rootLayout.addView(composeView)
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, 100, 100)
        assertThat(context.viewCount).isEqualTo(1)
        assertThat(context.isAccessibilityEnabled).isTrue()
        assertThat(context.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView, "Reattach text")

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        shadowAm.setTouchExplorationEnabled(false)
        assertThat(context.isTouchExplorationEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView, "Reattach text")
    }

    @Test
    fun testSharedComposeViewContext_multipleComposeViews_keepsObservingUntilLastDetached() {
        val activity = rule.activity
        val am = activity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setEnabled(true)
        shadowAm.setTouchExplorationEnabled(true)

        val rootLayout =
            activity.findViewById<ViewGroup>(android.R.id.content).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
            }

        val composeView1 = ComposeView(activity).apply { setContent { SimpleText("View 1") } }
        val composeView2 = ComposeView(activity).apply { setContent { SimpleText("View 2") } }

        rootLayout.addView(composeView1)
        rootLayout.addView(composeView2)
        composeView1.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView1.layout(0, 0, 100, 100)
        composeView2.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        composeView2.layout(0, 0, 100, 100)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val androidComposeView1 = composeView1.getChildAt(0) as AndroidComposeView
        val androidComposeView2 = composeView2.getChildAt(0) as AndroidComposeView
        val sharedContext = androidComposeView1.composeViewContext
        assertThat(androidComposeView2.composeViewContext).isSameInstanceAs(sharedContext)
        assertThat(sharedContext.viewCount).isEqualTo(2)
        assertTalkBackCanFocusNode(androidComposeView2, "View 2")

        // Detach the first ComposeView; sharedContext still has viewCount == 1 so it must keep
        // observing AccessibilityManager changes for composeView2.
        rootLayout.removeView(composeView1)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(sharedContext.viewCount).isEqualTo(1)

        shadowAm.setTouchExplorationEnabled(false)
        assertThat(sharedContext.isTouchExplorationEnabled).isFalse()
        assertTalkBackCannotFocusNode(androidComposeView2, "View 2")
        shadowAm.setTouchExplorationEnabled(true)
        assertThat(sharedContext.isTouchExplorationEnabled).isTrue()
        assertTalkBackCanFocusNode(androidComposeView2, "View 2")

        // Detach the second ComposeView; viewCount drops to 0.
        rootLayout.removeView(composeView2)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(sharedContext.viewCount).isEqualTo(0)
    }
}
