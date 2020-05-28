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

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.internal.ViewPorts;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link ViewPorts}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ViewPortsTest {

    // A container rect that is 4:3.
    private static final RectF CONTAINER_RECT = new RectF(10, 10, 50, 40);
    // 1:1 narrow aspect ratio
    private static final Rational FIT_ASPECT_RATIO = new Rational(100, 100);

    @Test
    public void viewPortRectWithTwoSurfacesIntersectWide() {
        assertThat(
                getViewPortRects(
                        new Size(800, 800),
                        new Rational(1, 2),
                        180,
                        ViewPort.FILL_CENTER,
                        LayoutDirection.LTR,
                        new Size(400, 800),
                        new Size(800, 400)))
                .isEqualTo(
                        new Rect[]{
                                new Rect(100, 200, 300, 600),
                                new Rect(300, 0, 500, 400)
                        });
    }

    @Test
    public void viewPortRectWithTwoSurfacesIntersectNarrow() {
        assertThat(
                getViewPortRects(
                        new Size(800, 800),
                        new Rational(2, 1),
                        180,
                        ViewPort.FILL_CENTER,
                        LayoutDirection.LTR,
                        new Size(400, 800),
                        new Size(800, 400)))
                .isEqualTo(
                        new Rect[]{
                                new Rect(0, 300, 400, 500),
                                new Rect(200, 100, 600, 300)
                        });
    }

    @Test
    public void viewPortRectLandscapeForPortraitModeAndRotate90Degrees() {
        assertThat(
                getViewPortRects(
                        new Size(800, 600),
                        new Rational(400, 300),
                        90,
                        ViewPort.FILL_CENTER,
                        LayoutDirection.LTR,
                        new Size(400, 300)))
                .isEqualTo(new Rect[]{
                        new Rect(88, 0, 313, 300)
                });
    }

    @Test
    public void viewPortRectFitEnd_topIsNegative() {
        assertThat(
                getViewPortRects(
                        new Size(800, 600),
                        new Rational(1, 1),
                        0,
                        ViewPort.FIT_END,
                        LayoutDirection.LTR,
                        new Size(400, 300)))
                .isEqualTo(new Rect[]{
                        new Rect(0, -100, 400, 300)
                });
    }

    @Test
    public void viewPortRectFillStartWithRtl() {
        assertThat(
                getViewPortRects(
                        new Size(800, 600),
                        new Rational(1, 1),
                        0,
                        ViewPort.FILL_START,
                        LayoutDirection.RTL,
                        new Size(400, 300)))
                .isEqualTo(new Rect[]{
                        new Rect(100, 0, 400, 300)
                });
    }

    /**
     * Calls {@link ViewPorts#calculateViewPortRects(Rect, Rational, int, int, int, Map)}.
     */
    private Rect[] getViewPortRects(Size sensorSize,
            Rational aspectRatio,
            @IntRange(from = 0, to = 359) int rotationDegree,
            @ViewPort.ScaleType int scaleType,
            @ViewPort.LayoutDirection int layoutDirection,
            Size... sizes) {
        // Convert the sizes into a UseCase map.
        List<UseCase> orderedUseCases = new ArrayList<>();
        Map<UseCase, Size> useCaseSizeMap = new HashMap<UseCase, Size>() {
            {
                for (Size size : sizes) {
                    FakeUseCase fakeUseCase = new FakeUseCaseConfig.Builder().build();
                    put(fakeUseCase, size);
                    orderedUseCases.add(fakeUseCase);
                }
            }
        };

        Map<UseCase, Rect> useCaseCropRects = ViewPorts.calculateViewPortRects(
                new Rect(0, 0, sensorSize.getWidth(), sensorSize.getHeight()),
                aspectRatio,
                rotationDegree,
                scaleType,
                layoutDirection,
                useCaseSizeMap);

        // Converts the map back to sizes array.
        List<Rect> orderedCropRects = new ArrayList<>();
        for (UseCase useCase : orderedUseCases) {
            orderedCropRects.add(useCaseCropRects.get(useCase));
        }

        return orderedCropRects.toArray(new Rect[0]);
    }

    @Test
    public void viewPortRectFillCenter() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FILL_CENTER).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(15, 10, 45, 40));
    }

    @Test
    public void getScaledRectFillStart() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FILL_START).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(10, 10, 40, 40));
    }

    @Test
    public void getScaledRectFillEnd() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FILL_END).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(20, 10, 50, 40));
    }

    @Test
    public void getScaledRectFitCenter() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FIT_CENTER).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(10, 5, 50, 45));
    }

    @Test
    public void getScaledRectFitStart() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FIT_START).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(10, 10, 50, 50));
    }

    @Test
    public void getScaledRectFitEnd() {
        Rect expectedRect = new Rect();
        ViewPorts.getScaledRect(CONTAINER_RECT, FIT_ASPECT_RATIO, ViewPort.FIT_END).round(
                expectedRect);
        assertThat(expectedRect).isEqualTo(new Rect(10, 0, 50, 40));
    }
}
