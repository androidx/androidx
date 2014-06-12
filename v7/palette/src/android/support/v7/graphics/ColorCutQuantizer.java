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
import android.graphics.Color;
import android.util.SparseIntArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * An color quantizer based on the Median-cut algorithm, but optimized for picking out distinct
 * colors rather than representation colors.
 *
 * The color space is represented as a 3-dimensional cube with each dimension being an RGB
 * component. The cube is then repeatedly divided until we have reduced the color space to the
 * requested number of colors. An average color is then generated from each cube.
 *
 * What makes this different to median-cut is that median-cut divided cubes so that all of the cubes
 * have roughly the same population, where this quantizer divides boxes based on their color volume.
 * This means that the color space is divided into distinct colors, rather than representative
 * colors.
 */
final class ColorCutQuantizer {

    private static final String LOG_TAG = ColorCutQuantizer.class.getSimpleName();

    private final float[] mTempHsl = new float[3];

    private static final float BLACK_MAX_LIGHTNESS = 0.05f;
    private static final float WHITE_MIN_LIGHTNESS = 0.95f;

    private static final int COMPONENT_RED = -3;
    private static final int COMPONENT_GREEN = -2;
    private static final int COMPONENT_BLUE = -1;

    private final int[] mColors;
    private final SparseIntArray mColorPopulations;

    private final List<PaletteItem> mQuantizedColors;

    /**
     * Factory-method to generate a {@link ColorCutQuantizer} from a {@link Bitmap} object.
     *
     * @param bitmap Bitmap to extract the pixel data from
     * @param maxColors The maximum number of colors that should be in the result palette.
     */
    static ColorCutQuantizer fromBitmap(Bitmap bitmap, int maxColors) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        final int[] rgbPixels = new int[width * height];
        bitmap.getPixels(rgbPixels, 0, width, 0, 0, width, height);

