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

package androidx.pdf.view.annotation

import android.app.Activity
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.pdf.ActivityUtils
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.R
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_END
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_START
import androidx.pdf.view.annotation.draganddrop.AnnotationToolbarCoordinatorView
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalPdfApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class AnnotationToolbarViewTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = ActivityUtils.getThemedActivity()
    }

    @Test
    fun applyDockConstraints_preservesChildViewVisibility() {
        val toolbar = AnnotationToolbarView(activity)

        for (state in listOf(DOCK_STATE_BOTTOM, DOCK_STATE_START, DOCK_STATE_END)) {
            toolbar.dockState = state
            assertThat(toolbar.findViewById<View>(R.id.scrollable_tool_tray_container).visibility)
                .isEqualTo(View.VISIBLE)
            assertThat(toolbar.findViewById<View>(R.id.brush_size_selector).visibility)
                .isEqualTo(View.GONE)
            assertThat(toolbar.findViewById<View>(R.id.color_palette).visibility)
                .isEqualTo(View.GONE)
            assertThat(toolbar.findViewById<View>(R.id.collapsed_tool).visibility)
                .isEqualTo(View.GONE)
        }
    }

    @Test
    fun onWindowVisibilityChanged_appliesRestoredStateSynchronouslyBeforeLayout() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        val visibleActivity = controller.get()
        visibleActivity.setTheme(
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        val root = FrameLayout(visibleActivity)
        visibleActivity.setContentView(root)
        controller.start().resume().visible()
        ShadowLooper.idleMainLooper()

        val toolbarId = View.generateViewId()
        val initialToolbar = AnnotationToolbarView(visibleActivity).apply { id = toolbarId }
        root.addView(
            initialToolbar,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        initialToolbar.dispatchWindowVisibilityChanged(View.VISIBLE)

        // Open the brush slider and save state
        initialToolbar.findViewById<View>(R.id.pen_button).performClick()
        ShadowLooper.idleMainLooper()
        assertThat(initialToolbar.findViewById<View>(R.id.brush_size_selector).isVisible).isTrue()
        val openPopupState = SparseArray<Parcelable>()
        initialToolbar.saveHierarchyState(openPopupState)

        // Dismiss popups and save state
        initialToolbar.dismissPopups()
        ShadowLooper.idleMainLooper()
        assertThat(initialToolbar.findViewById<View>(R.id.brush_size_selector).isVisible).isFalse()
        val dismissedPopupState = SparseArray<Parcelable>()
        initialToolbar.saveHierarchyState(dismissedPopupState)
        root.removeView(initialToolbar)

        // 1. Recreate with open popup state: must synchronously restore brush_size_selector to
        // VISIBLE
        val recreatedWithOpenPopup = AnnotationToolbarView(visibleActivity).apply { id = toolbarId }
        recreatedWithOpenPopup.restoreHierarchyState(openPopupState)
        root.addView(
            recreatedWithOpenPopup,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        recreatedWithOpenPopup.dispatchWindowVisibilityChanged(View.VISIBLE)
        assertThat(recreatedWithOpenPopup.findViewById<View>(R.id.brush_size_selector).isVisible)
            .isTrue()
        assertThat(recreatedWithOpenPopup.isConfigPopupVisible).isTrue()
        root.removeView(recreatedWithOpenPopup)

        // 2. Recreate with dismissed popup state: popups must remain GONE immediately
        val recreatedWithDismissedPopup =
            AnnotationToolbarView(visibleActivity).apply { id = toolbarId }
        recreatedWithDismissedPopup.restoreHierarchyState(dismissedPopupState)
        root.addView(
            recreatedWithDismissedPopup,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        recreatedWithDismissedPopup.dispatchWindowVisibilityChanged(View.VISIBLE)
        assertThat(
                recreatedWithDismissedPopup.findViewById<View>(R.id.brush_size_selector).isVisible
            )
            .isFalse()
        assertThat(recreatedWithDismissedPopup.findViewById<View>(R.id.color_palette).isVisible)
            .isFalse()
        assertThat(recreatedWithDismissedPopup.isConfigPopupVisible).isFalse()
    }

    @Test
    fun coordinatorView_updateLayout_preservesToolbarVisibility() {
        val coordinator = AnnotationToolbarCoordinatorView(activity)
        val toolbar =
            AnnotationToolbarView(activity).apply {
                id = R.id.annotationToolbar
                visibility = View.GONE
            }
        coordinator.addView(
            toolbar,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )

        coordinator.updateLayout()

        assertThat(toolbar.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun init_setsDefaultId_whenNoIdProvided() {
        val toolbar = AnnotationToolbarView(activity)

        assertThat(toolbar.id).isEqualTo(R.id.annotationToolbar)
    }

    @Test
    fun onConfigPopupVisibilityChangedListener_invokedWhenPopupToggledAndRemovedCleanly() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        val visibleActivity = controller.get()
        visibleActivity.setTheme(
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        val root = FrameLayout(visibleActivity)
        visibleActivity.setContentView(root)
        controller.start().resume().visible()
        ShadowLooper.idleMainLooper()

        val toolbar = AnnotationToolbarView(visibleActivity)
        val popupVisibilityEvents = mutableListOf<Boolean>()
        val listener = AnnotationToolbarView.OnConfigPopupVisibilityChangedListener { isVisible ->
            popupVisibilityEvents.add(isVisible)
        }
        toolbar.addOnConfigPopupVisibilityChangedListener(listener)

        root.addView(toolbar, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        toolbar.dispatchWindowVisibilityChanged(View.VISIBLE)
        ShadowLooper.idleMainLooper()

        assertThat(popupVisibilityEvents).containsExactly(false)

        // Open brush size slider
        toolbar.findViewById<View>(R.id.pen_button).performClick()
        ShadowLooper.idleMainLooper()
        assertThat(popupVisibilityEvents).containsExactly(false, true).inOrder()

        // Switching directly to color palette keeps popup visible and should not emit duplicate
        // true
        toolbar.findViewById<View>(R.id.color_palette_button).performClick()
        ShadowLooper.idleMainLooper()
        assertThat(popupVisibilityEvents).containsExactly(false, true).inOrder()

        // Dismiss popup (and verify a redundant dismiss does not emit duplicate false)
        toolbar.dismissPopups()
        toolbar.dismissPopups()
        ShadowLooper.idleMainLooper()
        assertThat(popupVisibilityEvents).containsExactly(false, true, false).inOrder()

        // Remove listener and verify no further invocations
        toolbar.removeOnConfigPopupVisibilityChangedListener(listener)
        toolbar.findViewById<View>(R.id.pen_button).performClick()
        ShadowLooper.idleMainLooper()
        assertThat(popupVisibilityEvents).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun onExpandedStateChangedListener_invokedWhenExpandedOrCollapsedAndRemovedCleanly() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        val visibleActivity = controller.get()
        visibleActivity.setTheme(
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        val root = FrameLayout(visibleActivity)
        visibleActivity.setContentView(root)
        controller.start().resume().visible()
        ShadowLooper.idleMainLooper()

        val toolbar = AnnotationToolbarView(visibleActivity)
        val expandedEvents = mutableListOf<Boolean>()
        val listener = AnnotationToolbarView.OnExpandedStateChangedListener { isExpanded ->
            expandedEvents.add(isExpanded)
        }
        toolbar.addOnExpandedStateChangedListener(listener)

        root.addView(toolbar, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        toolbar.dispatchWindowVisibilityChanged(View.VISIBLE)
        ShadowLooper.idleMainLooper()

        assertThat(toolbar.isExpanded).isTrue()
        assertThat(expandedEvents).containsExactly(true)

        // Collapse (and verify a redundant collapse does not emit duplicate false)
        toolbar.collapseToolbar()
        toolbar.collapseToolbar()
        ShadowLooper.idleMainLooper()
        assertThat(toolbar.isExpanded).isFalse()
        assertThat(expandedEvents).containsExactly(true, false).inOrder()

        // Expand (and verify a redundant expand does not emit duplicate true)
        toolbar.expandToolbar()
        toolbar.expandToolbar()
        ShadowLooper.idleMainLooper()
        assertThat(toolbar.isExpanded).isTrue()
        assertThat(expandedEvents).containsExactly(true, false, true).inOrder()

        // Remove listener and verify no further invocations
        toolbar.removeOnExpandedStateChangedListener(listener)
        toolbar.collapseToolbar()
        ShadowLooper.idleMainLooper()
        assertThat(toolbar.isExpanded).isFalse()
        assertThat(expandedEvents).containsExactly(true, false, true).inOrder()
    }

    @Test
    fun coordinatorView_restoreHierarchyState_withoutToolbar_doesNotThrow() {
        val coordinator = AnnotationToolbarCoordinatorView(activity)
        val state = SparseArray<Parcelable>()

        coordinator.saveHierarchyState(state)
        coordinator.restoreHierarchyState(state)
    }

    @Test
    fun coordinatorView_removeView_detachesToolbarAndUnregistersDockListener() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        val visibleActivity = controller.get()
        val coordinator = AnnotationToolbarCoordinatorView(visibleActivity)
        visibleActivity.setContentView(coordinator)
        controller.start().resume().visible()
        ShadowLooper.idleMainLooper()

        val toolbar = AnnotationToolbarView(visibleActivity)
        coordinator.addView(toolbar)
        toolbar.dispatchWindowVisibilityChanged(View.VISIBLE)
        ShadowLooper.idleMainLooper()
        assertThat(toolbar.parent).isNotNull()

        // Removing an unrelated view is a no-op and keeps the toolbar attached
        coordinator.removeView(View(visibleActivity))
        assertThat(toolbar.parent).isNotNull()

        // Removing the attached toolbar detaches it and unregisters the coordinator's dock listener
        val paramsBeforeRemove = toolbar.layoutParams as ConstraintLayout.LayoutParams
        assertThat(paramsBeforeRemove.horizontalBias).isEqualTo(0.5f)
        coordinator.removeView(toolbar)
        assertThat(toolbar.parent).isNull()

        toolbar.dockState = AnnotationToolbarView.DOCK_STATE_START
        ShadowLooper.idleMainLooper()
        assertThat((toolbar.layoutParams as ConstraintLayout.LayoutParams).horizontalBias)
            .isEqualTo(0.5f)
    }
}
