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

package androidx.pdf.ink.view.layout

import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.pdf.ink.R

/**
 * A manager that handles swapping between vertical and horizontal scroll containers for the
 * [androidx.pdf.ink.view.AnnotationToolbar]'s tool tray.
 *
 * This class eliminates the need for deeply nested view hierarchies by dynamically re-parenting the
 * tool tray into either a [ScrollView] or a [HorizontalScrollView] based on the current
 * orientation, while maintaining a stable view ID for layout constraints.
 *
 * @param container The parent [ViewGroup] where the scroll containers will be attached.
 * @param toolTray The [ViewGroup] containing the actual tools that needs to be scrollable.
 */
internal class ToolTrayScrollerManager(
    private val container: ViewGroup,
    private val toolTray: ViewGroup,
) {

    private val context = container.context
    private val scrollViewId = R.id.scrollable_tool_tray_container
    private val wrapContentLayoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

    private val verticalScrollView =
        ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            layoutParams = wrapContentLayoutParams
            // Scroll view doesn't contain any state, skip save/restoration cycle
            isSaveEnabled = false
        }
    private val horizontalScrollView =
        HorizontalScrollView(context).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            layoutParams = wrapContentLayoutParams
            // Scroll view doesn't contain any state, skip save/restoration cycle
            isSaveEnabled = false
        }

    fun setOrientation(newOrientation: Int) {
        if (toolTray !is LinearLayout || toolTray.orientation == newOrientation) return

        // Detach the tool tray from its current parent (if any) before re-parenting
        (toolTray.parent as? ViewGroup)?.removeView(toolTray)

        // Clear the existing scroll container from the main layout
        val currentScrollView = container.findViewById<View>(scrollViewId)
        container.removeView(currentScrollView)

        toolTray.orientation = newOrientation

        val scrollView =
            if (newOrientation == LinearLayout.VERTICAL) {
                verticalScrollView.apply {
                    id = scrollViewId
                    addView(toolTray)
                }
            } else {
                horizontalScrollView.apply {
                    id = scrollViewId
                    addView(toolTray)
                }
            }

        // Inject the selected scroll container back into the layout hierarchy.
        // Note: Layout constraints are managed externally by AnnotationToolbar
        // based on the current docking state.
        container.addView(scrollView)
    }
}
