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

import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.util.Collections;
import java.util.List;

/**
 * A helper class to extract prominent colors from an image.
 * <p>
 * A number of colors with different profiles are extracted from the image:
 * <ul>
 *     <li>Vibrant</li>
 *     <li>Vibrant Dark</li>
 *     <li>Vibrant Light</li>
 *     <li>Muted</li>
 *     <li>Muted Dark</li>
 *     <li>Muted Light</li>
 * </ul>
 * These can be retrieved from the appropriate getter method.
 *
 * <p>
 * Instances can be created with the synchronous factory methods {@link #generate(Bitmap)} and
 * {@link #generate(Bitmap, int)}.
 * <p>
 * These should be called on a background thread, ideally the one in
 * which you load your images on. Sometimes that is not possible, so asynchronous factory methods
 * have also been provided: {@link #generateAsync(Bitmap, PaletteAsyncListener)} and
 * {@link #generateAsync(Bitmap, int, PaletteAsyncListener)}. These can be used as so:
 *
 * <pre>
 * Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
 *     @Override
 *     public void onGenerated(Palette palette) {
 *         // Do something with colors...
 *     }
 * });
 * </pre>
 */
public final class Palette {

    /**
     * Listener to be used with {@link #generateAsync(Bitmap, PaletteAsyncListener)} or
     * {@link #generateAsync(Bitmap, int, PaletteAsyncListener)}
     */
    public interface PaletteAsyncListener {

        /**
         * Called when the {@link Palette} has been generated.
         */
        void onGenerated(Palette palette);
    }

    private static final int CALCULATE_BITMAP_MIN_DIMENSION = 100;
    private static final int DEFAULT_CALCULATE_NUMBER_COLORS = 16;

    private static final float TARGET_DARK_LUMA = 0.26f;
    private static final float MAX_DARK_LUMA = 0.45f;

    private static final float MIN_LIGHT_LUMA = 0.55f;
    private static final float TARGET_LIGHT_LUMA = 0.74f;

    private static final float MIN_NORMAL_LUMA = 0.3f;
    private static final float TARGET_NORMAL_LUMA = 0.5f;
    private static final float MAX_NORMAL_LUMA = 0.7f;

    private static final float TARGET_MUTED_SATURATION = 0.3f;
    private static final float MAX_MUTED_SATURATION = 0.4f;

    private static final float TARGET_VIBRANT_SATURATION = 1f;
    private static final float MIN_VIBRANT_SATURATION = 0.35f;

    private final List<PaletteItem> mPallete;
    private final int mHighestPopulation;

    private PaletteItem mVibrantColor;
    private PaletteItem mMutedColor;

    private PaletteItem mDarkVibrantColor;
    private PaletteItem mDarkMutedColor;

    private PaletteItem mLightVibrantColor;
    private PaletteItem mLightMutedColor;

    /**
     * Generate a {@link Palette} from a {@link Bitmap} using the default number of colors.
     */
    public static Palette generate(Bitmap bitmap) {
        return generate(bitmap, DEFAULT_CALCULATE_NUMBER_COLORS);
    }

    /**
     * Generate a {@link Palette} from a {@link Bitmap} using the specified {@code numColors}.
     * Good values for {@code numColors} depend on the source image type.
     * For landscapes, a good values are in the range 12-16. For images which are largely made up
     * of people's faces then this value should be increased to 24-32.
     *
     * @param numColors The maximum number of colors in the generated palette. Increasing this
     *                  number will increase the time needed to compute the values.
     */
    public static Palette generate(Bitmap bitmap, int numColors) {
        if (bitmap == null) {
            throw new IllegalArgumentException("bitmap can not be null");
        }

        // First we'll scale down the bitmap so it's shortest dimension is 100px
        final Bitmap scaledBitmap = scaleBitmapDown(bitmap);

        // Now generate a quantizer from the Bitmap
        ColorCutQuantizer quantizer = ColorCutQuantizer.fromBitmap(scaledBitmap, numColors);

        // If created a new bitmap, recycle it
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }

