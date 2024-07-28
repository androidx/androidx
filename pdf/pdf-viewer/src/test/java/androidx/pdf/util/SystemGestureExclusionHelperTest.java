/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;


import static androidx.pdf.util.SystemGestureExclusionHelper.createExclusionRectsForCorners;
import static androidx.pdf.util.SystemGestureExclusionHelper.createLeftSideExclusionRect;
import static androidx.pdf.util.SystemGestureExclusionHelper.createRightSideExclusionRect;
import static androidx.pdf.util.SystemGestureExclusionHelper.needsLeftSideExclusionRect;
import static androidx.pdf.util.SystemGestureExclusionHelper.needsRightSideExclusionRect;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Rect;
import android.os.Build;

import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/** Unit tests for {@link SystemGestureExclusionHelper}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class SystemGestureExclusionHelperTest {

    private static final int BUFFER_DISTANCE_ZERO = 0;
    private static final int BUFFER_DISTANCE_FIVE = 5;
    private static final int RESERVED_DISTANCE_ZERO = 0;
    private static final int SCREEN_WIDTH_100 = 100;
    private static final int SYSTEM_GESTURE_WIDTH_10 = 10;

    @Test
    public void needsLeftSideExclusionRect_noBuffer() {
        // Should only return true if xCoordinatePx is < systemGestureInsetsWidthPx.
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 0, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_ZERO))
                .isTrue();
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 11, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_ZERO))
                .isFalse();
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 10, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_ZERO))
                .isFalse();
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 0,
                        /* systemGestureInsetsWidthPx = */ 0,
                        BUFFER_DISTANCE_ZERO))
                .isFalse();
    }

    @Test
    public void needsLeftSideExclusionRect_withBuffer() {
        // Should return true when xCoordinatePx < systemGestureInsetsWidthPx + bufferDistancePx.
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 0, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE))
                .isTrue();
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 11, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE))
                .isTrue();
        assertThat(
                needsLeftSideExclusionRect(
                        /* xCoordinatePx = */ 15, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE))
                .isFalse();
    }

    @Test
    public void needsLeftSideExclusionRect_negativeXCoordinateThrowsException() {
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    needsLeftSideExclusionRect(
                            /* xCoordinatePx = */ -5, SYSTEM_GESTURE_WIDTH_10,
                            BUFFER_DISTANCE_ZERO);
                });
    }

    @Test
    public void needRightSideExclusionRect_noBuffer() {
        // Should only return true if xCoordinatePx is > screenWidthPx - systemGestureInsetsWidthPx.
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 95,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_ZERO,
                        SCREEN_WIDTH_100))
                .isTrue();
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 90,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_ZERO,
                        SCREEN_WIDTH_100))
                .isFalse();
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 90,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_ZERO,
                        SCREEN_WIDTH_100))
                .isFalse();
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 100,
                        /* systemGestureInsetsWidthPx = */ 0,
                        BUFFER_DISTANCE_ZERO,
                        SCREEN_WIDTH_100))
                .isFalse();
    }

    @Test
    public void needsRightSideExclusionRect_withBuffer() {
        // Should return true when xCoordinatePx >  screenWidth - (systemGestureInsetsWidthPx +
        // bufferDistancePx).
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 100,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_FIVE,
                        SCREEN_WIDTH_100))
                .isTrue();
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 89,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_FIVE,
                        SCREEN_WIDTH_100))
                .isTrue();
        assertThat(
                needsRightSideExclusionRect(
                        /* xCoordinatePx = */ 85,
                        SYSTEM_GESTURE_WIDTH_10,
                        BUFFER_DISTANCE_FIVE,
                        SCREEN_WIDTH_100))
                .isFalse();
    }

    @Test
    public void needsRightSideExclusionRect_xCoordinateTooBigThrowsException() {
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    needsRightSideExclusionRect(
                            /* xCoordinatePx = */ SCREEN_WIDTH_100 + 5,
                            SYSTEM_GESTURE_WIDTH_10,
                            BUFFER_DISTANCE_ZERO,
                            SCREEN_WIDTH_100);
                });
    }

    @Test
    public void createLeftSideExclusionRect_invalidReservedDistanceThrowsException() {
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    createLeftSideExclusionRect(
                            /* yCoordinatePx = */ 0, SYSTEM_GESTURE_WIDTH_10,
                            RESERVED_DISTANCE_ZERO);
                });
    }

    @Test
    public void testCreateLeftSideExclusionRect() {
        int yCoordinatePx = 50;
        int reservedSpace = 10;
        Rect expected = new Rect(0, yCoordinatePx - reservedSpace, 10,
                yCoordinatePx + reservedSpace);
        assertThat(
                createLeftSideExclusionRect(yCoordinatePx, SYSTEM_GESTURE_WIDTH_10, reservedSpace))
                .isEqualTo(expected);
    }

    @Test
    public void createRightSideExclusionRect_invalidReservedDistance() {
        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    createRightSideExclusionRect(
                            /* yCoordinatePx = */ 0,
                            SYSTEM_GESTURE_WIDTH_10,
                            RESERVED_DISTANCE_ZERO,
                            SCREEN_WIDTH_100);
                });
    }

    @Test
    public void testCreateRightSideExclusionRect() {
        int yCoordinatePx = 50;
        int reservedSpace = 10;
        Rect expected =
                new Rect(
                        SCREEN_WIDTH_100 - SYSTEM_GESTURE_WIDTH_10,
                        yCoordinatePx - reservedSpace,
                        SCREEN_WIDTH_100,
                        yCoordinatePx + reservedSpace);
        assertThat(
                createRightSideExclusionRect(
                        yCoordinatePx, SYSTEM_GESTURE_WIDTH_10, reservedSpace, SCREEN_WIDTH_100))
                .isEqualTo(expected);
    }

    @Test
    public void testCreateExclusionRectsForCorners_noneNeededReturnsEmptyList() {
        // 10x10 rect that would sit in the center of a 100x100 screen.
        Rect rect = new Rect(45, 45, 55, 55);

        List<Rect> returned =
                createExclusionRectsForCorners(
                        rect, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE, SCREEN_WIDTH_100);

        assertThat(returned).isEmpty();
    }

    @Test
    public void testCreateExclusionRectsForCorners_onlyLeftSideNeeded() {
        // 10x10 rect that would sit in the top left corner of a screen.
        Rect rect = new Rect(0, 0, 10, 10);

        List<Rect> returned =
                createExclusionRectsForCorners(
                        rect, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE, SCREEN_WIDTH_100);

        assertThat(returned).containsExactly(new Rect(0, -5, 10, 5), new Rect(0, 5, 10, 15));
    }

    @Test
    public void testCreateExclusionRectsForCorners_onlyRightSideNeeded() {
        // 10x10 rect that would sit in the top right corner of a 100x100 screen.
        Rect rect = new Rect(90, 0, SCREEN_WIDTH_100, 10);

        List<Rect> returned =
                createExclusionRectsForCorners(
                        rect, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE, SCREEN_WIDTH_100);

        assertThat(returned)
                .containsExactly(
                        new Rect(90, -5, SCREEN_WIDTH_100, 5),
                        new Rect(90, 5, SCREEN_WIDTH_100, 15));
    }

    @Test
    public void testCreateExclusionRectsForCorners_allNeeded() {
        // 100x10 rect that would be located at the top of a 100x100 screen.
        Rect rect = new Rect(0, 0, SCREEN_WIDTH_100, 10);

        List<Rect> returned =
                createExclusionRectsForCorners(
                        rect, SYSTEM_GESTURE_WIDTH_10, BUFFER_DISTANCE_FIVE, SCREEN_WIDTH_100);

        assertThat(returned)
                .containsExactly(
                        new Rect(0, -5, 10, 5),
                        new Rect(0, 5, 10, 15),
                        new Rect(90, -5, SCREEN_WIDTH_100, 5),
                        new Rect(90, 5, SCREEN_WIDTH_100, 15));
    }
}
