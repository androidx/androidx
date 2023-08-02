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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.ColorBuilders.argb;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.wear.protolayout.ColorBuilders.ColorProp;

/**
 * Represents the indicator and track colors used in {@link CircularProgressIndicator}.
 *
 * <p>See {@link ProgressIndicatorDefaults#DEFAULT_COLORS} for the default colors used in a {@link
 * CircularProgressIndicator}.
 */
public class ProgressIndicatorColors {
    @NonNull private final ColorProp mIndicatorColor;
    @NonNull private final ColorProp mTrackColor;

    /**
     * Constructor for {@link ProgressIndicatorColors} object.
     *
     * @param indicatorColor The indicator color to be used for {@link CircularProgressIndicator}.
     * @param trackColor The background track color to be used for {@link
     *     CircularProgressIndicator}.
     */
    public ProgressIndicatorColors(
            @NonNull ColorProp indicatorColor, @NonNull ColorProp trackColor) {
        this.mIndicatorColor = indicatorColor;
        this.mTrackColor = trackColor;
    }

    /**
     * Constructor for {@link ProgressIndicatorColors} object.
     *
     * @param indicatorColor The indicator color to be used for {@link CircularProgressIndicator}.
     *     Should be in ARGB format.
     * @param trackColor The background track color to be used for {@link
     *     CircularProgressIndicator}. Should be in ARGB format.
     */
    public ProgressIndicatorColors(@ColorInt int indicatorColor, @ColorInt int trackColor) {
        this.mIndicatorColor = argb(indicatorColor);
        this.mTrackColor = argb(trackColor);
    }

    /**
     * Returns a {@link ProgressIndicatorColors} object, using the current Primary color for
     * indicator color and the current Surface color for the track color from the given {@link
     * Colors}.
     */
    @NonNull
    public static ProgressIndicatorColors progressIndicatorColors(@NonNull Colors colors) {
        return new ProgressIndicatorColors(colors.getPrimary(), colors.getSurface());
    }

    /** The indicator color to be used for {@link CircularProgressIndicator}. */
    @NonNull
    public ColorProp getIndicatorColor() {
        return mIndicatorColor;
    }

    /** The background track color to be used for {@link CircularProgressIndicator}. */
    @NonNull
    public ColorProp getTrackColor() {
        return mTrackColor;
    }
}
