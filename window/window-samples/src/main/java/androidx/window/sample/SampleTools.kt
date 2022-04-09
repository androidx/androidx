/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.window.sample
import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import androidx.window.layout.DisplayFeature

/**
 * Offset the [DisplayFeature]'s bounds to match its respective
 * location in the view's coordinate space.
 */
fun adjustFeaturePositionOffset(displayFeature: DisplayFeature, view: View): Rect {
    // Get the location of the view in window to be in the same coordinate space as the feature.
    val viewLocationInWindow = IntArray(2)
    view.getLocationInWindow(viewLocationInWindow)

    // Offset the feature coordinates to view coordinate space start point
    val featureRectInView = Rect(displayFeature.bounds)
    featureRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1])

    return featureRectInView
}

/**
 * Gets the layout params for placing a rectangle indicating a
 * [DisplayFeature] inside a [FrameLayout].
 */
fun getLayoutParamsForFeatureInFrameLayout(displayFeature: DisplayFeature, view: FrameLayout):
    FrameLayout.LayoutParams {
    val featureRectInView = adjustFeaturePositionOffset(displayFeature, view)
    val lp = FrameLayout.LayoutParams(featureRectInView.width(), featureRectInView.height())
    lp.leftMargin = featureRectInView.left
    lp.topMargin = featureRectInView.top
    return lp
}
