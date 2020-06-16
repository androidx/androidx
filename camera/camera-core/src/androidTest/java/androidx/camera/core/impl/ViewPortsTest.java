/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl;

import static android.util.LayoutDirection.LTR;
import static android.util.LayoutDirection.RTL;

import static androidx.camera.core.ViewPort.FILL_CENTER;
import static androidx.camera.core.ViewPort.FILL_END;
import static androidx.camera.core.ViewPort.FILL_START;
import static androidx.camera.core.ViewPort.FIT_CENTER;
import static androidx.camera.core.ViewPort.FIT_END;
import static androidx.camera.core.ViewPort.FIT_START;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Rational;
import android.util.Size;

import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.internal.ViewPorts;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link ViewPorts}.
 */
@SmallTest
@RunWith(Enclosed.class)
public class ViewPortsTest {

    // Rotation degrees.
    private static final int R0 = 0;
    private static final int R90 = 90;
    private static final int R180 = 180;
    private static final int R270 = 270;

    // 2:1 aspect ratio wider than 4:3.
    private static final Rational WIDE = new Rational(2, 1);
    // 1:2 aspect ratio narrower than 3:4.
    private static final Rational NARROW = new Rational(1, 2);

    /**
     * Parameterized tests for {@link ViewPorts#getScaledRect(RectF, Rational, int, int, int)}
     * testing all possible input combinations.
     */
    @SmallTest
    @RunWith(Parameterized.class)
    public static class GetScaledRectTests {

        // A 60x40 rect located at (10, 20).
        private static final RectF FITTING_RECT = new RectF(10, 20, 70, 60);
        // Size of crop rect for all 4 possible aspect ratio/ fill type combinations.
        private static final Size WIDE_FILL = new Size(60, 30);
        private static final Size WIDE_FIT = new Size(80, 40);
        private static final Size NARROW_FILL = new Size(20, 40);
        private static final Size NARROW_FIT = new Size(60, 120);

        @Parameterized.Parameter()
        public Rational mAspectRatio;

        @Parameterized.Parameter(1)
        @ViewPort.LayoutDirection
        public int mLayoutDirection;

        @Parameterized.Parameter(2)
        public int mRotationDegrees;

        @Parameterized.Parameter(3)
        @ViewPort.ScaleType
        public int mScaleType;

        @Parameterized.Parameter(4)
        public int mExpectedLeft;

        @Parameterized.Parameter(5)
        public int mExpectedTop;

        @Parameterized.Parameter(6)
        public Size mExpectedSize;

