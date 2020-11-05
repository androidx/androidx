/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.os.Build;
import android.view.Display;
import android.view.Surface;

import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class DisplayOrientedMeteringPointFactoryTest {
    private static final float WIDTH = 480;
    private static final float HEIGHT = 640;

    private Display mMockDisplay;

    private CameraInfo mFrontCameraInfo;
    private CameraInfo mBackCameraInfo;

    @Before
    public void setUp() {
        mFrontCameraInfo = new FakeCameraInfoInternal(270, CameraSelector.LENS_FACING_FRONT);
        mBackCameraInfo = new FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK);

        mMockDisplay = Mockito.mock(Display.class);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
    }

    @Test
    public void defaultAreaSize() {
        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        MeteringPoint point = factory.createPoint(0, 0);
        assertThat(point.getSize()).isEqualTo(MeteringPointFactory.getDefaultPointSize());
        assertThat(point.getSurfaceAspectRatio()).isNull();
    }

    @Test
    public void createPointWithValidAreaSize() {
        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        final float areaSize = 0.2f;
        MeteringPoint point = factory.createPoint(0, 0, areaSize);
        assertThat(point.getSize()).isEqualTo(areaSize);
        assertThat(point.getSurfaceAspectRatio()).isNull();
    }

    @Test
    public void display0_back() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(1f);
        assertThat(meteringPoint2.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(0f);
        assertThat(meteringPoint3.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(0f);
    }

    @Test
    public void display0_front() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mFrontCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(1f);
        assertThat(meteringPoint.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(0f);
        assertThat(meteringPoint4.getY()).isEqualTo(0f);
    }

    @Test
    public void display90_back() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_90);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(1f);
    }

    @Test
    public void display90_front() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_90);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mFrontCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(1f);
        assertThat(meteringPoint.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(1f);
        assertThat(meteringPoint2.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(0f);
        assertThat(meteringPoint3.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(0f);
        assertThat(meteringPoint4.getY()).isEqualTo(1f);
    }

    @Test
    public void display180_back() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_180);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(1f);
        assertThat(meteringPoint.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(0f);
        assertThat(meteringPoint4.getY()).isEqualTo(1f);
    }

    @Test
    public void display180_front() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_180);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mFrontCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(1f);
        assertThat(meteringPoint2.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(0f);
        assertThat(meteringPoint3.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(1f);
    }

    @Test
    public void display270_back() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_270);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mBackCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(1f);
        assertThat(meteringPoint.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(1f);
        assertThat(meteringPoint2.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(0f);
        assertThat(meteringPoint3.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(0f);
        assertThat(meteringPoint4.getY()).isEqualTo(0f);
    }

    @Test
    public void display270_front() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_270);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mFrontCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(0f);
    }

    @Test
    public void display0_front_useCustomDisplay() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_270);

        DisplayOrientedMeteringPointFactory factory = new DisplayOrientedMeteringPointFactory(
                mMockDisplay, mFrontCameraInfo, WIDTH, HEIGHT);

        MeteringPoint meteringPoint = factory.createPoint(0f, 0f);
        assertThat(meteringPoint.getX()).isEqualTo(0f);
        assertThat(meteringPoint.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint2 = factory.createPoint(0f, HEIGHT);
        assertThat(meteringPoint2.getX()).isEqualTo(0f);
        assertThat(meteringPoint2.getY()).isEqualTo(0f);

        MeteringPoint meteringPoint3 = factory.createPoint(WIDTH, 0f);

        assertThat(meteringPoint3.getX()).isEqualTo(1f);
        assertThat(meteringPoint3.getY()).isEqualTo(1f);

        MeteringPoint meteringPoint4 = factory.createPoint(WIDTH, HEIGHT);

        assertThat(meteringPoint4.getX()).isEqualTo(1f);
        assertThat(meteringPoint4.getY()).isEqualTo(0f);
    }

}
