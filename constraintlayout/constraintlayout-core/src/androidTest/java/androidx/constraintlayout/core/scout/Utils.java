/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.constraintlayout.core.scout;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Guideline;

import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Simple Utilities used by the Inference system
 */
public class Utils {
    private static DecimalFormat sDF = new DecimalFormat("0.0#####");

    /**
     * Calculate the maximum of an array
     *
     * @return the index of the maximum
     */
    static int max(float[] array) {
        int max = 0;
        float val = array[0];
        for (int i = 1; i < array.length; i++) {
            if (val < array[i]) {
                max = i;
                val = array[i];
            }
        }
        return max;
    }

    /**
     * Calculate the maximum of a 2D array
     *
     * @param result the index of the maximum filled by the function
     * @return the value of the maximum probabilities
     */
    static float max(float[][] array, int[] result) {
        int max1 = 0;
        int max2 = 0;
        float val = array[max1][max2];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                if (val < array[i][j]) {
                    max1 = i;
                    max2 = j;
                    val = array[max1][max2];
                }
            }
        }
        result[0] = max1;
        result[1] = max2;
        return val;
    }

    /**
     * convert an array of floats to fixed length strings
     */
    static String toS(float[] a) {
        String s = "[";
        if (a == null) {
            return "[null]";
        }
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                s += " , ";
            }
            String t = sDF.format(a[i]) + "       ";
            s += t.substring(0, 7);

        }
        s += "]";
        return s;
    }

    /**
     * Left trim a string to a fixed length
     *
     * @param str String to trim
     * @param len length to trim to
     * @return the trimmed string
     */
    static String leftTrim(String str, int len) {
        return str.substring(str.length() - len);
    }

    /**
     * Fill a 2D array of floats with 0.0
     */
    static void zero(float[][] array) {
        for (float[] aFloat : array) {
            Arrays.fill(aFloat, -1);
        }
    }

    /**
     * Calculate the number of gaps + 1 given a start and end range
     *
     * @param start table of range starts
     * @param end   table of range ends
     */
    public static int gaps(int[] start, int[] end) {
        Arrays.sort(start);
        Arrays.sort(end);
        int overlap = 0;
        int gaps = 0;
        for (int i = 0, j = 0; j < end.length; ) {
            if (i < start.length && start[i] < end[j]) {
                overlap++;
                i++;
            } else {
                j++;
                overlap--;
            }
            if (overlap == 0) {
                gaps++;
            }
        }
        return gaps;
    }

    /**
     * calculate the ranges for the cells
     *
     * @param start table of range starts
     * @param end   table of range ends
     * @return array of integers 2 for each cell
     */
    public static int[] cells(int[] start, int[] end) {
        Arrays.sort(start);
        Arrays.sort(end);

        int overlap = 0;
        int gaps = 0;
        for (int i = 0, j = 0; j < end.length; ) {
            if (i < start.length && start[i] < end[j]) {
                overlap++;
                i++;
            } else {
                j++;
                overlap--;
            }
            if (overlap == 0) {
                gaps++;
            }
        }
        int[] cells = new int[gaps * 2];
        overlap = 0;
        gaps = 0;
        int previousOverlap = 0;
        for (int i = 0, j = 0; j < end.length; ) {
            if (i < start.length && start[i] < end[j]) {
                overlap++;
                if (previousOverlap == 0) {
                    cells[gaps++] = start[i];
                }
                i++;
            } else {
                overlap--;
                if (overlap == 0) {
                    cells[gaps++] = end[j];
                }
                j++;
            }
            previousOverlap = overlap;
        }

        return cells;
    }

    /**
     * Search within the collection of ranges for the position
     *
     * @param pos range pairs
     * @param p1  start of widget
     * @param p2  end of widget
     * @return the pair of ranges it is within
     */
    public static int getPosition(int[] pos, int p1, int p2) {
        for (int j = 0; j < pos.length; j += 2) { // linear search is best because N typically < 10
            if (pos[j] <= p1 && p2 <= pos[j + 1]) {
                return j / 2;
            }
        }
        return -1;
    }

    /**
     * Sort a list of integers and remove duplicates
     */
    static int[] sortUnique(int[] list) {
        Arrays.sort(list);
        int count = 1;
        for (int i = 1; i < list.length; i++) {
            if (list[i] != list[i - 1]) {
                count++;
            }
        }
        int[] ret = new int[count];
        count = 1;
        ret[0] = list[0];
        for (int i = 1; i < list.length; i++) {
            if (list[i] != list[i - 1]) {
                ret[count++] = list[i];
            }
        }
        return ret;
    }

    /**
     * print a string that is a fixed width of size used in debugging
     */
    static void fwPrint(String s, int size) {
        s += "                                             ";
        s = s.substring(0, size);
        System.out.print(s);
    }


    /**
     * Get the bounding box around a list of widgets
     */
    static Rectangle getBoundingBox(ArrayList<ConstraintWidget> widgets) {
        Rectangle all = null;
        Rectangle tmp = new Rectangle();
        for (ConstraintWidget widget : widgets) {
            if (widget instanceof Guideline) {
                continue;
            }
            tmp.x = widget.getX();
            tmp.y = widget.getY();
            tmp.width = widget.getWidth();
            tmp.height = widget.getHeight();
            if (all == null) {
                all = new Rectangle(tmp);
            } else {
                all = all.union(tmp);
            }
        }
        return all;
    }

}
