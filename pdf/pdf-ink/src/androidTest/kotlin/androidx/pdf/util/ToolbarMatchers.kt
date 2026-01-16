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

package androidx.pdf.util

import android.graphics.RectF
import android.view.View
import androidx.ink.authoring.InProgressStrokesView
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Matcher

internal object ToolbarMatchers {
    /**
     * Verifies that the [InProgressStrokesView] has a mask that matches the bounds of the toolbar.
     */
    fun matchesToolbarMask(toolbar: AnnotationToolbar): Matcher<View> {
        return object :
            BoundedMatcher<View, InProgressStrokesView>(InProgressStrokesView::class.java) {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText(
                    "with mask path matching toolbar at ${toolbar.x}, ${toolbar.y}"
                )
            }

            override fun matchesSafely(view: InProgressStrokesView): Boolean {
                val mask = view.maskPath ?: return false
                val bounds = RectF()
                mask.computeBounds(bounds, true)

                // The mask should roughly match the toolbar's location and size
                return bounds.left == toolbar.x &&
                    bounds.top == toolbar.y &&
                    bounds.width() == toolbar.width.toFloat() &&
                    bounds.height() == toolbar.height.toFloat()
            }
        }
    }

    /** Custom Matcher to check the internal dockState of the AnnotationToolbar. */
    fun withDockState(expectedState: Int): Matcher<View> {
        return object : BoundedMatcher<View, AnnotationToolbar>(AnnotationToolbar::class.java) {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("with dockState: $expectedState")
            }

            override fun matchesSafely(toolbar: AnnotationToolbar): Boolean {
                return toolbar.dockState == expectedState
            }
        }
    }
}
