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

package androidx.camera.view.preview.transform;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class RotationTransformTest {
    private final int[] mPossibleRotations = {Surface.ROTATION_0, Surface.ROTATION_90,
            Surface.ROTATION_180, Surface.ROTATION_270};

    @Test
    public void rotation_0() {
        final View view = setUpView(Surface.ROTATION_0);
        assertThat(RotationTransform.getRotationDegrees(view)).isEqualTo(0);
    }

    @Test
    public void rotation_90() {
        final View view = setUpView(Surface.ROTATION_90);
        assertThat(RotationTransform.getRotationDegrees(view)).isEqualTo(90);
    }

    @Test
    public void rotation_180() {
        final View view = setUpView(Surface.ROTATION_180);
        assertThat(RotationTransform.getRotationDegrees(view)).isEqualTo(180);
    }

    @Test
    public void rotation_270() {
        final View view = setUpView(Surface.ROTATION_270);
        assertThat(RotationTransform.getRotationDegrees(view)).isEqualTo(270);
    }

    @Test
    public void viewAndDeviceRotationCombinations() {
        for (int viewRotation : mPossibleRotations) {
            final View view = setUpView(viewRotation);

            // If the device rotation value is available, the return degrees will be calculated
            // from the device rotation value.
            for (int deviceRotation : mPossibleRotations) {
                int deviceRotationDegrees =
                        SurfaceRotation.rotationDegreesFromSurfaceRotation(deviceRotation);
                assertThat(
                        RotationTransform.getRotationDegrees(view, deviceRotation)).isEqualTo(
                        deviceRotationDegrees);
            }

            // If the device rotation value is ROTATION_AUTOMATIC, the return degrees will be
            // calculated from the view.
            int viewRotationDegrees =
                    SurfaceRotation.rotationDegreesFromSurfaceRotation(viewRotation);
            assertThat(RotationTransform.getRotationDegrees(view,
                    RotationTransform.ROTATION_AUTOMATIC)).isEqualTo(viewRotationDegrees);
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unknown_rotation() {
        final View view = setUpView(-1);
        RotationTransform.getRotationDegrees(view);
    }

    private View setUpView(int rotation) {
        final View view = mock(View.class);
        final Display display = mock(Display.class);
        when(view.getDisplay()).thenReturn(display);
        when(display.getRotation()).thenReturn(rotation);
        return view;
    }
}
