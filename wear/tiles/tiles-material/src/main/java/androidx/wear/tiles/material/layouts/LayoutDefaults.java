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

import androidx.annotation.NonNull;
import androidx.wear.tiles.material.ButtonDefaults;

/**
 * Contains the default values used by layout templates for Tiles.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.layouts.LayoutDefaults}
 *     which provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class LayoutDefaults {
    private LayoutDefaults() {}

    /**
     * The default percentage for the bottom margin for primary chip in the {@link PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_BOTTOM_ROUND_PERCENT = 2.1f / 100;

    /**
     * The default percentage for the bottom margin for primary chip in the {@link PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_BOTTOM_SQUARE_PERCENT = 0;

    /**
     * The default percentage for the top margin for primary chip in the {@link PrimaryLayout} on
     * round devices.
     */
    static final float PRIMARY_LAYOUT_MARGIN_TOP_ROUND_PERCENT = 16.7f / 100;

    /**
     * The default percentage for the top margin for primary chip in the {@link PrimaryLayout} on
     * square devices.
     */
    static final float PRIMARY_LAYOUT_MARGIN_TOP_SQUARE_PERCENT = 13.3f / 100;

    /**
     * The default spacer above primary label in {@link PrimaryLayout} to make space for Tile icon
     * on round devices.
     */
    static final androidx.wear.tiles.DimensionBuilders.DpProp
            PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP =
                    androidx.wear.tiles.DimensionBuilders.dp(0);

    /**
     * The default spacer above primary label in {@link PrimaryLayout} to make space for Tile icon
     * on square devices.
     */
    static final androidx.wear.tiles.DimensionBuilders.DpProp
            PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP =
                    androidx.wear.tiles.DimensionBuilders.dp(4);

    /**
     * The default percentage for the horizontal margin for primary chip in the {@link
     * PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_HORIZONTAL_ROUND_PERCENT = 6.3f / 100;

    /**
     * The default percentage for the horizontal margin for primary chip in the {@link
     * PrimaryLayout}.
     */
    static final float PRIMARY_LAYOUT_MARGIN_HORIZONTAL_SQUARE_PERCENT = 2.8f / 100;

    /**
     * The padding for the primary chip in {@link PrimaryLayout} so it doesn't bleed off screen if
     * text is too big.
     */
    static final float PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_ROUND_DP = 30;

    /**
     * The padding for the primary chip in {@link PrimaryLayout} so it doesn't bleed off screen if
     * text is too big.
     */
    static final float PRIMARY_LAYOUT_CHIP_HORIZONTAL_PADDING_SQUARE_DP = 0;

    /** The default horizontal margin in the {@link EdgeContentLayout}. */
    static final float EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_ROUND_DP = 14;

    /** The default horizontal margin in the {@link EdgeContentLayout}. */
    static final float EDGE_CONTENT_LAYOUT_MARGIN_HORIZONTAL_SQUARE_DP = 16;

    /**
     * The recommended padding that should be above the main content (text) in the {@link
     * EdgeContentLayout}.
     */
    public static final float EDGE_CONTENT_LAYOUT_PADDING_ABOVE_MAIN_CONTENT_DP = 6;

    /**
     * The recommended padding that should be below the main content (text) in the {@link
     * EdgeContentLayout}.
     */
    public static final float EDGE_CONTENT_LAYOUT_PADDING_BELOW_MAIN_CONTENT_DP = 8;

    /** The default spacer width for slots in a {@link MultiSlotLayout}. */
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp
            MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH = androidx.wear.tiles.DimensionBuilders.dp(8);

    /** The recommended space between the main content and additional labels in layouts. */
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp
            DEFAULT_VERTICAL_SPACER_HEIGHT = androidx.wear.tiles.DimensionBuilders.dp(8);

    /** The maximum number of button that can be added to the {@link MultiButtonLayout}. */
    public static final int MULTI_BUTTON_MAX_NUMBER = 7;

    /**
     * The default size of button in case when there are 3 or more buttons in the {@link
     * MultiButtonLayout}.
     */
    static final androidx.wear.tiles.DimensionBuilders.DpProp MULTI_BUTTON_3_PLUS_SIZE =
            ButtonDefaults.DEFAULT_SIZE;

    /** The default size of button in case when there 2 buttons in the {@link MultiButtonLayout}. */
    static final androidx.wear.tiles.DimensionBuilders.DpProp MULTI_BUTTON_2_SIZE =
            ButtonDefaults.LARGE_SIZE;

    /**
     * The default size of button in case when there is 1 button in the {@link MultiButtonLayout}.
     */
    static final androidx.wear.tiles.DimensionBuilders.DpProp MULTI_BUTTON_1_SIZE =
            ButtonDefaults.EXTRA_LARGE_SIZE;

    /** The default width for vertical spacer between buttons in the {@link MultiButtonLayout}. */
    static final androidx.wear.tiles.DimensionBuilders.DpProp MULTI_BUTTON_SPACER_WIDTH =
            androidx.wear.tiles.DimensionBuilders.dp(6);

    /**
     * The default height for horizontal spacer between buttons in the {@link MultiButtonLayout}.
     */
    static final androidx.wear.tiles.DimensionBuilders.DpProp MULTI_BUTTON_SPACER_HEIGHT =
            androidx.wear.tiles.DimensionBuilders.dp(4);
}
