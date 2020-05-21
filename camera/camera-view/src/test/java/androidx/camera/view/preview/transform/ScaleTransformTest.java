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

import androidx.annotation.NonNull;
import androidx.camera.view.preview.transform.transformation.ScaleTransformation;
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
public class ScaleTransformTest {

    @Test
    public void fill_viewNotScaled_rotation0() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 1F, 1F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fill(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fill_viewNotScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 1F, 1F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fill(container, view, Surface.ROTATION_0);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fill_viewNotScaled_rotation90() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 1F, 1F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fill(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fill_viewNotScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 1F, 1F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fill(container, view,
                Surface.ROTATION_90);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fill_viewScaled_rotation0() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 2F, 1.5F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fill(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fill_viewScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 2F, 1.5F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fill(container, view, Surface.ROTATION_0);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fill_viewScaled_rotation90() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 2F, 1.5F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fill(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fill_viewScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer(300, 400);
        final View view = setUpView(200, 400, 2F, 1.5F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fill(container, view, Surface.ROTATION_90);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtLeast(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtLeast(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fit_viewNotScaled_rotation0() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 1F, 1F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fit(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fit_viewNotScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 1F, 1F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fit(container, view, Surface.ROTATION_0);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fit_viewNotScaled_rotation90() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 1F, 1F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fit(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fit_viewNotScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 1F, 1F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fit(container, view, Surface.ROTATION_90);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fit_viewScaled_rotation0() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 0.5F, 2F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fit(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fit_viewScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 0.5F, 2F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fit(container, view, Surface.ROTATION_0);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getWidth());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getHeight());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getWidth()
                || getScaledHeight(view) * scale.getScaleY() == container.getHeight()).isTrue();
    }

    @Test
    public void fit_viewScaled_rotation90() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 0.5F, 2F, Surface.ROTATION_90);

        final ScaleTransformation scale = ScaleTransform.fit(container, view,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @Test
    public void fit_viewScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer(200, 500);
        final View view = setUpView(500, 1000, 0.5F, 2F, Surface.ROTATION_0);

        final ScaleTransformation scale = ScaleTransform.fit(container, view, Surface.ROTATION_90);

        assertThat(scale.getScaleX()).isEqualTo(scale.getScaleY());
        assertThat(getScaledWidth(view) * scale.getScaleX()).isAtMost(container.getHeight());
        assertThat(getScaledHeight(view) * scale.getScaleY()).isAtMost(container.getWidth());
        assertThat(getScaledWidth(view) * scale.getScaleX() == container.getHeight()
                || getScaledHeight(view) * scale.getScaleY() == container.getWidth()).isTrue();
    }

    @NonNull
    private View setUpContainer(int width, int height) {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(width);
        when(container.getHeight()).thenReturn(height);
        return container;
    }

    @NonNull
    private View setUpView(int width, int height, float scaleX, float scaleY, int rotation) {
        final View view = mock(View.class);
        when(view.getWidth()).thenReturn(width);
        when(view.getHeight()).thenReturn(height);
        when(view.getScaleX()).thenReturn(scaleX);
        when(view.getScaleY()).thenReturn(scaleY);

        final Display display = mock(Display.class);
        when(view.getDisplay()).thenReturn(display);
        when(display.getRotation()).thenReturn(rotation);

        return view;
    }

    private float getScaledWidth(@NonNull final View view) {
        return view.getWidth() * view.getScaleX();
    }

    private float getScaledHeight(@NonNull final View view) {
        return view.getHeight() * view.getScaleY();
    }
}
