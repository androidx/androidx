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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ZoomUtils}. */
@RunWith(RobolectricTestRunner.class)
public class ZoomUtilsTest {

    private static final float MINZOOM = 0.1f;
    private static final float MAXZOOM = 25f;

    @Test
    public void testCalculateZoomToFit() {
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 2)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 9)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 10)).isEqualTo(2f);
        assertThat(ZoomUtils.calculateZoomToFit(2, 20, 1, 11)).isEqualTo((20 / 11f));

        // Inner bigger than outer
        assertThat(ZoomUtils.calculateZoomToFit(1, 2, 2, 20)).isEqualTo(0.1f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 9, 2, 20)).isEqualTo(0.45f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 10, 2, 20)).isEqualTo(0.5f);
        assertThat(ZoomUtils.calculateZoomToFit(1, 11, 2, 20)).isEqualTo(0.5f);

        // Reverse
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 2)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 9)).isEqualTo((2f / 9f));
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 10)).isEqualTo(0.2f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 11)).isEqualTo((2f / 11f));

        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 0, 2)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(20, 2, 1, 0)).isEqualTo(1f);
        assertThat(ZoomUtils.calculateZoomToFit(0, 0, 1, 2)).isEqualTo(0f);
    }

    @Test
    public void toContentCoordinate_nonZeroZoomScroll_returnsNonZeroCoordinate() {
        // Arrange
        float dummyZoomViewCoordinate = 10f;
        float dummyZoom = 2f;
        int dummyScroll = 5;
        float expectedContentCoordinate = 7.5f;

        // Act
        float contentCoordinate = ZoomUtils.toContentCoordinate(dummyZoomViewCoordinate,
                dummyZoom, dummyScroll);

        // Assert
        assertThat(contentCoordinate).isEqualTo(expectedContentCoordinate);
    }

    @Test
    public void toContentCoordinate_zeroZoom_throwsIllegalArgumentException() {
        float dummyZoomViewCoordinate = 10f;
        float zoom = 0;
        int dummyScroll = 10;

        assertThrows(IllegalArgumentException.class,
                () -> ZoomUtils.toContentCoordinate(dummyZoomViewCoordinate, zoom,
                        dummyScroll));
    }

    @Test
    public void toZoomViewCoordinate_nonZeroZoomScroll_returnsNonZeroCoordinate() {
        float dummyContentCoordinate = 10f;
        float dummyZoom = 2f;
        int dummyScroll = 5;
        float expectedZoomViewCoordinate = 15f;

        float zoomViewCoordinate = ZoomUtils.toZoomViewCoordinate(dummyContentCoordinate,
                dummyZoom, dummyScroll);

        assertThat(zoomViewCoordinate).isEqualTo(expectedZoomViewCoordinate);
    }

    @Test
    public void toZoomViewCoordinate_zeroZoom_throwsIllegalArgumentException() {
        float dummyContentCoordinate = 10f;
        float zoom = 0;
        int dummyScroll = 10;

        assertThrows(IllegalArgumentException.class,
                () -> ZoomUtils.toZoomViewCoordinate(dummyContentCoordinate, zoom,
                        dummyScroll));
    }

    @Test
    public void scrollDeltaNeededForZoomChange_noZoomChange_returnsZeroDelta() {
        float oldZoom = 1f;
        float newZoom = 1f;
        float zoomPivot = 5f;
        int scroll = 1;

        int delta = ZoomUtils.scrollDeltaNeededForZoomChange(oldZoom, newZoom, zoomPivot,
                scroll);

        assertEquals(0, delta);
    }

    @Test
    public void scrollDeltaNeededForZoomChange_zoomedIn_returnsPositiveDelta() {
        float oldZoom = 1f;
        float newZoom = 3f;
        float zoomPivot = 5f;
        int scroll = 1;

        int delta = ZoomUtils.scrollDeltaNeededForZoomChange(oldZoom, newZoom, zoomPivot,
                scroll);

        assertTrue(delta >= 0);
    }

    @Test
    public void scrollDeltaNeededForZoomChange_zoomedOut_returnsNegativeDelta() {
        float oldZoom = 2f;
        float newZoom = 1f;
        float zoomPivot = 5f;
        int scroll = 1;

        int delta = ZoomUtils.scrollDeltaNeededForZoomChange(oldZoom, newZoom, zoomPivot,
                scroll);

        assertTrue(delta <= 0);
    }

    @Test
    public void constrainCoordinate_contentLargerThanViewport_returnsZero() {
        float zoom = 1f;
        int scroll = 0;
        int rawContentDimension = 5;
        int viewportDimension = 5;

        int adjustedCoordinate = ZoomUtils.constrainCoordinate(zoom, scroll,
                rawContentDimension, viewportDimension);

        assertEquals(0, adjustedCoordinate);
    }

    @Test
    public void constrainCoordinate_scaledContentWithinViewport_returnsNegativeAdjustedCoord() {
        float zoom = 1f;
        int scroll = 0;
        int rawContentDimension = 6;
        int viewportDimension = 8;

        int adjustedCoordinate = ZoomUtils.constrainCoordinate(zoom, scroll,
                rawContentDimension, viewportDimension);

        assertTrue(adjustedCoordinate <= 0);
    }

    @Test
    public void constrainCoordinate_contentWithLeftDeadMargins_returnsPositiveAdjustedCoord() {
        float zoom = 1f;
        int scroll = -2;
        int rawContentDimension = 6;
        int viewportDimension = 5;

        int adjustedCoordinate = ZoomUtils.constrainCoordinate(zoom, scroll,
                rawContentDimension, viewportDimension);

        assertTrue(adjustedCoordinate >= 0);
    }

    @Test
    public void constrainCoordinate_contentWithRightDeadMargin_returnsNegativeAdjustedCoord() {
        float zoom = 1f;
        int scroll = 2;
        int rawContentDimension = 6;
        int viewportDimension = 5;

        int adjustedCoordinate = ZoomUtils.constrainCoordinate(zoom, scroll,
                rawContentDimension, viewportDimension);

        assertTrue(adjustedCoordinate <= 0);
    }

    @Test
    public void test_DoubleTap__whenZoomedOut() {
        int viewportWidth = 400;
        int viewportHeight = 1000;
        int contentWidth = 50;
        float currentZoom = 1f;

        float newZoom = ZoomUtils.calculateZoomForDoubleTap(
                viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
        );
        assertEquals(8f, newZoom, 0.001);
    }

    @Test
    public void test_DoubleTap__whenZoomedIn() {
        int viewportWidth = 100;
        int viewportHeight = 1000;
        int contentWidth = 200;
        float currentZoom = 1f;

        float newZoom = ZoomUtils.calculateZoomForDoubleTap(
                viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
        );
        assertEquals(0.5f, newZoom, 0.001);
    }

    @Test
    public void test_DoubleTap__whenCurrentZoomIsSameAsFitZoom() {
        int viewportWidth = 400;
        int viewportHeight = 1000;
        int contentWidth = 50;
        float currentZoom = 8f;

        float newZoom = ZoomUtils.calculateZoomForDoubleTap(
                viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
        );
        assertEquals(16f, newZoom, 0.001);
    }

    @Test
    public void test_DoubleTap__whenCurrentZoomSimilarToFitZoom() {
        int viewportWidth = 400;
        int viewportHeight = 1000;
        int contentWidth = 50;
        float currentZoom = 7.76f;

        float newZoom = ZoomUtils.calculateZoomForDoubleTap(
                viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
        );
        assertEquals(16f, newZoom, 0.001);
    }

    @Test
    public void test_DoubleTap__validatingFirstAndThirdZoomAreSame() {
        int viewportWidth = 400;
        int viewportHeight = 1000;
        int contentWidth = 50;

        while (contentWidth < 40000) {
            float currentZoom = 0.1f;
            while (currentZoom < 30f) {
                float newZoom1 = ZoomUtils.calculateZoomForDoubleTap(
                        viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
                );
                ZoomUtils.calculateZoomForDoubleTap(
                        viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
                );
                float newZoom3 = ZoomUtils.calculateZoomForDoubleTap(
                        viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
                );
                // The first and third zoom levels should match because double-tap toggles between
                // consistent zoom states (eg. fit-to-width and double ZoomIn/Original size 1f) in a
                // predictable cycle.
                assertEquals(newZoom3, newZoom1, 0.001);
                currentZoom += 0.1f;
            }
            contentWidth *= 2;
        }
    }

    @Test
    public void test_DoubleTap__currentZoomIsSameAsMaxZoom() {
        int viewportWidth = 2500;
        int viewportHeight = 10000;
        int contentWidth = 100;
        float currentZoom = 25f;

        float newZoom = ZoomUtils.calculateZoomForDoubleTap(
                viewportWidth, viewportHeight, contentWidth, currentZoom, MINZOOM, MAXZOOM
        );
        assertEquals(1f, newZoom, 0.001);
    }

}
