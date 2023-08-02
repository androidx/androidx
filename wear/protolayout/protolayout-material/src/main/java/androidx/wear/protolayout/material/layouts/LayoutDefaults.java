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

package androidx.wear.protolayout.material.layouts;

import static androidx.wear.protolayout.DimensionBuilders.dp;

import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.material.ButtonDefaults;

/** Contains the default values used by layout templates for ProtoLayout. */
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
    static final DpProp PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_ROUND_DP = dp(0);

    /**
     * The default spacer above primary label in {@link PrimaryLayout} to make space for Tile icon
     * on square devices.
     */
    static final DpProp PRIMARY_LAYOUT_PRIMARY_LABEL_SPACER_HEIGHT_SQUARE_DP = dp(4);

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
    public static final DpProp MULTI_SLOT_LAYOUT_HORIZONTAL_SPACER_WIDTH = dp(8);

    /** The recommended space between the main content and additional labels in layouts. */
    public static final DpProp DEFAULT_VERTICAL_SPACER_HEIGHT = dp(8);

    /**
     * The maximum number of button that can be added to the {@link MultiButtonLayout}.
     *
     * @deprecated Use {@link MultiButtonLayoutDefaults#MAX_BUTTONS} instead.
     */
    @Deprecated public static final int MULTI_BUTTON_MAX_NUMBER = 7;

    /** Contains default values used for {@link MultiButtonLayout}. */
    public static final class MultiButtonLayoutDefaults {
        private MultiButtonLayoutDefaults() {}

        /** The maximum number of button that can be added to the {@link MultiButtonLayout}. */
        @SuppressWarnings("MinMaxConstant")
        public static final int MAX_BUTTONS = 7;

        /**
         * The default size of button in case when there are 3 or more buttons in the {@link
         * MultiButtonLayout}.
         */
        public static final DpProp BUTTON_SIZE_FOR_3_PLUS_BUTTONS = ButtonDefaults.DEFAULT_SIZE;

        /**
         * The default size of button in case when there are 2 buttons in the {@link
         * MultiButtonLayout}.
         */
        public static final DpProp BUTTON_SIZE_FOR_2_BUTTONS = ButtonDefaults.LARGE_SIZE;

        /**
         * The default size of button in case when there is 1 button in the {@link
         * MultiButtonLayout}.
         */
        public static final DpProp BUTTON_SIZE_FOR_1_BUTTON = ButtonDefaults.EXTRA_LARGE_SIZE;

        /**
         * The default width for vertical spacer between buttons in the {@link MultiButtonLayout}.
         */
        static final DpProp SPACER_WIDTH = dp(6);

        /**
         * The default height for horizontal spacer between buttons in the {@link
         * MultiButtonLayout}.
         */
        static final DpProp SPACER_HEIGHT = dp(4);
    }
}
