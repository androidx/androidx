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

package androidx.testutils

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat

/**
 * Sets exclusion rects for system gestures on this view that will make sure Espresso's
 * predefined swipes (such as [swipeRight][androidx.test.espresso.action.ViewActions.swipeRight])
 * won't be mistaken for system gestures.
 *
 * Set this on the view on which you will perform the swipe ViewActions.
 */
fun View.setSystemExclusionRectsForEspressoSwipes() {
    addOnLayoutChangeListener(SetExclusionRectsOnLayout())
}

private class SetExclusionRectsOnLayout : View.OnLayoutChangeListener {
    private val exclusionRects = mutableListOf<Rect>()

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if (v == null) return
        exclusionRects.clear()
        val vCenter = (bottom - top) / 2
        val hCenter = (right - left) / 2
        // Create an exclusion strip of 2px thick (center +/- 1) through the middle, hor and ver
        exclusionRects += Rect(left, vCenter - 1, right, vCenter + 1)
        exclusionRects += Rect(hCenter - 1, top, hCenter + 1, bottom)
        ViewCompat.setSystemGestureExclusionRects(v, exclusionRects)
    }
}
