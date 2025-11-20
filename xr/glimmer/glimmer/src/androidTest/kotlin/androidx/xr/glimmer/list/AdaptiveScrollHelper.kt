/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy

/**
 * In the Glimmer list, we use adaptive scrolling to make focus movement predictable. However, this
 * can result in non-linear content scrolling. In a regular list, if you scroll 100 pixels, the
 * content will also scroll 100 pixels. In the Glimmer list, however, the passed scroll value is
 * shared between content scrolling (Sc) and focus line scrolling (Sf). This method allows you to
 * specify a desired value for the content scroll, but internally a larger value will be applied to
 * account for the focus line movement.
 *
 * In mathematical terms, it converts `Sc -> Su` and applies `Su` to the state.
 */
internal suspend fun ListState.scrollContentBy(value: Float) {
    val fullViewport =
        if (layoutInfo.orientation == Orientation.Vertical) {
            layoutInfo.viewportSize.height
        } else {
            layoutInfo.viewportSize.width
        }
    val viewportWithoutPaddings =
        fullViewport - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val contentLength = layoutInfo.totalItemsCount * layoutInfo.visibleItemsAverageSize()
    val scrollThreshold = viewportWithoutPaddings * ProportionalThresholdFactor

    val userScroll =
        AutoFocusScrollConverter.convertContentScrollToUserScroll(
            contentScroll = value,
            scrollThreshold = scrollThreshold,
            viewportSize = viewportWithoutPaddings.toFloat(),
            contentLength = contentLength.toFloat(),
        )
    scrollBy(userScroll)
}
