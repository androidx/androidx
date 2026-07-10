/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Path;
import android.graphics.RectF;

import androidx.core.graphics.PathParser;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link DisplayShapeCompat}. These tests focus on the backported logic
 * within {@link DisplayShapeCompat}, as instances created via the public static
 * factory methods are always backported and do not rely on platform APIs.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class DisplayShapeCompatTest {

    private static final String SPEC_RECTANGULAR_SHAPE = "M 0,0 L 100,0 L 100,200 L 0,200 Z";
    private static final String SPEC_ROUND_SHAPE_100 =
            "M0,50.0 A50.0,50.0 0 1,1 100,50.0 A50.0,50.0 0 1,1 0,50.0 Z";
    private static final float DELTA = 1e-6f;

    private static void assertEquals(String message, RectF expected, RectF actual) {
        Assert.assertEquals(message + " (left)", expected.left, actual.left, DELTA);
        Assert.assertEquals(message + " (top)", expected.top, actual.top, DELTA);
        Assert.assertEquals(message + " (right)", expected.right, actual.right, DELTA);
        Assert.assertEquals(message + " (bottom)", expected.bottom, actual.bottom, DELTA);
    }

    private void assertEquals(RectF expected, RectF actual) {
        assertEquals("RectF comparison failed", expected, actual);
    }

    @Test
    public void test_create_rectangular() {
        final DisplayShapeCompat compat = DisplayShapeCompat
                .create(
                        SPEC_RECTANGULAR_SHAPE,
                        1f,
                        100,
                        200);
        assertNotNull(compat);

        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualRect = new RectF();
        path.computeBounds(actualRect, false);
        final RectF expectRect = new RectF(0f, 0f, 100f, 200f);
        assertEquals("Rectangular shape bounds mismatch", expectRect, actualRect);
    }

    @Test
    public void test_create_is_round() {
        final DisplayShapeCompat compat = DisplayShapeCompat.create(100, 100,
                true, 0, 0, 0, 0);
        assertNotNull(compat);

        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualRect = new RectF();
        path.computeBounds(actualRect, false);
        final RectF expectRect = new RectF(0f, 0f, 100f, 100f);
        assertEquals("Round shape bounds mismatch", expectRect, actualRect);
    }

    @Test
    public void test_create_is_not_round() {
        final DisplayShapeCompat compat = DisplayShapeCompat.create(100, 200,
                false, 0, 0, 0, 0);
        assertNotNull(compat);

        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualRect = new RectF();
        path.computeBounds(actualRect, false);
        final RectF expectRect = new RectF(0f, 0f, 100f, 200f);
        assertEquals("Default rectangular shape bounds mismatch", expectRect, actualRect);
    }

    @Test
    public void test_physical_pixel_display_size_ratio() {
        final DisplayShapeCompat compat = DisplayShapeCompat.create(SPEC_RECTANGULAR_SHAPE,
                0.5f, 100, 200);
        final Path path = compat.getPath();
        assertNotNull(path);
        final RectF actualRect = new RectF();
        path.computeBounds(actualRect, false);
        final RectF expectRect = new RectF(0f, 0f, 50f, 100f);
        assertEquals("Physical pixel ratio bounds mismatch", expectRect, actualRect);
    }

    @Test
    public void test_getPath_cache() {
        final DisplayShapeCompat compat = DisplayShapeCompat.create(SPEC_RECTANGULAR_SHAPE,
                1f, 100, 200);
        final Path path1 = compat.getPath();
        assertNotNull(path1);

        // Second call with the same instance should return the same cached path object.
        final Path path2 = compat.getPath();
        assertTrue("Second getPath() call on same instance should return the same object",
                path1 == path2);

        // Instance with different parameters should result in a different path.
        final DisplayShapeCompat compat2 = DisplayShapeCompat.create(SPEC_RECTANGULAR_SHAPE,
                1f, 100, 201);
        final Path path3 = compat2.getPath();
        assertNotNull(path3);
        assertFalse("Different shape should have a different path object", path1 == path3);
    }

    @Test
    public void test_equals_backported_instances() {
        final DisplayShapeCompat compat1a = DisplayShapeCompat.create(
                SPEC_RECTANGULAR_SHAPE, 1f, 100, 200);
        final DisplayShapeCompat compat1b = DisplayShapeCompat.create(
                SPEC_RECTANGULAR_SHAPE, 1f, 100, 200);
        final DisplayShapeCompat compat2 = DisplayShapeCompat.create(SPEC_ROUND_SHAPE_100,
                1f, 100, 100);
        final DisplayShapeCompat compat3 = DisplayShapeCompat.create(SPEC_RECTANGULAR_SHAPE,
                1f, 100, 201);
        final DisplayShapeCompat compat4 = DisplayShapeCompat.create(100, 200,
                false,  0, 0, 0, 0);

        Assert.assertEquals("Instances from identical create params should be equal", compat1a,
                compat1b);
        Assert.assertEquals("Hash codes should match for equal objects", compat1a.hashCode(),
                compat1b.hashCode());

        assertNotEquals("Different specs should not be equal", compat1a, compat2);
        assertNotEquals("Different dimensions should not be equal", compat1a, compat3);

        Assert.assertEquals("createDefaultDisplayShape matches create for rectangular", compat1a,
                compat4);
    }

    @Test
    public void test_empty_returns_empty_path() {
        final DisplayShapeCompat emptyCompat = DisplayShapeCompat.EMPTY;
        final Path path = emptyCompat.getPath();
        assertNotNull(path);
        assertTrue("Empty compat should yield an empty path", path.isEmpty());
        final RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        assertTrue("Bounds of empty path should be empty", bounds.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void test_min34_toDisplayShapeCompat_returns_null() {
        assertNull("toDisplayShapeCompat(null) must return null.",
                DisplayShapeCompat.toDisplayShapeCompat(null));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void test_create_all_corners_equal() {
        final int width = 400;
        final int height = 200;
        final int radius = 50;
        final DisplayShapeCompat compat = DisplayShapeCompat.create(
                width, height, false, radius, radius, radius, radius);

        assertNotNull(compat);
        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualBounds = new RectF();
        path.computeBounds(actualBounds, true);
        assertEquals(new RectF(0f, 0f, (float) width, (float) height), actualBounds);

        final String expectedSpec =
                "M 50,0 L 350,0 A 50,50 0 0,1 400,50 L 400,150 A 50,50 0 0,1 350,200 L 50,200 A "
                        + "50,50 0 0,1 0,150 L 0,50 A 50,50 0 0,1 50,0 Z";

        final Path expectedPath = PathParser.createPathFromPathData(expectedSpec);
        final Path diffPath = new Path();
        diffPath.op(path, expectedPath, Path.Op.XOR);
        assertTrue("The path generated by create must be the "
                + "same as the one parsed from the expected spec.", diffPath.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void test_create_some_corners() {
        final int width = 300;
        final int height = 150;
        final int topLeftRadius = 30;
        final int bottomRightRadius = 40;

        final DisplayShapeCompat compat = DisplayShapeCompat.create(
                width, height, false, topLeftRadius, 0, bottomRightRadius, 0);

        assertNotNull(compat);
        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualBounds = new RectF();
        path.computeBounds(actualBounds, true);
        assertEquals(new RectF(0f, 0f, (float) width, (float) height), actualBounds);

        // Expected spec: TL (30) and BR (40) rounded, others sharp.
        final String expectedSpec =
                "M 30,0 L 300,0 L 300,110 A 40,40 0 0,1 260,150 L 0,150 L 0,30 A 30,30 0 0,1 30,0"
                        + " Z";

        final Path expectedPath = PathParser.createPathFromPathData(expectedSpec);
        final Path diffPath = new Path();
        diffPath.op(path, expectedPath, Path.Op.XOR);
        assertTrue("The path generated by create must be the "
                + "same as the one parsed from the expected spec.", diffPath.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void test_create_radius_clamping() {
        final int width = 100;
        final int height = 100;
        final int largeRadius = 60;

        final DisplayShapeCompat compat = DisplayShapeCompat.create(
                width, height, false, largeRadius, largeRadius, largeRadius, largeRadius);

        assertNotNull(compat);
        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
        // The clamped radius should be 50. This should result in a circular shape.
        final RectF actualBounds = new RectF();
        path.computeBounds(actualBounds, true);
        assertEquals(new RectF(0f, 0f, (float) width, (float) height), actualBounds);

        // The spec generated by createSpecStringWithRoundedCorners for a full circle:
        final String expectedClampedSpec =
                "M 50,0 L 50,0 A 50,50 0 0,1 100,50 L 100,50 A 50,50 0 0,1 50,100 L 50,100 A "
                        + "50,50 0 0,1 0,50 L 0,50 A 50,50 0 0,1 50,0 Z";
        final Path expectedPath = PathParser.createPathFromPathData(expectedClampedSpec);
        final Path diffPath = new Path();
        diffPath.op(path, expectedPath, Path.Op.XOR);
        assertTrue("The path generated by create must be the "
                + "same as the one parsed from the expected spec.", diffPath.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void test_create_zero_radius() {
        final int width = 200;
        final int height = 100;
        final DisplayShapeCompat compat = DisplayShapeCompat.create(
                width, height, false, 0, 0, 0, 0);

        assertNotNull(compat);
        final Path path = compat.getPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());

        final RectF actualBounds = new RectF();
        path.computeBounds(actualBounds, true);
        assertEquals(new RectF(0f, 0f, (float) width, (float) height), actualBounds);

        // Zero radius should result in a simple rectangular shape.
        final String expectedSpec = "M 0,0 L 200,0 L 200,100 L 0,100 L 0,0 Z";
        final Path expectedPath = PathParser.createPathFromPathData(expectedSpec);
        final Path diffPath = new Path();
        diffPath.op(path, expectedPath, Path.Op.XOR);
        assertTrue("The path generated by create must be the "
                + "same as the one parsed from the expected spec.", diffPath.isEmpty());
    }
}
