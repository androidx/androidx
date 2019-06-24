/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.graphics;

import android.graphics.BlendMode;
import android.graphics.PorterDuff;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Utility class used to map BlendModeCompat parameters to the corresponding
 * PorterDuff mode or BlendMode depending on the API level of the platform
 */
/* package */ class BlendModeUtils {

    @RequiresApi(29)
    /* package */ static @Nullable BlendMode obtainBlendModeFromCompat(
            @NonNull BlendModeCompat blendModeCompat) {
        switch (blendModeCompat) {
            case CLEAR:
                return BlendMode.CLEAR;
            case SRC:
                return BlendMode.SRC;
            case DST:
                return BlendMode.DST;
            case SRC_OVER:
                return BlendMode.SRC_OVER;
            case DST_OVER:
                return BlendMode.DST_OVER;
            case SRC_IN:
                return BlendMode.SRC_IN;
            case DST_IN:
                return BlendMode.DST_IN;
            case SRC_OUT:
                return BlendMode.SRC_OUT;
            case DST_OUT:
                return BlendMode.DST_OUT;
            case SRC_ATOP:
                return BlendMode.SRC_ATOP;
            case DST_ATOP:
                return BlendMode.DST_ATOP;
            case XOR:
                return BlendMode.XOR;
            case PLUS:
                return BlendMode.PLUS;
            case MODULATE:
                return BlendMode.MODULATE;
            case SCREEN:
                return BlendMode.SCREEN;
            case OVERLAY:
                return BlendMode.OVERLAY;
            case DARKEN:
                return BlendMode.DARKEN;
            case LIGHTEN:
                return BlendMode.LIGHTEN;
            case COLOR_DODGE:
                return BlendMode.COLOR_DODGE;
            case COLOR_BURN:
                return BlendMode.COLOR_BURN;
            case HARD_LIGHT:
                return BlendMode.HARD_LIGHT;
            case SOFT_LIGHT:
                return BlendMode.SOFT_LIGHT;
            case DIFFERENCE:
                return BlendMode.DIFFERENCE;
            case EXCLUSION:
                return BlendMode.EXCLUSION;
            case MULTIPLY:
                return BlendMode.MULTIPLY;
            case HUE:
                return BlendMode.HUE;
            case SATURATION:
                return BlendMode.SATURATION;
            case COLOR:
                return BlendMode.COLOR;
            case LUMINOSITY:
                return BlendMode.LUMINOSITY;
            default:
                return null;
        }
    }

    /* package */ static @Nullable PorterDuff.Mode obtainPorterDuffFromCompat(
            @Nullable BlendModeCompat blendModeCompat) {
        if (blendModeCompat != null) {
            switch (blendModeCompat) {
                case CLEAR:
                    return PorterDuff.Mode.CLEAR;
                case SRC:
                    return PorterDuff.Mode.SRC;
                case DST:
                    return PorterDuff.Mode.DST;
                case SRC_OVER:
                    return PorterDuff.Mode.SRC_OVER;
                case DST_OVER:
                    return PorterDuff.Mode.DST_OVER;
                case SRC_IN:
                    return PorterDuff.Mode.SRC_IN;
                case DST_IN:
                    return PorterDuff.Mode.DST_IN;
                case SRC_OUT:
                    return PorterDuff.Mode.SRC_OUT;
                case DST_OUT:
                    return PorterDuff.Mode.DST_OUT;
                case SRC_ATOP:
                    return PorterDuff.Mode.SRC_ATOP;
                case DST_ATOP:
                    return PorterDuff.Mode.DST_ATOP;
                case XOR:
                    return PorterDuff.Mode.XOR;
                case PLUS:
                    return PorterDuff.Mode.ADD;
                // b/73224934 PorterDuff Multiply maps to Skia Modulate
                case MODULATE:
                    return PorterDuff.Mode.MULTIPLY;
                case SCREEN:
                    return PorterDuff.Mode.SCREEN;
                case OVERLAY:
                    return PorterDuff.Mode.OVERLAY;
                case DARKEN:
                    return PorterDuff.Mode.DARKEN;
                case LIGHTEN:
                    return PorterDuff.Mode.LIGHTEN;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private BlendModeUtils() { }
}