        return new ColorCutQuantizer(rgbPixels, maxColors);
    }

    /**
     * Private constructor.
     *
     * @param pixels array of rgb packed ints
     * @param maxColors The maximum number of colors that should be in the result palette.
     */
    ColorCutQuantizer(int[] pixels, int maxColors) {
        final ColorHistogram colorHist = new ColorHistogram(pixels);
        final int rawColorCount = colorHist.getNumberOfColors();
        final int[] rawColors = colorHist.getColors();
        final int[] rawColorCounts = colorHist.getColorCounts();

        // First, lets pack the populations into a SparseIntArray so that they can be easily
        // retrieved without knowing a color's index
        mColorPopulations = new SparseIntArray(rawColorCount);
        for (int i = 0; i < rawColors.length; i++) {
            mColorPopulations.append(rawColors[i], rawColorCounts[i]);
        }

        // Now go through all of the colors and keep those which we do not want to ignore
        mColors = new int[rawColorCount];
        int validColorCount = 0;
        for (int color : rawColors) {
            if (!shouldIgnoreColor(color)) {
                mColors[validColorCount++] = color;
            }
        }

        if (validColorCount <= maxColors) {
            // The image has fewer colors than the maximum requested, so just return the colors
            mQuantizedColors = new ArrayList<PaletteItem>();
            for (final int color : mColors) {
                mQuantizedColors.add(new PaletteItem(color, mColorPopulations.get(color)));
            }
        } else {
            // We need use quantization to reduce the number of colors
            mQuantizedColors = quantizePixels(validColorCount - 1, maxColors);
        }
    }

    /**
     * @return the list of quantized colors
     */
    List<PaletteItem> getQuantizedColors() {
        return mQuantizedColors;
    }

    private List<PaletteItem> quantizePixels(int maxColorIndex, int maxColors) {
        // Create the priority queue which is sorted by volume descending. This means we always
        // split the largest box in the queue
        final PriorityQueue<Vbox> pq = new PriorityQueue<Vbox>(maxColors, VBOX_COMPARATOR_VOLUME);

        // To start, offer a box which contains all of the colors
        pq.offer(new Vbox(0, maxColorIndex));

        // Now go through the boxes, splitting them until we have reached maxColors or there are no
        // more boxes to split
        splitBoxes(pq, maxColors);

        // Finally, return the average colors of the color boxes
        return generateAverageColors(pq);
    }

    /**
     * Iterate through the {@link java.util.Queue}, popping
     * {@link ColorCutQuantizer.Vbox} objects from the queue
     * and splitting them. Once split, the new box and the remaining box are offered back to the
     * queue.
     *
     * @param queue {@link java.util.PriorityQueue} to poll for boxes
     * @param maxSize Maximum amount of boxes to split
     */
    private void splitBoxes(final PriorityQueue<Vbox> queue, final int maxSize) {
        while (queue.size() < maxSize) {
            final Vbox vbox = queue.poll();

            if (vbox != null && vbox.canSplit()) {
                // First split the box, and offer the result
                queue.offer(vbox.splitBox());
                // Then offer the box back
                queue.offer(vbox);
            } else {
                // If we get here then there are no more boxes to split, so return
                return;
            }
        }
    }

    private List<PaletteItem> generateAverageColors(Collection<Vbox> vboxes) {
        ArrayList<PaletteItem> colors = new ArrayList<PaletteItem>(vboxes.size());
        for (Vbox vbox : vboxes) {
            PaletteItem color = vbox.getAverageColor();
            if (!shouldIgnoreColor(color)) {
                // As we're averaging a color box, we can still get colors which we do not want, so
                // we check again here
                colors.add(color);
            }
        }
        return colors;
    }

    /**
     * Represents a tightly fitting box around a color space.
     */
    private class Vbox {
        private int lowerIndex;
        private int upperIndex;

        private int minRed, maxRed;
        private int minGreen, maxGreen;
        private int minBlue, maxBlue;

        Vbox(int lowerIndex, int upperIndex) {
            this.lowerIndex = lowerIndex;
            this.upperIndex = upperIndex;
            fitBox();
        }

        int getVolume() {
            return (maxRed - minRed + 1) * (maxGreen - minGreen + 1) * (maxBlue - minBlue + 1);
        }

        boolean canSplit() {
            return getColorCount() > 1;
        }

        int getColorCount() {
            return upperIndex - lowerIndex;
        }

        /**
         * Recomputes the boundaries of this box to tightly fit the colors within the box.
         */
        void fitBox() {
            // Reset the min and max to opposite values
            minRed = minGreen = minBlue = 0xFF;
            maxRed = maxGreen = maxBlue = 0x0;

            for (int i = lowerIndex; i <= upperIndex; i++) {
                final int color = mColors[i];
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                if (r > maxRed) {
                    maxRed = r;
                }
                if (r < minRed) {
                    minRed = r;
                }
                if (g > maxGreen) {
                    maxGreen = g;
                }
                if (g < minGreen) {
                    minGreen = g;
                }
                if (b > maxBlue) {
                    maxBlue = b;
                }
                if (b < minBlue) {
                    minBlue = b;
                }
            }
        }

        /**
         * Split this color box at the mid-point along it's longest dimension
         *
         * @return the new ColorBox
         */
        Vbox splitBox() {
            if (!canSplit()) {
                throw new IllegalStateException("Can not split a box with only 1 color");
            }

            // find median along the longest dimension
            final int splitPoint = findSplitPoint();

            Vbox newBox = new Vbox(splitPoint + 1, upperIndex);

            // Now change this box's upperIndex and recompute the color boundaries
            upperIndex = splitPoint;
            fitBox();

            return newBox;
        }

        /**
         * @return the dimension which this box is largest in
         */
        int getLongestColorDimension() {
            final int redLength = maxRed - minRed;
            final int greenLength = maxGreen - minGreen;
            final int blueLength = maxBlue - minBlue;

            if (redLength >= greenLength && redLength >= blueLength) {
                return COMPONENT_RED;
            } else if (greenLength >= redLength && greenLength >= blueLength) {
                return COMPONENT_GREEN;
            } else {
                return COMPONENT_BLUE;
            }
        }

        /**
         * Finds the point within this box's lowerIndex and upperIndex index of where to split.
         *
         * This is calculated by finding the longest color dimension, and then sorting the
         * sub-array based on that dimension value in each color. The colors are then iterated over
         * until a color is found with at least the midpoint of the whole box's dimension midpoint.
         *
         * @return the index of the colors array to split from
         */
        int findSplitPoint() {
            final int longestDimension = getLongestColorDimension();

            // We need to sort the colors in this box based on the longest color dimension.
            // As we can't use a Comparator to define the sort logic, we modify each color so that
            // it's most significant is the desired dimension
            modifySignificantOctet(longestDimension, lowerIndex, upperIndex);

            // Now sort...
            Arrays.sort(mColors, lowerIndex, upperIndex + 1);

            // Now revert all of the colors so that they are packed as RGB again
            modifySignificantOctet(longestDimension, lowerIndex, upperIndex);

            final int dimensionMidPoint = midPoint(longestDimension);

            for (int i = lowerIndex; i < upperIndex; i++)  {
                final int color = mColors[i];

                switch (longestDimension) {
                    case COMPONENT_RED:
                        if (Color.red(color) >= dimensionMidPoint) {
                            return i;
                        }
                        break;
                    case COMPONENT_GREEN:
                        if (Color.green(color) >= dimensionMidPoint) {
                            return i;
                        }
                        break;
                    case COMPONENT_BLUE:
                        if (Color.blue(color) > dimensionMidPoint) {
                            return i;
                        }
                        break;
                }
            }

            return lowerIndex;
        }

        /**
         * @return the average color of this box.
         */
        PaletteItem getAverageColor() {
            int redSum = 0;
            int greenSum = 0;
            int blueSum = 0;
            int totalPopulation = 0;

            for (int i = lowerIndex; i <= upperIndex; i++) {
                final int color = mColors[i];
                final int colorPopulation = mColorPopulations.get(color);

                totalPopulation += colorPopulation;
                redSum += colorPopulation * Color.red(color);
                greenSum += colorPopulation * Color.green(color);
                blueSum += colorPopulation * Color.blue(color);
            }

            final int redAverage = Math.round(redSum / (float) totalPopulation);
            final int greenAverage = Math.round(greenSum / (float) totalPopulation);
            final int blueAverage = Math.round(blueSum / (float) totalPopulation);

            return new PaletteItem(redAverage, greenAverage, blueAverage, totalPopulation);
        }

        /**
         * @return the midpoint of this box in the given {@code dimension}
         */
        int midPoint(int dimension) {
            switch (dimension) {
                case COMPONENT_RED:
                default:
                    return (minRed + maxRed) / 2;
                case COMPONENT_GREEN:
                    return (minGreen + maxGreen) / 2;
                case COMPONENT_BLUE:
                    return (minBlue + maxBlue) / 2;
            }
        }
    }

    /**
     * Modify the significant octet in a packed color int. Allows sorting based on the value of a
     * single color component.
     *
     * @see Vbox#findSplitPoint()
     */
    private void modifySignificantOctet(final int dimension, int lowIndex, int highIndex) {
        switch (dimension) {
            case COMPONENT_RED:
                // Already in RGB, no need to do anything
                break;
            case COMPONENT_GREEN:
                // We need to do a RGB to GRB swap, or vice-versa
                for (int i = lowIndex; i <= highIndex; i++) {
                    final int color = mColors[i];
                    mColors[i] = Color.rgb((color >> 8) & 0xFF, (color >> 16) & 0xFF, color & 0xFF);
                }
                break;
            case COMPONENT_BLUE:
                // We need to do a RGB to BGR swap, or vice-versa
                for (int i = lowIndex; i <= highIndex; i++) {
                    final int color = mColors[i];
                    mColors[i] = Color.rgb(color & 0xFF, (color >> 8) & 0xFF, (color >> 16) & 0xFF);
                }
                break;
        }
    }

    private boolean shouldIgnoreColor(int color) {
        ColorUtils.RGBtoHSL(Color.red(color), Color.green(color), Color.blue(color), mTempHsl);
        return shouldIgnoreColor(mTempHsl);
    }

    private static boolean shouldIgnoreColor(PaletteItem color) {
        return shouldIgnoreColor(color.getHsl());
    }

    private static boolean shouldIgnoreColor(float[] hslColor) {
        return isWhite(hslColor) || isBlack(hslColor) || isNearRedILine(hslColor);
    }

    /**
     * @return true if the color represents a color which is close to black.
     */
    private static boolean isBlack(float[] hslColor) {
        return hslColor[2] <= BLACK_MAX_LIGHTNESS;
    }

    /**
     * @return true if the color represents a color which is close to white.
     */
    private static boolean isWhite(float[] hslColor) {
        return hslColor[2] >= WHITE_MIN_LIGHTNESS;
    }

    /**
     * @return true if the color lies close to the red side of the I line.
     */
    private static boolean isNearRedILine(float[] hslColor) {
        return hslColor[0] >= 10f && hslColor[0] <= 37f && hslColor[1] <= 0.82f;
    }

    /**
     * Comparator which sorts {@link Vbox} instances based on their volume, in descending order
     */
    private static final Comparator<Vbox> VBOX_COMPARATOR_VOLUME = new Comparator<Vbox>() {
        @Override
        public int compare(Vbox lhs, Vbox rhs) {
            return rhs.getVolume() - lhs.getVolume();
        }
    };

}