        @Parameterized.Parameters
        public static Collection<Object[]> getParameters() {
            List<Object[]> result = new ArrayList<>();
            // Wide viewport & FILL_CENTER. Parameter index 0~7.
            result.add(new Object[]{WIDE, LTR, R0, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R90, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R180, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R270, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R0, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R90, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R180, FILL_CENTER, 10, 25, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R270, FILL_CENTER, 10, 25, WIDE_FILL});
            // Wide viewport & FIT_CENTER. Parameter index 8~15.
            result.add(new Object[]{WIDE, LTR, R0, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R90, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R180, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R270, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R0, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R90, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R180, FIT_CENTER, 0, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R270, FIT_CENTER, 0, 20, WIDE_FIT});
            // Wide viewport & FILL_START. Parameter index 16~23.
            result.add(new Object[]{WIDE, LTR, R0, FILL_START, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R90, FILL_START, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R180, FILL_START, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R270, FILL_START, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R0, FILL_START, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R90, FILL_START, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R180, FILL_START, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R270, FILL_START, 10, 30, WIDE_FILL});
            // Wide viewport & FIT_START. Parameter index 24~31.
            result.add(new Object[]{WIDE, LTR, R0, FIT_START, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R90, FIT_START, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R180, FIT_START, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R270, FIT_START, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R0, FIT_START, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R90, FIT_START, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R180, FIT_START, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R270, FIT_START, -10, 20, WIDE_FIT});
            // Wide viewport & FILL_END. Parameter index 32~39.
            result.add(new Object[]{WIDE, LTR, R0, FILL_END, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R90, FILL_END, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R180, FILL_END, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, LTR, R270, FILL_END, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R0, FILL_END, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R90, FILL_END, 10, 30, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R180, FILL_END, 10, 20, WIDE_FILL});
            result.add(new Object[]{WIDE, RTL, R270, FILL_END, 10, 20, WIDE_FILL});
            // Wide viewport & FIT_END. Parameter index 40~47.
            result.add(new Object[]{WIDE, LTR, R0, FIT_END, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R90, FIT_END, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R180, FIT_END, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, LTR, R270, FIT_END, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R0, FIT_END, 10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R90, FIT_END, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R180, FIT_END, -10, 20, WIDE_FIT});
            result.add(new Object[]{WIDE, RTL, R270, FIT_END, 10, 20, WIDE_FIT});
            // Narrow viewport & FILL_CENTER. Parameter index 48~55.
            result.add(new Object[]{NARROW, LTR, R0, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R90, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R180, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R270, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R0, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R90, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R180, FILL_CENTER, 30, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R270, FILL_CENTER, 30, 20, NARROW_FILL});
            // Narrow viewport & FIT_CENTER. Parameter index 56~63.
            result.add(new Object[]{NARROW, LTR, R0, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R90, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R180, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R270, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R0, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R90, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R180, FIT_CENTER, 10, -20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R270, FIT_CENTER, 10, -20, NARROW_FIT});
            // Narrow viewport & FILL_START. Parameter index 64~71.
            result.add(new Object[]{NARROW, LTR, R0, FILL_START, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R90, FILL_START, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R180, FILL_START, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R270, FILL_START, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R0, FILL_START, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R90, FILL_START, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R180, FILL_START, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R270, FILL_START, 50, 20, NARROW_FILL});
            // Narrow viewport & FIT_START. Parameter index 72~79.
            result.add(new Object[]{NARROW, LTR, R0, FIT_START, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R90, FIT_START, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R180, FIT_START, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R270, FIT_START, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R0, FIT_START, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R90, FIT_START, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R180, FIT_START, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R270, FIT_START, 10, -60, NARROW_FIT});
            // Narrow viewport & FILL_END. Parameter index 80~87.
            result.add(new Object[]{NARROW, LTR, R0, FILL_END, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R90, FILL_END, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R180, FILL_END, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, LTR, R270, FILL_END, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R0, FILL_END, 10, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R90, FILL_END, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R180, FILL_END, 50, 20, NARROW_FILL});
            result.add(new Object[]{NARROW, RTL, R270, FILL_END, 10, 20, NARROW_FILL});
            // Narrow viewport & FIT_END. Parameter index 88~95.
            result.add(new Object[]{NARROW, LTR, R0, FIT_END, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R90, FIT_END, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R180, FIT_END, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, LTR, R270, FIT_END, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R0, FIT_END, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R90, FIT_END, 10, -60, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R180, FIT_END, 10, 20, NARROW_FIT});
            result.add(new Object[]{NARROW, RTL, R270, FIT_END, 10, 20, NARROW_FIT});
            return result;
        }

        @Test
        public void testGetScaledRect() {
            Rect rect = new Rect();
            ViewPorts.getScaledRect(FITTING_RECT, mAspectRatio, mScaleType, mLayoutDirection,
                    mRotationDegrees).round(rect);
            assertThat(new int[]{rect.left, rect.top, rect.width(), rect.height()})
                    .isEqualTo(new int[]{mExpectedLeft, mExpectedTop, mExpectedSize.getWidth(),
                            mExpectedSize.getHeight()});
        }
    }

    @SmallTest
    @RunWith(Parameterized.class)
    public static class GetViewPortRectsTests {

        private static final Size SENSOR_SIZE = new Size(8, 8);
        private static final Size SURFACE_WIDE = new Size(8, 4);
        private static final Size SURFACE_NARROW = new Size(4, 8);