        // Now return a ColorExtractor instance
        return new Palette(quantizer.getQuantizedColors());
    }

    /**
     * Generate a {@link Palette} asynchronously. {@link PaletteAsyncListener#onGenerated(Palette)}
     * will be called with the created instance. The resulting {@link Palette} is the same as
     * what would be created by calling {@link #generate(Bitmap)}.
     *
     * @param listener Listener to be invoked when the {@link Palette} has been generated.
     *
     * @return the {@link android.os.AsyncTask} used to asynchronously generate the instance.
     */
    public static AsyncTask<Void, Void, Palette> generateAsync(
            Bitmap bitmap, PaletteAsyncListener listener) {
        return generateAsync(bitmap, DEFAULT_CALCULATE_NUMBER_COLORS, listener);
    }

    /**
     * Generate a {@link Palette} asynchronously. {@link PaletteAsyncListener#onGenerated(Palette)}
     * will be called with the created instance. The resulting {@link Palette} is the same as what
     * would be created by calling {@link #generate(Bitmap, int)}.
     *
     * @param listener Listener to be invoked when the {@link Palette} has been generated.
     *
     * @return the {@link android.os.AsyncTask} used to asynchronously generate the instance.
     */
    public static AsyncTask<Void, Void, Palette> generateAsync(
            final Bitmap bitmap, final int numColors, final PaletteAsyncListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }

        AsyncTask<Void, Void, Palette> task = new AsyncTask<Void, Void, Palette>() {
            @Override
            protected Palette doInBackground(Void... voids) {
                return generate(bitmap, numColors);
            }

            @Override
            protected void onPostExecute(Palette colorExtractor) {
                super.onPostExecute(colorExtractor);
                listener.onGenerated(colorExtractor);
            }
        };
        task.execute();
        return task;
    }

    private Palette(List<PaletteItem> palette) {
        mPallete = palette;
        mHighestPopulation = findMaxPopulation();

        mVibrantColor = findColor(TARGET_NORMAL_LUMA, MIN_NORMAL_LUMA, MAX_NORMAL_LUMA,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mLightVibrantColor = findColor(TARGET_LIGHT_LUMA, MIN_LIGHT_LUMA, 1f,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mDarkVibrantColor = findColor(TARGET_DARK_LUMA, 0f, MAX_DARK_LUMA,
                TARGET_VIBRANT_SATURATION, MIN_VIBRANT_SATURATION, 1f);

        mMutedColor = findColor(TARGET_NORMAL_LUMA, MIN_NORMAL_LUMA, MAX_NORMAL_LUMA,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        mLightMutedColor = findColor(TARGET_LIGHT_LUMA, MIN_LIGHT_LUMA, 1f,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        mDarkMutedColor = findColor(TARGET_DARK_LUMA, 0f, MAX_DARK_LUMA,
                TARGET_MUTED_SATURATION, 0f, MAX_MUTED_SATURATION);

        // Now try and generate any missing colors
        generateEmptyColors();
    }

    /**
     * The total palette of colors which make up the image.
     */
    public List<PaletteItem> getPallete() {
        return Collections.unmodifiableList(mPallete);
    }

    /**
     * Returns the most vibrant color in the image. Might be null.
     */
    public PaletteItem getVibrantColor() {
        return mVibrantColor;
    }

    /**
     * Returns a light and vibrant color from the image. Might be null.
     */
    public PaletteItem getLightVibrantColor() {
        return mLightVibrantColor;
    }

    /**
     * Returns a dark and vibrant color from the image. Might be null.
     */
    public PaletteItem getDarkVibrantColor() {
        return mDarkVibrantColor;
    }

    /**
     * Returns a muted color from the image. Might be null.
     */
    public PaletteItem getMutedColor() {
        return mMutedColor;
    }

    /**
     * Returns a muted and light color from the image. Might be null.
     */
    public PaletteItem getLightMutedColor() {
        return mLightMutedColor;
    }

    /**
     * Returns a muted and dark color from the image. Might be null.
     */
    public PaletteItem getDarkMutedColor() {
        return mDarkMutedColor;
    }

    /**
     * @return true if we have already selected {@code item}
     */
    private boolean isAlreadySelected(PaletteItem item) {
        return mVibrantColor == item || mDarkVibrantColor == item || mLightVibrantColor == item ||
                mMutedColor == item || mDarkMutedColor == item || mLightMutedColor == item;
    }

    private PaletteItem findColor(float targetLuma, float minLuma, float maxLuma,
                                float targetSaturation, float minSaturation, float maxSaturation) {
        PaletteItem max = null;
        float maxValue = 0f;

        for (PaletteItem paletteItem : mPallete) {
            final float sat = paletteItem.getHsl()[1];
            final float luma = paletteItem.getHsl()[2];

            if (sat >= minSaturation && sat <= maxSaturation &&
                    luma >= minLuma && luma <= maxLuma &&
                    !isAlreadySelected(paletteItem)) {
                float thisValue = createComparisonValue(sat, targetSaturation, luma, targetLuma,
                        paletteItem.getPopulation(), mHighestPopulation);
                if (max == null || thisValue > maxValue) {
                    max = paletteItem;
                    maxValue = thisValue;
                }
            }
        }

        return max;
    }

    /**
     * Try and generate any missing colors from the colors we did find.
     */
    private void generateEmptyColors() {
        if (mVibrantColor == null) {
            // If we do not have a vibrant color...
            if (mDarkVibrantColor != null) {
                // ...but we do have a dark vibrant, generate the value by modifying the luma
                final float[] newHsl = copyHslValues(mDarkVibrantColor);
                newHsl[2] = TARGET_NORMAL_LUMA;
                mVibrantColor = new PaletteItem(ColorUtils.HSLtoRGB(newHsl), 0);
            }
        }

        if (mDarkVibrantColor == null) {
            // If we do not have a dark vibrant color...
            if (mVibrantColor != null) {
                // ...but we do have a vibrant, generate the value by modifying the luma
                final float[] newHsl = copyHslValues(mVibrantColor);
                newHsl[2] = TARGET_DARK_LUMA;
                mDarkVibrantColor = new PaletteItem(ColorUtils.HSLtoRGB(newHsl), 0);
            }
        }
    }

    /**
     * Find the {@link PaletteItem} with the highest population value and return the population.
     */
    private int findMaxPopulation() {
        int population = 0;
        for (PaletteItem item : mPallete) {
            population = Math.max(population, item.getPopulation());
        }
        return population;
    }

    /**
     * Scale the bitmap down so that it's smallest dimension is
     * {@value #CALCULATE_BITMAP_MIN_DIMENSION}px. If {@code bitmap} is smaller than this, than it
     * is returned.
     */
    private static Bitmap scaleBitmapDown(Bitmap bitmap) {
        final int minDimension = Math.min(bitmap.getWidth(), bitmap.getHeight());

        if (minDimension <= CALCULATE_BITMAP_MIN_DIMENSION) {
            // If the bitmap is small enough already, just return it
            return bitmap;
        }

        final float scaleRatio = CALCULATE_BITMAP_MIN_DIMENSION / (float) minDimension;
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(bitmap.getWidth() * scaleRatio),
                Math.round(bitmap.getHeight() * scaleRatio),
                false);
    }

    private static float createComparisonValue(float saturation, float targetSaturation,
            float luma, float targetLuma,
            int population, int highestPopulation) {
        return weightedMean(
                invertDiff(saturation, targetSaturation), 3f,
                invertDiff(luma, targetLuma), 6.5f,
                population / (float) highestPopulation, 0.5f
        );
    }

    /**
     * Copy a {@link PaletteItem}'s HSL values into a new float[].
     */
    private static float[] copyHslValues(PaletteItem color) {
        final float[] newHsl = new float[3];
        System.arraycopy(color.getHsl(), 0, newHsl, 0, 3);
        return newHsl;
    }

    /**
     * Returns a value in the range 0-1. 1 is returned when {@code value} equals the
     * {@code targetValue} and then decreases as the absolute difference between {@code value} and
     * {@code targetValue} increases.
     *
     * @param value the item's value
     * @param targetValue the value which we desire
     */
    private static float invertDiff(float value, float targetValue) {
        return 1f - Math.abs(value - targetValue);
    }

    private static float weightedMean(float... values) {
        float sum = 0f;
        float sumWeight = 0f;

        for (int i = 0; i < values.length; i += 2) {
            float value = values[i];
            float weight = values[i + 1];

            sum += (value * weight);
            sumWeight += weight;
        }

        return sum / sumWeight;
    }

}
