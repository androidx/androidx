/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material.layouts;

import static androidx.wear.tiles.DimensionBuilders.dp;

import androidx.wear.tiles.DimensionBuilders.DpProp;

/** Contains the default values used by layout templates for Tiles. */
public class LayoutDefaults {
    private LayoutDefaults() {}

    /**
     * The default percentage for the bottom margin for primary chip in the {@link PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT = 6.3f / 100;

    /**
     * The default percentage for the bottom margin for primary chip in the {@link PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT = 2.2f / 100;

    /**
     * The default percentage for the top margin for primary chip in the {@link PrimaryLayout} on
     * round devices.
     */
    static final float PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT = 16.7f / 100;

    /**
     * The default percentage for the top margin for primary chip in the {@link PrimaryLayout} on
     * square devices.
     */
    static final float PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT = 15.6f / 100;

    /**
     * The default percentage for the horizontal margin for primary chip in the {@link
     * PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT = 5.2f / 100;

    /**
     * The default percentage for the horizontal margin for primary chip in the {@link
     * PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT = 2.8f / 100;

    /** The default spacer height for primary chip in the {@link PrimaryLayout}. */
    static final DpProp PRIMARY_LAYOUT_SPACER_HEIGHT = dp(12);

    /** The default horizontal margin in the {@link ProgressIndicatorLayout}. */
    static final float PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP = 14;

    /** The default horizontal margin in the {@link ProgressIndicatorLayout}. */
    static final float PROGRESS_INDICATOR_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP = 16;

    /**
     * The recommended padding that should be above the main content (text) in the {@link
     * ProgressIndicatorLayout}.
     */
    public static final float PROGRESS_INDICATOR_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP = 6;

    /**
     * The recommended padding that should be below the main content (text) in the {@link
     * ProgressIndicatorLayout}.
     */
    public static final float PROGRESS_INDICATOR_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP = 8;

    /** The default spacer width for slots in a {@link MultiSlotLayout}. */
    public static final DpProp MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH = dp(8);

    /** The recommended space between slots in a {@link MultiSlotLayout} and additional labels. */
    public static final DpProp MULTI_SLOT_LAYOUT_VERTICAL_SPACER_HEIGHT = dp(8);
}
