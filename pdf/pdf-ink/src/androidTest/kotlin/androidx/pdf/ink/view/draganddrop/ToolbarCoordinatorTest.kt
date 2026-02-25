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

package androidx.pdf.ink.view.draganddrop

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.pdf.PdfTestActivity
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import androidx.pdf.util.ToolbarViewActions
import androidx.pdf.util.ToolbarViewActions.performDragAndDrop
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolbarCoordinatorTest {

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var coordinator: ToolbarCoordinator
    private lateinit var toolbar: AnnotationToolbar

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity ->
            coordinator =
                ToolbarCoordinator(activity).apply {
                    id = COORDINATOR_VIEW_ID
                    areAnimationsEnabled = false
                }
            toolbar =
                AnnotationToolbar(activity).apply {
                    id = TOOLBAR_VIEW_ID
                    areAnimationsEnabled = false
                }
            // Use FrameLayout.LayoutParams since coordinator is added to a FrameLayout container
            activity.container.addView(
                coordinator,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            coordinator.attachToolbar(toolbar)
        }
        instrumentation.waitForIdleSync()
    }

    @After
    fun tearDown() {
        activityRule.scenario.onActivity { activity -> activity.container.removeAllViews() }
        PdfTestActivity.onCreateCallback = {}
    }

    @Test
    fun attachToolbar_setsInitialStateOnTabAndPhone() {
        onIdle()
        val screenWidthDp = instrumentation.context.resources.configuration.smallestScreenWidthDp
        val params = toolbar.layoutParams as ConstraintLayout.LayoutParams

        if (screenWidthDp >= TABLET_SMALLEST_SCREEN_WIDTH_DP) {
            assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_END)
            // Verify pinned to End
            assertThat(params.horizontalBias).isEqualTo(1f)
            assertThat(params.verticalBias).isEqualTo(0.5f)
        } else {
            assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_BOTTOM)
            // Verify pinned to Bottom
            assertThat(params.verticalBias).isEqualTo(1f)
            assertThat(params.horizontalBias).isEqualTo(0.5f)
        }
    }

    @Test
    fun dragAndDrop_toLeftSide_snapsToStart() {
        onView(withId(TOOLBAR_VIEW_ID)).perform(longClick())
        onIdle()

        performDragAndDrop(toolbarId = TOOLBAR_VIEW_ID, to = ToolbarViewActions.DragTarget.LEFT)
        onIdle()

        assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_START)

        val params = toolbar.layoutParams as ConstraintLayout.LayoutParams
        // Center vertical (0.5) and snapped to start (0.0)
        assertThat(params.horizontalBias).isEqualTo(0f)
        assertThat(params.verticalBias).isEqualTo(0.5f)
        assertThat(params.leftMargin).isAtLeast(1) // Verify margin applied
    }

    @Test
    fun dragAndDrop_toRightSide_snapsToEnd() {
        onView(withId(TOOLBAR_VIEW_ID)).perform(longClick())
        onIdle()

        performDragAndDrop(toolbarId = TOOLBAR_VIEW_ID, to = ToolbarViewActions.DragTarget.RIGHT)
        onIdle()

        assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_END)
        val params = toolbar.layoutParams as ConstraintLayout.LayoutParams
        assertThat(params.horizontalBias).isEqualTo(1f)
        assertThat(params.verticalBias).isEqualTo(0.5f)
    }

    @Test
    fun dragAndDrop_toBottomSide_snapsToBottom() {
        // Move to Start first so we can drag back to bottom
        activityRule.scenario.onActivity { toolbar.dockState = DOCK_STATE_START }
        onIdle()

        onView(withId(TOOLBAR_VIEW_ID)).perform(longClick())
        onIdle()

        performDragAndDrop(toolbarId = TOOLBAR_VIEW_ID, to = ToolbarViewActions.DragTarget.BOTTOM)
        onIdle()

        assertThat(toolbar.dockState).isEqualTo(DOCK_STATE_BOTTOM)
        val params = toolbar.layoutParams as ConstraintLayout.LayoutParams
        assertThat(params.verticalBias).isEqualTo(1f)
        assertThat(params.horizontalBias).isEqualTo(0.5f)
    }

    companion object {
        private const val COORDINATOR_VIEW_ID = 1001
        private const val TOOLBAR_VIEW_ID = 1002

        private const val TABLET_SMALLEST_SCREEN_WIDTH_DP = 600
    }
}
