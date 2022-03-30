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

import static androidx.annotation.Dimension.DP;
import static androidx.wear.tiles.DimensionBuilders.dp;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.wear.tiles.DimensionBuilders.DpProp;

/** Contains the default values used by button Tiles components. */
public class ButtonDefaults {
    private ButtonDefaults() {}

    /** The default size for standard {@link Button}. */
    @NonNull public static final DpProp DEFAULT_BUTTON_SIZE = dp(52);

    /** The recommended size for large {@link Button}. */
    @NonNull public static final DpProp LARGE_BUTTON_SIZE = dp(60);

    /** The recommended size for extra large {@link Button}. */
    @NonNull public static final DpProp EXTRA_LARGE_BUTTON_SIZE = dp(88);

    /** Returns the recommended icon size for the given size of a {@link Button}. */
    @NonNull
    public static DpProp recommendedIconSize(@NonNull DpProp buttonSize) {
        return recommendedIconSize(buttonSize.getValue());
    }

    /** Returns the recommended icon size for the given size of a {@link Button}. */
    @NonNull
    public static DpProp recommendedIconSize(@Dimension(unit = DP) float buttonSize) {
        return dp(buttonSize / 2);
    }

    /** The recommended colors for a primary {@link Button}. */
    @NonNull
    public static final ButtonColors PRIMARY_BUTTON_COLORS =
            ButtonColors.primaryButtonColors(Colors.DEFAULT);

    /** The recommended colors for a secondary {@link Button}. */
    @NonNull
    public static final ButtonColors SECONDARY_BUTTON_COLORS =
            ButtonColors.secondaryButtonColors(Colors.DEFAULT);
}
