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

package androidx.pdf.view.annotation.draganddrop

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.animation.OvershootInterpolator
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.pdf.R
import androidx.pdf.view.annotation.AnnotationToolbar
import androidx.pdf.view.annotation.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.view.annotation.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.view.annotation.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START

/**
 * A [ConstraintLayout] layout that manages the dragging, dropping, and docking of an
 * [AnnotationToolbar].
 *
 * This coordinator is responsible for providing visible anchor points for docking, listening for
 * drag gestures initiated on the attached toolbar, moving the toolbar in response to the user's
 * drag input and finally snapping it to the closest anchor when the drag ends.
 *
 * It also applied the correct layout parameters and orientation for toolbar's final docked state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ToolbarCoordinator(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    // Required to disable any animation while performing ui tests
    @VisibleForTesting
    internal var areAnimationsEnabled: Boolean = true
        set(value) {
            field = value
            toolbar?.areAnimationsEnabled = value
        }

    private var toolbar: AnnotationToolbar? = null

    private val anchorManager: AnchorManager

    private val collapseToolWidth = resources.getDimensionPixelSize(R.dimen.annotation_tool_width)
    private val collapsedToolHeight =
        resources.getDimensionPixelSize(R.dimen.annotation_tool_height)

    private val margin16Dp = resources.getDimensionPixelSize(R.dimen.margin_16dp)

    init {
        LayoutInflater.from(context).inflate(R.layout.toolbar_coordinator, this, true)

        anchorManager =
            AnchorManager(
                left = findViewById(R.id.anchorLeft),
                right = findViewById(R.id.anchorRight),
                bottom = findViewById(R.id.anchorBottom),
            )
    }

    /**
     * Attaches the [AnnotationToolbar] to this coordinator and sets up drag-and-drop handling.
     *
     * @param view The [AnnotationToolbar] instance to manage.
     */
    public fun attachToolbar(view: AnnotationToolbar) {
        // Remove if already added
        toolbar?.let { removeView(it) }

        this.toolbar = view
        view.areAnimationsEnabled = areAnimationsEnabled

        addView(view)
        initializeDragAndDrop()
        updateLayout()
    }

    /** Re-applies the layout constraints for the toolbar's current dock state. */
    public fun updateLayout() {
        val localToolbar = toolbar ?: throw IllegalStateException("toolbar is not attached")

        applyDockLayoutParams(localToolbar.dockState)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable?>?) {
        super.dispatchRestoreInstanceState(container)
        // update layout after toolbar has restored its previous dock state
        updateLayout()
    }

    private fun initializeDragAndDrop() {
        val toolbar = toolbar ?: return

        setOnDragListener { _, event ->
            val isToolbarVertical = toolbar.dockState != DOCK_STATE_BOTTOM
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    anchorManager.showAnchors()
                    toolbar.collapseToolbar()
                    true
                }

                DragEvent.ACTION_DRAG_LOCATION -> {
                    val collapseViewSize =
                        if (isToolbarVertical) collapsedToolHeight else collapseToolWidth
                    toolbar.x = event.x - collapseViewSize
                    toolbar.y = event.y - collapseViewSize

                    anchorManager.updateHighlightingAndGetClosest(
                        toolbar.x,
                        toolbar.y,
                        toolbar.width,
                        toolbar.height,
                    )
                    true
                }

                DragEvent.ACTION_DROP -> {
                    // No-Op: We handle snapping in ACTION_DRAG_ENDED to ensure it happens even if
                    // the drop occurs outside the coordinator's bounds.
                    true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    anchorManager.hideAnchors()
                    val closestState =
                        anchorManager.updateHighlightingAndGetClosest(
                            toolbar.x,
                            toolbar.y,
                            toolbar.width,
                            toolbar.height,
                        )
                    snapToState(closestState)
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Animates the toolbar to its final position over the target anchor.
     *
     * @param state The target [ToolbarDockState] to snap to.
     */
    private fun snapToState(@ToolbarDockState.DockState state: Int) {
        val localToolbar = toolbar ?: return
        val targetAnchor = anchorManager.getAnchorView(state) ?: return

        // Calculate target position (centering toolbar over anchor)
        val targetX = targetAnchor.x + (targetAnchor.width / 2) - (localToolbar.width / 2)
        val targetY = targetAnchor.y + (targetAnchor.height / 2) - (localToolbar.height / 2)

        dockToolbar(localToolbar, targetX, targetY, state)
    }

    private fun dockToolbar(
        toolbar: AnnotationToolbar,
        targetX: Float,
        targetY: Float,
        state: Int,
    ) {
        if (areAnimationsEnabled) {
            toolbar
                .animate()
                .x(targetX)
                .y(targetY)
                .setDuration(SNAP_ANIMATION_DURATION)
                .setInterpolator(OvershootInterpolator(SNAP_BOUNCE_TENSION)) // The "Snap" bounce
                .withEndAction {
                    applyDockLayoutParams(state)
                    toolbar.post { toolbar.expandToolbar() }
                }
                .start()
        } else {
            toolbar.x = targetX
            toolbar.y = targetY
            applyDockLayoutParams(state)
            toolbar.post { toolbar.expandToolbar() }
        }
    }

    /**
     * Applies the final layout parameters to the toolbar based on its new docked state.
     *
     * @param state The target [ToolbarDockState].
     */
    private fun applyDockLayoutParams(@ToolbarDockState.DockState state: Int) {
        val localToolbar = toolbar ?: return
        val toolbarId = localToolbar.id

        // Reset toolbar translation; critical as we previously animated view.translateX and
        // view.translateY
        localToolbar.translationX = 0f
        localToolbar.translationY = 0f

        ConstraintSet().apply {
            clone(this@ToolbarCoordinator)
            clear(toolbarId)

            constrainWidth(toolbarId, ConstraintSet.WRAP_CONTENT)
            constrainHeight(toolbarId, ConstraintSet.WRAP_CONTENT)

            // Center toolbar by default, then bias it toward the specific dock edge
            center(toolbarId, ConstraintSet.PARENT_ID)

            when (state) {
                DOCK_STATE_START -> {
                    setHorizontalBias(toolbarId, 0f)
                    setMargin(toolbarId, ConstraintSet.START, margin16Dp)
                    constrainedWidth(toolbarId, true)
                }
                DOCK_STATE_END -> {
                    setHorizontalBias(toolbarId, 1f)
                    setMargin(toolbarId, ConstraintSet.END, margin16Dp)
                    constrainedWidth(toolbarId, true)
                }
                DOCK_STATE_BOTTOM -> {
                    setVerticalBias(toolbarId, 1f)
                    setMargin(toolbarId, ConstraintSet.BOTTOM, margin16Dp)
                    constrainedHeight(toolbarId, true)
                }
            }

            applyTo(this@ToolbarCoordinator)
        }
        localToolbar.dockState = state
    }

    /**
     * Helper to establish constraints to all four sides of the parent. This makes biasing
     * (start/end/bottom) much more concise.
     */
    private fun ConstraintSet.center(viewId: Int, parentId: Int) {
        connect(viewId, ConstraintSet.START, parentId, ConstraintSet.START)
        connect(viewId, ConstraintSet.END, parentId, ConstraintSet.END)
        connect(viewId, ConstraintSet.TOP, parentId, ConstraintSet.TOP)
        connect(viewId, ConstraintSet.BOTTOM, parentId, ConstraintSet.BOTTOM)
    }

    public companion object {
        private const val SNAP_ANIMATION_DURATION = 250L
        private const val SNAP_BOUNCE_TENSION = 1.0f
    }
}
