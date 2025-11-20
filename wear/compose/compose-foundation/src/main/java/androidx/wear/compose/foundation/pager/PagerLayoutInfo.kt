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

package androidx.wear.compose.foundation.pager

import androidx.compose.foundation.gestures.Orientation

/**
 * Contains useful information about the currently displayed layout state of a [HorizontalPager] or
 * [VerticalPager]. This information is available after the first measure pass.
 *
 * Use [PagerState.layoutInfo] to retrieve this
 */
public sealed interface PagerLayoutInfo {
    /** The main axis size of the pages in this pager, in pixels. */
    public val pageSize: Int

    /** The orientation of this pager (which could be [HorizontalPager] or [VerticalPager]. */
    public val orientation: Orientation
}
