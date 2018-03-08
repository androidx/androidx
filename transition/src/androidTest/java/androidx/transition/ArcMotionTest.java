/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import static org.junit.Assert.assertEquals;

import android.graphics.Path;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ArcMotionTest extends PathMotionTest {

    @Test
    public void test90Quadrants() {
        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMaximumAngle(90);

        Path expected = arcWithPoint(0, 100, 100, 0, 100, 100);
        Path path = arcMotion.getPath(0, 100, 100, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(100, 0, 0, -100, 0, 0);
        path = arcMotion.getPath(100, 0, 0, -100);
        assertPathMatches(expected, path);

        expected = arcWithPoint(0, -100, -100, 0, 0, 0);
        path = arcMotion.getPath(0, -100, -100, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(-100, 0, 0, 100, -100, 100);
        path = arcMotion.getPath(-100, 0, 0, 100);
        assertPathMatches(expected, path);
    }

    @Test
    public void test345Triangles() {
        // 3-4-5 triangles are easy to calculate the control points
        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMaximumAngle(90);
        Path expected;
        Path path;

        expected = arcWithPoint(0, 120, 160, 0, 125, 120);
        path = arcMotion.getPath(0, 120, 160, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(0, 160, 120, 0, 120, 125);
        path = arcMotion.getPath(0, 160, 120, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(-120, 0, 0, 160, -120, 125);
        path = arcMotion.getPath(-120, 0, 0, 160);
        assertPathMatches(expected, path);

        expected = arcWithPoint(-160, 0, 0, 120, -125, 120);
        path = arcMotion.getPath(-160, 0, 0, 120);
        assertPathMatches(expected, path);

        expected = arcWithPoint(0, -120, -160, 0, -35, 0);
        path = arcMotion.getPath(0, -120, -160, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(0, -160, -120, 0, 0, -35);
        path = arcMotion.getPath(0, -160, -120, 0);
        assertPathMatches(expected, path);

        expected = arcWithPoint(120, 0, 0, -160, 0, -35);
        path = arcMotion.getPath(120, 0, 0, -160);
        assertPathMatches(expected, path);

        expected = arcWithPoint(160, 0, 0, -120, 35, 0);
        path = arcMotion.getPath(160, 0, 0, -120);
        assertPathMatches(expected, path);
    }

    private static Path arcWithPoint(float startX, float startY, float endX, float endY,
            float eX, float eY) {
        float c1x = (eX + startX) / 2;
        float c1y = (eY + startY) / 2;
        float c2x = (eX + endX) / 2;
        float c2y = (eY + endY) / 2;
        Path path = new Path();
        path.moveTo(startX, startY);
        path.cubicTo(c1x, c1y, c2x, c2y, endX, endY);
        return path;
    }

    @Test
    public void testMaximumAngle() {
        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMaximumAngle(45f);
        assertEquals(45f, arcMotion.getMaximumAngle(), 0.0f);

        float ratio = (float) Math.tan(Math.PI / 8);
        float ex = 50 + (50 * ratio);
        float ey = ex;

        Path expected = arcWithPoint(0, 100, 100, 0, ex, ey);
        Path path = arcMotion.getPath(0, 100, 100, 0);
        assertPathMatches(expected, path);
    }

    @Test
    public void testMinimumHorizontalAngle() {
        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMinimumHorizontalAngle(45);
        assertEquals(45, arcMotion.getMinimumHorizontalAngle(), 0.0f);

        float ex = 37.5f;
        float ey = (float) (Math.tan(Math.PI / 4) * 50);
        Path expected = arcWithPoint(0, 0, 100, 50, ex, ey);
        Path path = arcMotion.getPath(0, 0, 100, 50);
        assertPathMatches(expected, path);

        // Pretty much the same, but follows a different path.
        expected = arcWithPoint(0, 0, 100.001f, 50, ex, ey);
        path = arcMotion.getPath(0, 0, 100.001f, 50);
        assertPathMatches(expected, path);

        // Moving in the opposite direction.
        expected = arcWithPoint(100, 50, 0, 0, ex, ey);
        path = arcMotion.getPath(100, 50, 0, 0);
        assertPathMatches(expected, path);

        // With x < y.
        ex = 0;
        ey = (float) (Math.tan(Math.PI / 4) * 62.5f);
        expected = arcWithPoint(0, 0, 50, 100, ex, ey);
        path = arcMotion.getPath(0, 0, 50, 100);
        assertPathMatches(expected, path);

        // Pretty much the same, but follows a different path.
        expected = arcWithPoint(0, 0, 50, 100.001f, ex, ey);
        path = arcMotion.getPath(0, 0, 50, 100.001f);
        assertPathMatches(expected, path);

        // Moving in the opposite direction.
        expected = arcWithPoint(50, 100, 0, 0, ex, ey);
        path = arcMotion.getPath(50, 100, 0, 0);
        assertPathMatches(expected, path);
    }

    @Test
    public void testMinimumVerticalAngle() {
        ArcMotion arcMotion = new ArcMotion();
        arcMotion.setMinimumVerticalAngle(45);
        assertEquals(45, arcMotion.getMinimumVerticalAngle(), 0.0f);

        float ex = 0;
        float ey = 62.5f;
        Path expected = arcWithPoint(0, 0, 50, 100, ex, ey);
        Path path = arcMotion.getPath(0, 0, 50, 100);
        assertPathMatches(expected, path);

        // Pretty much the same, but follows a different path.
        expected = arcWithPoint(0, 0, 50, 100.001f, ex, ey);
        path = arcMotion.getPath(0, 0, 50, 100.001f);
        assertPathMatches(expected, path);

        // Moving in opposite direction.
        expected = arcWithPoint(50, 100, 0, 0, ex, ey);
        path = arcMotion.getPath(50, 100, 0, 0);
        assertPathMatches(expected, path);

        // With x > y.
        ex = (float) (Math.tan(Math.PI / 4) * 37.5f);
        ey = 50;
        expected = arcWithPoint(0, 0, 100, 50, ex, ey);
        path = arcMotion.getPath(0, 0, 100, 50);
        assertPathMatches(expected, path);

        // Pretty much the same, but follows a different path.
        expected = arcWithPoint(0, 0, 100.001f, 50, ex, ey);
        path = arcMotion.getPath(0, 0, 100.001f, 50);
        assertPathMatches(expected, path);

        // Moving in opposite direction.
        expected = arcWithPoint(100, 50, 0, 0, ex, ey);
        path = arcMotion.getPath(100, 50, 0, 0);
        assertPathMatches(expected, path);

    }

}
