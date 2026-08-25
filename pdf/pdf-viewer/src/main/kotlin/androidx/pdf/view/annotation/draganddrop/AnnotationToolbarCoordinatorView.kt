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
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.R
import androidx.pdf.view.annotation.AnnotationToolbarView
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_END
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_START

/**
 * A [ViewGroup] layout that manages the dragging, dropping, and docking of an
 * [androidx.pdf.view.annotation.AnnotationToolbarView].
 *
 * This coordinator is responsible for providing visible anchor points for docking, listening for
 * drag gestures initiated on the attached toolbar, moving the toolbar in response to the user's
 * drag input and finally snapping it to the closest anchor when the drag ends.
 *
 * It also applies the correct layout parameters and orientation for toolbar's final docked state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalPdfApi::class)
public class AnnotationToolbarCoordinatorView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs, defStyleAttr) {

    // Required to disable any animation while performing ui tests
    @VisibleForTesting
    internal var areAnimationsEnabled: Boolean = true
        set(value) {
            field = value
            toolbar?.areAnimationsEnabled = value
        }

    private val container: ConstraintLayout =
        ConstraintLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

    private var toolbar: AnnotationToolbarView? = null

    private val anchorManager: AnchorManager

    private val collapseToolWidth = resources.getDimensionPixelSize(R.dimen.annotation_tool_width)
    private val collapsedToolHeight =
        resources.getDimensionPixelSize(R.dimen.annotation_tool_height)

    private val margin16Dp = resources.getDimensionPixelSize(R.dimen.margin_16dp)

    init {
        super.addView(container)
        LayoutInflater.from(context).inflate(R.layout.toolbar_coordinator, container, true)

        anchorManager =
            AnchorManager(
                left = container.findViewById(R.id.anchorLeft),
                right = container.findViewById(R.id.anchorRight),
                bottom = container.findViewById(R.id.anchorBottom),
            )
    }

    /** Re-applies the layout constraints for the toolbar's current dock state. */
    internal fun updateLayout() {
        val localToolbar = toolbar ?: return

        applyDockLayoutParams(localToolbar.dockState)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable?>?) {
        super.dispatchRestoreInstanceState(container)
        // update layout after toolbar has restored its previous dock state
        updateLayout()
    }

    /**
     * Adds an [AnnotationToolbarView] to the coordinator.
     *
     * Supports only a single [AnnotationToolbarView]. Calling this method again will remove and
     * replace any previously attached toolbar instance.
     *
     * @param child child [AnnotationToolbarView] to attach
     * @param index ignored; the toolbar is always attached as the top-most view above internal
     *   anchors.
     * @param params layout parameters for the child
     * @throws IllegalArgumentException if [child] is not an [AnnotationToolbarView]
     */
    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        if (child == null) return
        if (child === container) {
            super.addView(child, index, params)
            return
        }

        require(child is AnnotationToolbarView) {
            "AnnotationToolbarCoordinatorView only supports child of type AnnotationToolbarView"
        }

        if (child.id == NO_ID) {
            child.id = generateViewId()
        }

        // Remove if already added
        toolbar?.let { container.removeView(it) }

        this.toolbar = child
        child.areAnimationsEnabled = areAnimationsEnabled
        child.setOnDockStateChangedListener { dockState -> applyDockLayoutParams(dockState) }

        container.addView(child, -1, params)
        initializeDragAndDrop()
        updateLayout()
    }

    /**
     * Removes the specified view from this coordinator.
     *
     * Supports only removing the attached [AnnotationToolbarView]. If the provided view is not the
     * attached [AnnotationToolbarView], this method does nothing.
     *
     * @param view The [View] to remove.
     */
    override fun removeView(view: View?) {
        if (view == toolbar) {
            container.removeView(view)
            this.toolbar = null
        } else if (view == container) {
            super.removeView(view)
        }
    }

    /**
     * Removing views by index is unsupported on [AnnotationToolbarCoordinatorView], as it can hold
     * at most 1 child of type [AnnotationToolbarView].
     *
     * @throws UnsupportedOperationException always.
     */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException(
            "removeViewAt(index) is not supported on AnnotationToolbarCoordinatorView."
        )
    }

    /**
     * Removes the attached [AnnotationToolbarView] from this coordinator and clears internal state.
     */
    override fun removeAllViews() {
        toolbar?.let {
            container.removeView(it)
            this.toolbar = null
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        container.measure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(container.measuredWidth, container.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        container.layout(0, 0, r - l, b - t)
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
     * @param state The target [AnnotationToolbarView.DockState] to snap to.
     */
    private fun snapToState(@AnnotationToolbarView.DockState state: Int) {
        val localToolbar = toolbar ?: return
        val targetAnchor = anchorManager.getAnchorView(state) ?: return

        // Calculate target position (centering toolbar over anchor)
        val targetX = targetAnchor.x + (targetAnchor.width / 2) - (localToolbar.width / 2)
        val targetY = targetAnchor.y + (targetAnchor.height / 2) - (localToolbar.height / 2)

        dockToolbar(localToolbar, targetX, targetY, state)
    }

    private fun dockToolbar(
        toolbar: AnnotationToolbarView,
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
     * @param state The target [AnnotationToolbarView.DockState].
     */
    private fun applyDockLayoutParams(@AnnotationToolbarView.DockState state: Int) {
        val localToolbar = toolbar ?: return
        val toolbarId = localToolbar.id

        // Reset toolbar translation; critical as we previously animated view.translateX and
        // view.translateY
        localToolbar.translationX = 0f
        localToolbar.translationY = 0f

        ConstraintSet().apply {
            clone(container)
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

            applyTo(container)
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
