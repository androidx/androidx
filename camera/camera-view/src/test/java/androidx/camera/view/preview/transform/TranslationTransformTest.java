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

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.View.LAYOUT_DIRECTION_RTL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.preview.transform.transformation.TranslationTransformation;
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
public class TranslationTransformTest {

    private static final int CONTAINER_WIDTH = 500;
    private static final int CONTAINER_HEIGHT = 800;
    private static final int VIEW_WIDTH = 200;
    private static final int VIEW_HEIGHT = 300;

    @Test
    public void start_viewNotScaled_rotation0() {
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(0);
        assertThat(transformation.getTransY()).isEqualTo(0);
    }

    @Test
    public void start_viewNotScaled_rotation90_deviceRotation0() {
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(0);
        assertThat(transformation.getTransY()).isEqualTo(0);
    }

    @Test
    public void start_viewNotScaled_rotation90() {
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(50);
        assertThat(transformation.getTransY()).isEqualTo(-50);
    }

    @Test
    public void start_viewNotScaled_rotation0_deviceRotation90() {
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(50);
        assertThat(transformation.getTransY()).isEqualTo(-50);
    }

    @Test
    public void start_viewScaled_rotation0() {
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(100);
        assertThat(transformation.getTransY()).isEqualTo(-75);
    }

    @Test
    public void start_viewScaled_rotation90_deviceRotation0() {
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(100);
        assertThat(transformation.getTransY()).isEqualTo(-75);
    }

    @Test
    public void start_viewScaled_rotation90() {
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(-25);
        assertThat(transformation.getTransY()).isEqualTo(50);
    }

    @Test
    public void start_viewScaled_rotation0_deviceRotation90() {
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(-25);
        assertThat(transformation.getTransY()).isEqualTo(50);
    }

    @Test
    public void start_rtlLayout_rotation0() {
        final View view = setUpView(Surface.ROTATION_0, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(-100);
        assertThat(transformation.getTransY()).isEqualTo(-75);
    }

    @Test
    public void start_rtlLayout_rotation90_deviceRotation0() {
        final View view = setUpView(Surface.ROTATION_90, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(-100);
        assertThat(transformation.getTransY()).isEqualTo(-75);
    }

    @Test
    public void start_rtlLayout_rotation90() {
        final View view = setUpView(Surface.ROTATION_90, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(25);
        assertThat(transformation.getTransY()).isEqualTo(50);
    }

    @Test
    public void start_rtlLayout_rotation0_deviceRotation90() {
        final View view = setUpView(Surface.ROTATION_0, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.start(view, scaleXY,
                Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(25);
        assertThat(transformation.getTransY()).isEqualTo(50);
    }

    @Test
    public void center_viewNotScaled_rotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.center(container,
                view);

        assertThat(transformation.getTransX()).isEqualTo(150);
        assertThat(transformation.getTransY()).isEqualTo(250);
    }

    @Test
    public void center_viewNotScaled_rotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.center(container,
                view);

        assertThat(transformation.getTransX()).isEqualTo(150);
        assertThat(transformation.getTransY()).isEqualTo(250);
    }

    @Test
    public void center_viewScaled_rotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.center(container,
                view);

        assertThat(transformation.getTransX()).isEqualTo(150);
        assertThat(transformation.getTransY()).isEqualTo(250);
    }

    @Test
    public void center_viewScaled_rotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.center(container,
                view);

        assertThat(transformation.getTransX()).isEqualTo(150);
        assertThat(transformation.getTransY()).isEqualTo(250);
    }

    @Test
    public void center_rtlLayout() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.center(container,
                view);

        assertThat(transformation.getTransX()).isEqualTo(-150);
        assertThat(transformation.getTransY()).isEqualTo(250);
    }

    @Test
    public void end_viewNotScaled_rotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(300);
        assertThat(transformation.getTransY()).isEqualTo(500);
    }

    @Test
    public void end_viewNotScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(300);
        assertThat(transformation.getTransY()).isEqualTo(500);
    }

    @Test
    public void end_viewNotScaled_rotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(250);
        assertThat(transformation.getTransY()).isEqualTo(550);
    }

    @Test
    public void end_viewNotScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(250);
        assertThat(transformation.getTransY()).isEqualTo(550);
    }

    @Test
    public void end_viewScaled_rotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(200);
        assertThat(transformation.getTransY()).isEqualTo(575);
    }

    @Test
    public void end_viewScaled_rotation90_deviceRotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(200);
        assertThat(transformation.getTransY()).isEqualTo(575);
    }

    @Test
    public void end_viewScaled_rotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(325);
        assertThat(transformation.getTransY()).isEqualTo(450);
    }

    @Test
    public void end_viewScaled_rotation0_deviceRotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0);
        final Pair<Float, Float> scaleXY = new Pair<>(2F, 0.5F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(325);
        assertThat(transformation.getTransY()).isEqualTo(450);
    }

    @Test
    public void end_rtlLayout_rotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(-300);
        assertThat(transformation.getTransY()).isEqualTo(500);
    }

    @Test
    public void end_rtlLayout_rotation90_deviceRotation0() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_0);

        assertThat(transformation.getTransX()).isEqualTo(-300);
        assertThat(transformation.getTransY()).isEqualTo(500);
    }

    @Test
    public void end_rtlLayout_rotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_90, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, RotationTransform.ROTATION_AUTOMATIC);

        assertThat(transformation.getTransX()).isEqualTo(-250);
        assertThat(transformation.getTransY()).isEqualTo(550);
    }

    @Test
    public void end_rtlLayout_rotation0_deviceRotation90() {
        final View container = setUpContainer();
        final View view = setUpView(Surface.ROTATION_0, LAYOUT_DIRECTION_RTL);
        final Pair<Float, Float> scaleXY = new Pair<>(1F, 1F);
        final TranslationTransformation transformation = TranslationTransform.end(container, view,
                scaleXY, Surface.ROTATION_90);

        assertThat(transformation.getTransX()).isEqualTo(-250);
        assertThat(transformation.getTransY()).isEqualTo(550);
    }

    @NonNull
    private View setUpView(final int rotation) {
        return setUpView(rotation, LAYOUT_DIRECTION_LTR);
    }

    @NonNull
    private View setUpView(final int rotation, final int layoutDirection) {
        final View view = mock(View.class);
        when(view.getWidth()).thenReturn(VIEW_WIDTH);
        when(view.getHeight()).thenReturn(VIEW_HEIGHT);
        when(view.getLayoutDirection()).thenReturn(layoutDirection);

        final Display display = mock(Display.class);
        when(view.getDisplay()).thenReturn(display);
        when(display.getRotation()).thenReturn(rotation);

        return view;
    }

    @NonNull
    private View setUpContainer() {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(CONTAINER_WIDTH);
        when(container.getHeight()).thenReturn(CONTAINER_HEIGHT);
        return container;
    }
}
