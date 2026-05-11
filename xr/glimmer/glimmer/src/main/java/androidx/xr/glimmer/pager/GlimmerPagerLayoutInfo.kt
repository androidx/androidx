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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.unit.IntSize

/**
 * Contains useful information about the currently displayed layout state of a
 * [GlimmerHorizontalPager]. This information is available after the first measure pass.
 *
 * Use [GlimmerPagerState.layoutInfo] to retrieve this.
 */
public sealed interface GlimmerPagerLayoutInfo {
    /** The main axis size of the Pages in pixels for [GlimmerHorizontalPager]. */
    public val pageSize: Int

    /** The page spacing in pixels for [GlimmerHorizontalPager]. */
    public val pageSpacing: Int

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Usually it is 0, but it can be negative if non-zero
     * beforeContentPadding was applied as the content displayed in the content padding area is
     * still visible.
     */
    public val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the pager layout minus [beforeContentPadding].
     */
    public val viewportEndOffset: Int

    /**
     * The content padding in pixels applied before the first page in the direction of scrolling.
     * For example it is a start content padding for [GlimmerHorizontalPager] with reverseLayout set
     * to false.
     */
    public val beforeContentPadding: Int

    /**
     * The content padding in pixels applied after the last page in the direction of scrolling. For
     * example it is a end content padding for [GlimmerHorizontalPager] with reverseLayout set to
     * false.
     */
    public val afterContentPadding: Int

    /**
     * The size of the viewport in pixels. It is the [GlimmerHorizontalPager] layout size including
     * all the content paddings.
     */
    public val viewportSize: IntSize

    /** True if the direction of scrolling and layout is reversed. */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    public val reverseLayout: Boolean
}

internal class DefaultGlimmerPagerLayoutInfo(val state: PagerState) : GlimmerPagerLayoutInfo {
    override val pageSize: Int
        get() = state.layoutInfo.pageSize

    override val pageSpacing: Int
        get() = state.layoutInfo.pageSpacing

    override val viewportStartOffset: Int
        get() = state.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
        get() = state.layoutInfo.viewportEndOffset

    override val beforeContentPadding: Int
        get() = state.layoutInfo.beforeContentPadding

    override val afterContentPadding: Int
        get() = state.layoutInfo.afterContentPadding

    override val viewportSize: IntSize
        get() = state.layoutInfo.viewportSize

    override val reverseLayout: Boolean
        get() = state.layoutInfo.reverseLayout
}