        /**
         * Parameters for testing
         * {@link ViewPorts#calculateViewPortRects(Rect, Rational, int, int, int, Map)}
         *
         * <p>The goal of the algorithm is to fit-a-minimum/fill-a-maximum 2:1 rectangle
         * with the given rotation/layout-direction to the intersection area (marked with "XXX").
         * the sensor rect is 8 x 8, narrow surface (marked with "\\\") is 4 x 8 and wide
         * surface is 8 x 4 (marked with "///").The output rect is in the surface' coordinates.
         *
         * <pre>
         * . 0  1  2  3  4  5  6  7  8
         * 1 +-----\\\\\\\\\\\\-----+
         * 2 |     \\\\\\\\\\\\     |
         * 3 //////XXXXXXXXXXXX//////
         * 4 //////XXXXXXXXXXXX//////
         * 5 //////XXXXXXXXXXXX//////
         * 6 //////XXXXXXXXXXXX//////
         * 7 |     \\\\\\\\\\\\     |
         * 8 +-----\\\\\\\\\\\\-----+
         * </pre>
         *
         * Only test 2 groups of cases. The rest should be tested by {@link GetScaledRectTests}
         */
        @Parameterized.Parameters
        public static Collection<Object[]> getParameters() {
            List<Object[]> result = new ArrayList<>();

            // Wide and LTR.
            result.add(new Object[]{SENSOR_SIZE, WIDE, R0, FILL_START, LTR,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 2, 4, 4),
                            new Rect(2, 0, 6, 2)
                    }});
            result.add(new Object[]{SENSOR_SIZE, WIDE, R90, FILL_START, LTR,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 2, 2, 6),
                            new Rect(2, 0, 4, 4)
                    }});
            result.add(new Object[]{SENSOR_SIZE, WIDE, R180, FILL_START, LTR,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 4, 4, 6),
                            new Rect(2, 2, 6, 4)
                    }});
            result.add(new Object[]{SENSOR_SIZE, WIDE, R270, FILL_START, LTR,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(2, 2, 4, 6),
                            new Rect(4, 0, 6, 4)
                    }});

            // Narrow and RTL.
            result.add(new Object[]{SENSOR_SIZE, NARROW, R0, FILL_START, RTL,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(2, 2, 4, 6),
                            new Rect(4, 0, 6, 4)
                    }});
            result.add(new Object[]{SENSOR_SIZE, NARROW, R90, FILL_START, RTL,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 2, 4, 4),
                            new Rect(2, 0, 6, 2)
                    }});
            result.add(new Object[]{SENSOR_SIZE, NARROW, R180, FILL_START, RTL,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 2, 2, 6),
                            new Rect(2, 0, 4, 4)
                    }});
            result.add(new Object[]{SENSOR_SIZE, NARROW, R270, FILL_START, RTL,
                    new Size[]{SURFACE_NARROW, SURFACE_WIDE},
                    new Rect[]{
                            new Rect(0, 4, 4, 6),
                            new Rect(2, 2, 6, 4)
                    }});
            return result;
        }

        @Parameterized.Parameter()
        public Size mSensorSize;

        @Parameterized.Parameter(1)
        public Rational mAspectRatio;

        @Parameterized.Parameter(2)
        public int mRotationDegrees;

        @Parameterized.Parameter(3)
        @ViewPort.ScaleType
        public int mScaleType;

        @Parameterized.Parameter(4)
        @ViewPort.LayoutDirection
        public int mLayoutDirection;

        @Parameterized.Parameter(5)
        public Size[] mSurfaceSizes;

        @Parameterized.Parameter(6)
        public Rect[] mExpectedCropRects;

        @Test
        public void testGetViewPortRects() {
            // Arrange.
            // Convert the sizes into a UseCase map.
            List<UseCase> orderedUseCases = new ArrayList<>();
            Map<UseCase, Size> useCaseSizeMap = new HashMap<UseCase, Size>() {
                {
                    for (Size size : mSurfaceSizes) {
                        FakeUseCase fakeUseCase = new FakeUseCaseConfig.Builder().build();
                        put(fakeUseCase, size);
                        orderedUseCases.add(fakeUseCase);
                    }
                }
            };

            // Act.
            Map<UseCase, Rect> useCaseCropRects = ViewPorts.calculateViewPortRects(
                    new Rect(0, 0, mSensorSize.getWidth(), mSensorSize.getHeight()),
                    mAspectRatio,
                    mRotationDegrees,
                    mScaleType,
                    mLayoutDirection,
                    useCaseSizeMap);

            // Assert.
            // Converts the map back to sizes array.
            List<Rect> orderedCropRects = new ArrayList<>();
            for (UseCase useCase : orderedUseCases) {
                orderedCropRects.add(useCaseCropRects.get(useCase));
            }

            assertThat(orderedCropRects.toArray(new Rect[0])).isEqualTo(mExpectedCropRects);
        }
    }
}
