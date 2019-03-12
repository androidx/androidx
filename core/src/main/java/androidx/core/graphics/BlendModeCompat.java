/*
 * Copyright (C) 2019 The Android Open Source Project
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

/**
 * Compat version of {@link android.graphics.BlendMode}, usages of {@link BlendModeCompat} will
 * map to {@link android.graphics.PorterDuff.Mode} wherever possible
 */
public enum BlendModeCompat {

    /**
     * @see android.graphics.BlendMode#CLEAR
     */
    CLEAR,

    /**
     * @see android.graphics.BlendMode#SRC
     */
    SRC,

    /**
     * @see android.graphics.BlendMode#DST
     */
    DST,

    /**
     * @see android.graphics.BlendMode#SRC_OVER
     */
    SRC_OVER,

    /**
     * @see android.graphics.BlendMode#DST_OVER
     */
    DST_OVER,

    /**
     * @see android.graphics.BlendMode#SRC_IN
     */
    SRC_IN,

    /**
     * @see android.graphics.BlendMode#DST_IN
     */
    DST_IN,

    /**
     * @see android.graphics.BlendMode#SRC_OUT
     */
    SRC_OUT,

    /**
     * @see android.graphics.BlendMode#DST_OUT
     */
    DST_OUT,

    /**
     * @see android.graphics.BlendMode#SRC_ATOP
     */
    SRC_ATOP,

    /**
     * @see android.graphics.BlendMode#DST_ATOP
     */
    DST_ATOP,

    /**
     * @see android.graphics.BlendMode#XOR
     */
    XOR,

    /**
     * @see android.graphics.BlendMode#PLUS
     */
    PLUS,

    /**
     * @see android.graphics.BlendMode#MODULATE
     */
    MODULATE,

    /**
     * @see android.graphics.BlendMode#SCREEN
     */
    SCREEN,

    /**
     * @see android.graphics.BlendMode#OVERLAY
     */
    OVERLAY,

    /**
     * @see android.graphics.BlendMode#DARKEN
     */
    DARKEN,

    /**
     * @see android.graphics.BlendMode#LIGHTEN
     */
    LIGHTEN,

    /**
     * @see android.graphics.BlendMode#COLOR_DODGE
     */
    COLOR_DODGE,

    /**
     * @see android.graphics.BlendMode#COLOR_BURN
     */
    COLOR_BURN,

    /**
     * @see android.graphics.BlendMode#HARD_LIGHT
     */
    HARD_LIGHT,

    /**
     * @see android.graphics.BlendMode#SOFT_LIGHT
     */
    SOFT_LIGHT,

    /**
     * @see android.graphics.BlendMode#DIFFERENCE
     */
    DIFFERENCE,

    /**
     * @see android.graphics.BlendMode#EXCLUSION
     */
    EXCLUSION,

    /**
     * @see android.graphics.BlendMode#MULTIPLY
     */
    MULTIPLY,

    /**
     * @see android.graphics.BlendMode#HUE
     */
    HUE,

    /**
     * @see android.graphics.BlendMode#SATURATION
     */
    SATURATION,

    /**
     * @see android.graphics.BlendMode#COLOR
     */
    COLOR,

    /**
     * @see android.graphics.BlendMode#LUMINOSITY
     */
    LUMINOSITY
}
