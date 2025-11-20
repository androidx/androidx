/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.COS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
import static androidx.compose.remote.core.operations.utilities.PathGenerator.MONOTONIC;
import static androidx.compose.remote.core.operations.utilities.PathGenerator.SPLINE;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.PathGenerator;

import org.junit.Test;

import java.util.Arrays;

/** This test Path Generator */
public class PathGeneratorTest {
    @Test
    public void testBasicPathGenerator() {
        // Create a PathGenerator instance
        PathGenerator pathGenerator = new PathGenerator();
        int rad = 200;
        float[] xpoints = new float[10];
        float[] ypoints = new float[xpoints.length];
        int[] xp = new int[xpoints.length];
        int[] yp = new int[xpoints.length];
        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) Math.sin(i * Math.PI * 2 / xpoints.length) * rad + rad + 20;
            ypoints[i] = (float) Math.cos(i * Math.PI * 2 / xpoints.length) * rad + rad + 20;
            xp[i] = (int) xpoints[i];
            yp[i] = (int) ypoints[i];
        }

        float[] result = new float[xp.length * 9 + 5];
        int len;

        len = pathGenerator.getPath(result, xpoints, ypoints, SPLINE, false);
        assertEquals((xp.length - 1) * 9 + 3, len);
        len = pathGenerator.getPath(result, xpoints, ypoints, SPLINE, true);
        assertEquals((xp.length) * 9 + 4, len);
        len = pathGenerator.getPath(result, xpoints, ypoints, MONOTONIC, false);
        assertEquals((xp.length - 1) * 9 + 3, len);
        len = pathGenerator.getPath(result, xpoints, ypoints, MONOTONIC, true);
        assertEquals((xp.length) * 9 + 4, len);
    }

    @Test
    public void testPathGenerator() {
        // Create a PathGenerator instance
        PathGenerator pathGenerator = new PathGenerator();
        int rad = 200;
        float[] xpoints = new float[10];
        float[] ypoints = new float[xpoints.length];
        int[] xp = new int[xpoints.length];
        int[] yp = new int[xpoints.length];
        float pi = (float) Math.PI;
        float scale = pi * 2 / xpoints.length;

        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) Math.sin(i * scale) * rad + rad;
            ypoints[i] = (float) Math.cos(i * scale) * rad + rad;

            xp[i] = (int) xpoints[i];
            yp[i] = (int) ypoints[i];
        }

        float[] result1 = new float[pathGenerator.getReturnLength(xpoints.length, false)];
        float[] result2 = new float[result1.length];
        int len;

        len = pathGenerator.getPath(result1, xpoints, ypoints, SPLINE, false);
        assertEquals(len, result1.length);
        len =
                pathGenerator.getPath(
                        result2,
                        new float[] {VAR1, scale, MUL, SIN, rad, MUL, rad, ADD},
                        new float[] {VAR1, scale, MUL, COS, rad, MUL, rad, ADD},
                        0,
                        xpoints.length - 1,
                        xpoints.length,
                        SPLINE,
                        false,
                        null);
        assertEquals(len, result1.length);
        assertEquals(Arrays.toString(result1), Arrays.toString(result2));
    }

    @Test
    public void testPathGeneratorMonotonic() {
        // Create a PathGenerator instance
        PathGenerator pathGenerator = new PathGenerator();
        int rad = 200;
        float[] xpoints = new float[10];
        float[] ypoints = new float[xpoints.length];
        int[] xp = new int[xpoints.length];
        int[] yp = new int[xpoints.length];
        float pi = (float) Math.PI;
        float scale = pi * 2 / xpoints.length;

        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) Math.sin(i * scale) * rad + rad;
            ypoints[i] = (float) Math.cos(i * scale) * rad + rad;
            xp[i] = (int) xpoints[i];
            yp[i] = (int) ypoints[i];
        }

        float[] result1 = new float[pathGenerator.getReturnLength(xpoints.length, false)];
        float[] result2 = new float[result1.length];
        int len;

        len = pathGenerator.getPath(result1, xpoints, ypoints, MONOTONIC, false);
        assertEquals(len, result1.length);
        len =
                pathGenerator.getPath(
                        result2,
                        new float[] {VAR1, scale, MUL, SIN, rad, MUL, rad, ADD},
                        new float[] {VAR1, scale, MUL, COS, rad, MUL, rad, ADD},
                        0,
                        xpoints.length - 1,
                        xpoints.length,
                        MONOTONIC,
                        false,
                        null);
        assertEquals(len, result1.length);
        assertEquals(Arrays.toString(result1), Arrays.toString(result2));
    }

    @Test
    public void testPathGeneratorLoop() {
        // Create a PathGenerator instance
        PathGenerator pathGenerator = new PathGenerator();
        int rad = 200;
        float[] xpoints = new float[10];
        float[] ypoints = new float[xpoints.length];
        int[] xp = new int[xpoints.length];
        int[] yp = new int[xpoints.length];
        float pi = (float) Math.PI;
        float scale = pi * 2 / xpoints.length;

        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) Math.sin(i * scale) * rad + rad;
            ypoints[i] = (float) Math.cos(i * scale) * rad + rad;

            xp[i] = (int) xpoints[i];
            yp[i] = (int) ypoints[i];
        }

        float[] result1 = new float[pathGenerator.getReturnLength(xpoints.length, true)];
        float[] result2 = new float[result1.length];
        int len;

        len = pathGenerator.getPath(result1, xpoints, ypoints, SPLINE, true);
        assertEquals(len, result1.length);
        len =
                pathGenerator.getPath(
                        result2,
                        new float[] {VAR1, scale, MUL, SIN, rad, MUL, rad, ADD},
                        new float[] {VAR1, scale, MUL, COS, rad, MUL, rad, ADD},
                        0,
                        xpoints.length,
                        xpoints.length,
                        SPLINE,
                        true,
                        null);
        assertEquals(len, result1.length);
        assertEquals(Arrays.toString(result1), Arrays.toString(result2));
    }

    @Test
    public void testPathGeneratorMonotonicLoop() {
        // Create a PathGenerator instance
        PathGenerator pathGenerator = new PathGenerator();
        int rad = 200;
        float[] xpoints = new float[10];
        float[] ypoints = new float[xpoints.length];
        int[] xp = new int[xpoints.length];
        int[] yp = new int[xpoints.length];
        float pi = (float) Math.PI;
        float scale = pi * 2 / xpoints.length;

        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] = (float) Math.sin(i * scale) * rad + rad;
            ypoints[i] = (float) Math.cos(i * scale) * rad + rad;
            xp[i] = (int) xpoints[i];
            yp[i] = (int) ypoints[i];
        }

        float[] result1 = new float[pathGenerator.getReturnLength(xpoints.length, true)];
        float[] result2 = new float[result1.length];
        int len;

        len = pathGenerator.getPath(result1, xpoints, ypoints, MONOTONIC, true);
        assertEquals(len, result1.length);
        len =
                pathGenerator.getPath(
                        result2,
                        new float[] {VAR1, scale, MUL, SIN, rad, MUL, rad, ADD},
                        new float[] {VAR1, scale, MUL, COS, rad, MUL, rad, ADD},
                        0,
                        10,
                        10,
                        MONOTONIC,
                        true,
                        null);
        assertEquals(len, result1.length);
        assertEquals(Arrays.toString(result1), Arrays.toString(result2));
    }
}
