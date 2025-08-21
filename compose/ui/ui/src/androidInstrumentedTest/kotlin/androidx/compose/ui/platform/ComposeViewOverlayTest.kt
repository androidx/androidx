/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.TransitionManager
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeViewOverlayTest {
    /**
     * Note: this test does not use the compose rule to ensure default behavior of window-scoped
     * Recomposer installation.
     */
    @get:Rule val rule = activityScenarioRule<ComponentActivity>()

    /**
     * Moving a ComposeView to an [android.view.ViewOverlay] means it won't have a correct parent
     * chain. This happens when user code performs view transitions on the rest of the view
     * hierarchy. Test that ComposeView still works when moved back and forth as long as it was
     * originally attached to the target window "for real."
     */
    @OptIn(InternalComposeUiApi::class)
    @LargeTest
    @Test
    fun testComposeViewMovedToOverlay() {
        var factoryCallCount = 0
        lateinit var createdRecomposer: Recomposer
        WindowRecomposerPolicy.withFactory({ view ->
            factoryCallCount++
            WindowRecomposerFactory.LifecycleAware.createRecomposer(view).also {
                createdRecomposer = it
            }
        }) {
            val expectedText = "Hello, world"
            lateinit var composeView: ComposeView
            lateinit var contentAView: ViewGroup
            lateinit var contentBView: ViewGroup
            var localLifecycleOwner by mutableStateOf<LifecycleOwner?>(null)
            var publishedStage by mutableStateOf(0)
            var consumedStage by mutableStateOf(-1)
            var compositionCount = 0
            rule.scenario.onActivity { activity ->
                composeView =
                    ComposeView(activity).apply {
                        setContent {
                            BasicText(expectedText)
                            localLifecycleOwner = LocalLifecycleOwner.current
                            consumedStage = publishedStage
                            SideEffect { compositionCount++ }
                        }
                    }
                contentAView = FrameLayout(activity).apply { addView(composeView) }
                contentBView = FrameLayout(activity)
                val views =
                    LinearLayout(activity).apply {
                        addView(
                            contentAView,
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
                        )
                        addView(
                            contentBView,
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f),
                        )
                    }
                activity.setContentView(views)
            }

            createdRecomposer.waitForIdle()

            assertNotNull("expected non-null LocalLifecycleOwner", localLifecycleOwner)
            assertEquals("unexpected recomposition result", publishedStage, consumedStage)
            assertTrue("composed at least once", compositionCount > 0)
            val compositionCountAtFirstIdle: Int = compositionCount
            rule.scenario.onActivity {
                contentAView.removeView(composeView)
                contentBView.overlay.add(composeView)
                publishedStage++
                // Send apply notifications right away so that we know it happened
                // before this onActivity block returns back to the test thread.
                // Otherwise the waitForIdleRecomposers below can run before the publishedStage
                // change is picked up by the recomposer.
                Snapshot.sendApplyNotifications()
            }

            createdRecomposer.waitForIdle()

            assertNotNull("overlay expected non-null LocalLifecycleOwner", localLifecycleOwner)
            assertEquals("unexpected recomposition overlay result", publishedStage, consumedStage)
            assertTrue(
                "recomposed at least once since idle",
                compositionCount > compositionCountAtFirstIdle,
            )
        }
        assertEquals("Created recomposer count", 1, factoryCallCount)
    }

    // Regression test for b/287484338
    /**
     * Users can often (unintentionally) try to create a ComposeView inside of a ViewOverlay. We
     * should support this use case.
     */
    @LargeTest
    @Test
    fun testCreateCompositionInViewOverlay() {
        rule.scenario.onActivity { activity ->
            val contentView = FrameLayout(activity)
            activity.setContentView(contentView)

            // Should not crash.
            val composeView = ComposeView(activity)
            ViewCompat.addOverlayView(contentView, composeView)
            composeView.setContent { Text("Inside of an overlay!") }
        }
    }

    // Regression test for b/287484338
    /**
     * Transitions use ViewOverlay under the hood. Repeat [testCreateCompositionInViewOverlay] in
     * this context to form a deeper integration test.
     */
    @LargeTest
    @Test
    fun testCreateCompositionDuringTransition() {
        rule.scenario.onActivity { activity ->
            val transitionedComposeHost = FrameLayout(activity)
            activity.setContentView(transitionedComposeHost)

            val root = transitionedComposeHost.parent as ViewGroup
            val endScene = Scene(root, View(activity))
            TransitionManager.go(endScene, Fade())

            // We need to run this after the transition manager has started its first frame.
            Handler(Looper.getMainLooper()).post {
                transitionedComposeHost.assertInOverlay()
                val composeView = ComposeView(activity)
                // Should not crash.
                ViewCompat.addOverlayView(transitionedComposeHost, composeView)
                composeView.setContent { Text("Inside of an overlay!") }
                composeView.assertInOverlay()
            }
        }
    }

    private fun View.assertInOverlay() {
        var root = this as ViewParent
        while (root.parent != null) root = root.parent

        assertEquals(
            "Expected $this to be in a view overlay, but didn't have the expected root view type",
            // Hardcoded internal class name from the platform. This name is consistent since the
            // inception of view overlays in SDK level 18, but may require an update if a future
            // change in the platform occurs.
            "android.view.ViewOverlay.OverlayViewGroup",
            root.javaClass.canonicalName,
        )
    }

    private fun Recomposer.waitForIdle() = runBlocking {
        currentState.filter { it == Recomposer.State.Idle }.first()
    }
}
