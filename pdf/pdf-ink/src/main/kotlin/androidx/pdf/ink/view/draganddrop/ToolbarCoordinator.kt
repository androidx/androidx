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

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.pdf.ink.R
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import org.jetbrains.annotations.VisibleForTesting

/**
 * A [FrameLayout] layout that manages the dragging, dropping, and docking of an
 * [AnnotationToolbar].
 *
 * This coordinator is responsible for providing visible anchor points for docking, listening for
 * drag gestures initiated on the attached toolbar, moving the toolbar in response to the user's
 * drag input and finally snapping it to the closest anchor when the drag ends.
 *
 * It also applied the correct layout parameters and orientation for toolbar's final docked state.
 */
internal class ToolbarCoordinator(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    // Required to disable any animation while performing ui tests
    @VisibleForTesting internal var areAnimationsEnabled: Boolean = true

    private var toolbar: AnnotationToolbar? = null

    private val anchorManager: AnchorManager

    private lateinit var toolbarDragListener: ToolbarDragListener

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
    fun attachToolbar(view: AnnotationToolbar) {
        // Remove if already added
        toolbar?.let { removeView(it) }

        this.toolbar = view

        addView(view)
        initializeDragAndDrop()
        updateLayout()
    }

    /** Re-applies the layout constraints for the toolbar's current dock state. */
    fun updateLayout() {
        toolbar?.post {
            applyDockLayoutParams(
                toolbar?.dockState ?: throw IllegalStateException("dock state not initialized")
            )
        }
    }

    private fun initializeDragAndDrop() {
        val toolbar = toolbar ?: return
        val isToolbarVertical = toolbar.dockState != DOCK_STATE_BOTTOM

        toolbarDragListener =
            object : ToolbarDragListener {

                override fun onDragStart(event: MotionEvent) {
                    anchorManager.showAnchors()
                    toolbar.collapseToolbar()
                }

                override fun onDragMove(event: MotionEvent) {
                    val collapseViewSize =
                        if (isToolbarVertical) collapsedToolHeight else collapseToolWidth
                    toolbar.x = event.rawX - collapseViewSize
                    toolbar.y = event.rawY - collapseViewSize

                    anchorManager.updateHighlightingAndGetClosest(
                        toolbar.x,
                        toolbar.y,
                        toolbar.width,
                        toolbar.height,
                    )
                }

                override fun onDragEnd() {
                    val closestState =
                        anchorManager.updateHighlightingAndGetClosest(
                            toolbar.x,
                            toolbar.y,
                            toolbar.width,
                            toolbar.height,
                        )

                    anchorManager.hideAnchors()

                    snapToState(closestState)
                }
            }

        toolbar.setOnToolbarDragListener(toolbarDragListener)
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

        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        // Reset toolbar translation; critical as we previously animated view.translateX and
        // view.translateY
        localToolbar.translationX = 0f
        localToolbar.translationY = 0f

        when (state) {
            DOCK_STATE_START -> {
                params.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                params.marginStart = margin16Dp
            }
            DOCK_STATE_END -> {
                params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                params.marginEnd = margin16Dp
            }
            DOCK_STATE_BOTTOM -> {
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.bottomMargin = margin16Dp
            }
        }

        localToolbar.layoutParams = params
        localToolbar.dockState = state
    }

    companion object {
        private const val SNAP_ANIMATION_DURATION = 250L
        private const val SNAP_BOUNCE_TENSION = 1.0f
    }
}
