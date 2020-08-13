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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.graphics.Point;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.MeteringPoint;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
public class PreviewViewMeteringPointFactoryTest {
    private static final Size RESOLUTION = new Size(2000, 1000);
    private static final int VIEW_WIDTH = 100;
    private static final int VIEW_HEIGHT = 100;

    @ParameterizedRobolectricTestRunner.Parameter(0)
    public CameraInfo mCameraInfo;

    // Useless, just for showing parameter name during test.
    @ParameterizedRobolectricTestRunner.Parameter(1)
    public String mName;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[]{new FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK),
                "Back camera"});
        result.add(new Object[]{new FakeCameraInfoInternal(270, CameraSelector.LENS_FACING_FRONT),
                "Front camera"});
        return result;
    }

    private Display mDisplay;

    @Before
    public void setUp() {
        mDisplay = Mockito.mock(Display.class);

        mockDisplay(Surface.ROTATION_0, 1080, 1920);
    }

    @Test
    public void fillCenter_rotation0() {
        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 200;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_CENTER, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(0, 50));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth, 50));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(0, adjustedViewHeight - 50));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight - 50));
    }

    @Test
    public void fillStart_rotation0() {
        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 200;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_START, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(0, 0));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth, 0));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(0, adjustedViewHeight - 100));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight - 100));
    }

    @Test
    public void fillEnd_rotation0() {
        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 200;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_END, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(0, 100));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth, 100));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(0, adjustedViewHeight));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight));
    }

    @Test
    public void fillCenter_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 200;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_CENTER, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(50, 0));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth - 50, 0));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(50, adjustedViewHeight));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth - 50, adjustedViewHeight));
    }

    @Test
    public void fillStart_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 200;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_START, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(0, 0));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth - 100, 0));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(0, adjustedViewHeight));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth - 100, adjustedViewHeight));
    }

    @Test
    public void fillEnd_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 200;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FILL_END, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                factory.createPoint(0, 0),
                displayFactory.createPoint(100, 0));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, 0),
                displayFactory.createPoint(adjustedViewWidth, 0));
        assertEqual(
                factory.createPoint(0, VIEW_HEIGHT),
                displayFactory.createPoint(100, adjustedViewHeight));
        assertEqual(
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT),
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight));
    }

    @Test
    public void fitCenter_rotation0() {
        final int adjustedViewWidth = 50;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_CENTER, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(25, 0));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH - 25, 0));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(25, VIEW_HEIGHT));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH - 25, VIEW_HEIGHT)
        );
    }

    @Test
    public void fitStart_rotation0() {
        final int adjustedViewWidth = 50;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_START, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(0, 0));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH - 50, 0));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(0, VIEW_HEIGHT));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH - 50, VIEW_HEIGHT));
    }

    @Test
    public void fitEnd_rotation0() {
        final int adjustedViewWidth = 50;
        final int adjustedViewHeight = 100;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_END, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(50, 0));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH, 0));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(50, VIEW_HEIGHT));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT));
    }

    @Test
    public void fitCenter_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 50;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_CENTER, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(0, 25));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH, 25));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(0, VIEW_HEIGHT - 25));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT - 25)
        );
    }

    @Test
    public void fitStart_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 50;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_START, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(0, 0));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH, 0));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(0, VIEW_HEIGHT - 50));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT - 50)
        );
    }

    @Test
    public void fitEnd_rotation90() {
        mockDisplay(Surface.ROTATION_90, 1920, 1080);

        final int adjustedViewWidth = 100;
        final int adjustedViewHeight = 50;
        PreviewViewMeteringPointFactory factory = new PreviewViewMeteringPointFactory(mDisplay,
                mCameraInfo, RESOLUTION, PreviewView.ScaleType.FIT_END, VIEW_WIDTH,
                VIEW_HEIGHT);

        DisplayOrientedMeteringPointFactory displayFactory =
                new DisplayOrientedMeteringPointFactory(mDisplay, mCameraInfo,
                        adjustedViewWidth,
                        adjustedViewHeight);

        assertEqual(
                displayFactory.createPoint(0, 0),
                factory.createPoint(0, 50));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, 0),
                factory.createPoint(VIEW_WIDTH, 50));
        assertEqual(
                displayFactory.createPoint(0, adjustedViewHeight),
                factory.createPoint(0, VIEW_HEIGHT));
        assertEqual(
                displayFactory.createPoint(adjustedViewWidth, adjustedViewHeight),
                factory.createPoint(VIEW_WIDTH, VIEW_HEIGHT)
        );
    }

    private void mockDisplay(int rotation, int displayWidth, int displayHeight) {
        when(mDisplay.getRotation()).thenReturn(rotation);
        doAnswer(invocation -> {
            Point point = invocation.getArgument(0);
            point.x = displayWidth;
            point.y = displayHeight;
            return null;
        }).when(mDisplay).getRealSize(any(Point.class));
    }

    private void assertEqual(@NonNull MeteringPoint point1, @NonNull MeteringPoint point2) {
        assertThat(point1.getX()).isEqualTo(point2.getX());
        assertThat(point1.getY()).isEqualTo(point2.getY());
    }
}
