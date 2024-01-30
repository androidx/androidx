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

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Represent the container for default color scheme in your Tile, that can be used to create color
 * objects for all Material components.
 *
 * <p>See {@link #DEFAULT} for default color scheme.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.Colors} which provides
 *     the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class Colors {

    /** The default color used for primary elements (i.e. background color). */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ColorInt
    public static final int PRIMARY = 0xFFAECBFA;

    /** The default color used on primary elements (i.e. content color). */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ColorInt
    public static final int ON_PRIMARY = 0xFF303133;

    /** The default color used for secondary elements (i.e. background color). */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ColorInt
    public static final int SURFACE = 0xFF303133;

    /** The default color used on secondary elements (i.e. content color). */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ColorInt
    public static final int ON_SURFACE = 0xFFFFFFFF;

    /** The default color scheme to be used in Tiles Material components. */
    @NonNull
    public static final Colors DEFAULT = new Colors(PRIMARY, ON_PRIMARY, SURFACE, ON_SURFACE);

    private @ColorInt final int mPrimary;
    private @ColorInt final int mOnPrimary;
    private @ColorInt final int mSurface;
    private @ColorInt final int mOnSurface;

    /**
     * Constructor for {@link Colors} object.
     *
     * @param primary The background color to be used for primary components. Should be in ARGB
     *     format.
     * @param onPrimary The content color or tint color to be used for primary components. Should be
     *     in ARGB format.
     * @param surface The background color to be used for secondary components. Should be in ARGB
     *     format.
     * @param onSurface The content color or tint color to be used for secondary components. Should
     *     be in ARGB format.
     */
    public Colors(
            @ColorInt int primary,
            @ColorInt int onPrimary,
            @ColorInt int surface,
            @ColorInt int onSurface) {
        this.mPrimary = primary;
        this.mOnPrimary = onPrimary;
        this.mSurface = surface;
        this.mOnSurface = onSurface;
    }

    /** The primary color to be used on components. */
    @ColorInt
    public int getPrimary() {
        return mPrimary;
    }

    /** The onPrimary color to be used on components. */
    @ColorInt
    public int getOnPrimary() {
        return mOnPrimary;
    }

    /** The surface color to be used on components. */
    @ColorInt
    public int getSurface() {
        return mSurface;
    }

    /** The onSurface color to be used on components. */
    @ColorInt
    public int getOnSurface() {
        return mOnSurface;
    }
}
