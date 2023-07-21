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

package androidx.wear.tiles.material;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Contains the default values used by chip Tiles components.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.ChipDefaults} which
 *     provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class ChipDefaults {
    private ChipDefaults() {}

    /** The default height for standard {@link Chip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp DEFAULT_HEIGHT =
            androidx.wear.tiles.DimensionBuilders.dp(52);

    /** The default height for standard {@link CompactChip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp COMPACT_HEIGHT =
            androidx.wear.tiles.DimensionBuilders.dp(32);

    /** The default height of tappable area for standard {@link CompactChip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp COMPACT_HEIGHT_TAPPABLE =
            androidx.wear.tiles.DimensionBuilders.dp(48);

    /** The default height for standard {@link TitleChip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp TITLE_HEIGHT =
            androidx.wear.tiles.DimensionBuilders.dp(60);

    /** The recommended horizontal margin used for width for standard {@link Chip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final float DEFAULT_MARGIN_PERCENT = 5.2f;

    /** The recommended horizontal padding for standard {@link Chip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp HORIZONTAL_PADDING =
            androidx.wear.tiles.DimensionBuilders.dp(14);

    /** The recommended horizontal padding for standard {@link CompactChip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp COMPACT_HORIZONTAL_PADDING =
            androidx.wear.tiles.DimensionBuilders.dp(12);

    /** The recommended horizontal padding for standard {@link TitleChip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp TITLE_HORIZONTAL_PADDING =
            androidx.wear.tiles.DimensionBuilders.dp(16);

    /** The recommended vertical space between icon and text in standard {@link Chip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp ICON_SPACER_WIDTH =
            androidx.wear.tiles.DimensionBuilders.dp(6);

    /** The icon size used in standard {@link Chip} */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static final androidx.wear.tiles.DimensionBuilders.DpProp ICON_SIZE =
            androidx.wear.tiles.DimensionBuilders.dp(24);

    /** The recommended colors for a primary {@link Chip}. */
    @NonNull
    public static final ChipColors PRIMARY_COLORS = ChipColors.primaryChipColors(Colors.DEFAULT);

    /** The recommended colors for a secondary {@link Chip}. */
    @NonNull
    public static final ChipColors SECONDARY_COLORS =
            ChipColors.secondaryChipColors(Colors.DEFAULT);

    /** The recommended colors for a primary {@link CompactChip}. */
    @NonNull
    public static final ChipColors COMPACT_PRIMARY_COLORS =
            ChipColors.primaryChipColors(Colors.DEFAULT);

    /** The recommended colors for a secondary {@link CompactChip}. */
    @NonNull
    public static final ChipColors COMPACT_SECONDARY_COLORS =
            ChipColors.secondaryChipColors(Colors.DEFAULT);

    /** The recommended colors for a primary {@link TitleChip}. */
    @NonNull
    public static final ChipColors TITLE_PRIMARY_COLORS =
            ChipColors.primaryChipColors(Colors.DEFAULT);

    /** The recommended colors for a secondary {@link TitleChip}. */
    @NonNull
    public static final ChipColors TITLE_SECONDARY_COLORS =
            ChipColors.secondaryChipColors(Colors.DEFAULT);
}
