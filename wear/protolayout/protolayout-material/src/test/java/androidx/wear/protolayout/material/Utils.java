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

package androidx.wear.protolayout.material;

import androidx.annotation.Dimension;

public final class Utils {
    /** Returns true if the given ChipColors have the same colored content. */
    static boolean areChipColorsEqual(ChipColors colors1, ChipColors colors2) {
        return colors1.getBackgroundColor().getArgb() == colors2.getBackgroundColor().getArgb()
                && colors1.getContentColor().getArgb() == colors2.getContentColor().getArgb()
                && colors1.getSecondaryContentColor().getArgb()
                        == colors2.getSecondaryContentColor().getArgb()
                && colors1.getIconColor().getArgb() == colors2.getIconColor().getArgb();
    }

    @Dimension(unit = Dimension.DP)
    public static int pxToDp(int px, float scale) {
        return (int) ((px - 0.5f) / scale);
    }

    private Utils() {}
}
