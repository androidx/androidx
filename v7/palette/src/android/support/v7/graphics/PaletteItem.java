/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.graphics;

import android.graphics.Color;

import java.util.Arrays;

/**
 * Represents a color generated from an image's palette. The RGB color can be retrieved by
 * calling {@link #getRgb()}.
 */
public final class PaletteItem {

    final int red, green, blue;
    final int rgb;
    final int population;

    private float[] hsl;

    PaletteItem(int rgbColor, int population) {
        this.red = Color.red(rgbColor);
        this.green = Color.green(rgbColor);
        this.blue = Color.blue(rgbColor);
        this.rgb = rgbColor;
        this.population = population;
    }

    PaletteItem(int red, int green, int blue, int population) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.rgb = Color.rgb(red, green, blue);
        this.population = population;
    }

    /**
     * @return this item's RGB color value
     */
    public int getRgb() {
        return rgb;
    }

    /**
     * Return this item's HSL values.
     *     hsv[0] is Hue [0 .. 360)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Lightness [0...1]
     */
    public float[] getHsl() {
        if (hsl == null) {
            // Lazily generate HSL values from RGB
            hsl = new float[3];
            ColorUtils.RGBtoHSL(red, green, blue, hsl);
        }
        return hsl;
    }

    /**
     * @return the number of pixels represented by this color
     */
    int getPopulation() {
        return population;
    }

    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append(" ")
                .append("[").append(Integer.toHexString(getRgb())).append(']')
                .append("[HSL: ").append(Arrays.toString(getHsl())).append(']')
                .append("[Population: ").append(population).append(']').toString();
    }
}
