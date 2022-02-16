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

import static androidx.wear.tiles.DimensionBuilders.dp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DeviceParametersBuilders;
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.tiles.DimensionBuilders.DpProp;

/**
 * Helper class used for Tiles Material.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class Helper {
    private Helper() {}

    /**
     * Returns given value if not null or throws {@code NullPointerException} otherwise.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static <T> T checkNotNull(@Nullable T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    /** Returns radius in {@link DpProp} of the given diameter. */
    static DpProp radiusOf(DpProp diameter) {
        return dp(diameter.getValue() / 2);
    }

    /**
     * Returns true if the given DeviceParameters belong to the round screen device.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static boolean isRoundDevice(@NonNull DeviceParameters deviceParameters) {
        return deviceParameters.getScreenShape() == DeviceParametersBuilders.SCREEN_SHAPE_ROUND;
    }
}
