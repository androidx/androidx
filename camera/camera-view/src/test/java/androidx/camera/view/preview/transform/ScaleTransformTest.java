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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.test.filters.SmallTest;

import org.junit.Before;
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

    private static final Rect CONTAINER = new Rect(0, 0, 400, 600);
    private static final Size BUFFER = new Size(400, 300);

    private Display mDisplay;
    private View mView;

    @Before
    public void setUp() {
        final Context context = mock(Context.class);
        final WindowManager windowManager = mock(WindowManager.class);
        when(context.getSystemService(anyString())).thenReturn(windowManager);

        mDisplay = mock(Display.class);
        when(windowManager.getDefaultDisplay()).thenReturn(mDisplay);

        mView = mock(View.class);
        when(mView.getWidth()).thenReturn(BUFFER.getWidth());
        when(mView.getHeight()).thenReturn(BUFFER.getHeight());
        when(mView.getContext()).thenReturn(context);
    }

    @Test
    public void fill_naturalPortraitOrientedDevice_portrait() {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(CONTAINER.width());
        when(container.getHeight()).thenReturn(CONTAINER.height());

        setDeviceNaturalPortraitOriented_portrait();

        final Pair<Float, Float> scaleXY = ScaleTransform.fill(container, mView, BUFFER);
        assertThat(mView.getWidth() * scaleXY.first).isAtLeast(container.getWidth());
        assertThat(mView.getHeight() * scaleXY.second).isAtLeast(container.getHeight());
    }

    @Test
    public void fill_naturalPortraitOrientedDevice_landscape() {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(CONTAINER.height());
        when(container.getHeight()).thenReturn(CONTAINER.width());

        setDeviceNaturalPortraitOriented_landscape();

        final Pair<Float, Float> scaleXY = ScaleTransform.fill(container, mView, BUFFER);
        assertThat(mView.getWidth() * scaleXY.first).isAtLeast(container.getHeight());
        assertThat(mView.getHeight() * scaleXY.second).isAtLeast(container.getWidth());
    }

    @Test
    public void fill_naturalLandscapeOrientedDevice_portrait() {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(CONTAINER.width());
        when(container.getHeight()).thenReturn(CONTAINER.height());

        setDeviceNaturalLandscapeOriented_portrait();

        final Pair<Float, Float> scaleXY = ScaleTransform.fill(container, mView, BUFFER);
        assertThat(mView.getWidth() * scaleXY.first).isAtLeast(container.getHeight());
        assertThat(mView.getHeight() * scaleXY.second).isAtLeast(container.getWidth());
    }

    @Test
    public void fill_naturalLandscapeOrientedDevice_landscape() {
        final View container = mock(View.class);
        when(container.getWidth()).thenReturn(CONTAINER.height());
        when(container.getHeight()).thenReturn(CONTAINER.width());

        setDeviceNaturalLandscapeOriented_landscape();

        final Pair<Float, Float> scaleXY = ScaleTransform.fill(container, mView, BUFFER);
        assertThat(mView.getWidth() * scaleXY.first).isAtLeast(container.getWidth());
        assertThat(mView.getHeight() * scaleXY.second).isAtLeast(container.getHeight());
    }

    private void setDeviceNaturalPortraitOriented_portrait() {
        setUpDeviceNaturalOrientation(Surface.ROTATION_0, 300, 400);
    }

    private void setDeviceNaturalPortraitOriented_landscape() {
        setUpDeviceNaturalOrientation(Surface.ROTATION_90, 400, 300);
    }

    private void setDeviceNaturalLandscapeOriented_portrait() {
        setUpDeviceNaturalOrientation(Surface.ROTATION_90, 300, 400);
    }

    private void setDeviceNaturalLandscapeOriented_landscape() {
        setUpDeviceNaturalOrientation(Surface.ROTATION_0, 400, 300);
    }

    private void setUpDeviceNaturalOrientation(final int rotation, final int width,
            final int height) {
        when(mDisplay.getRotation()).thenReturn(rotation);
        doAnswer(invocation -> {
            final Point point = invocation.getArgument(0);
            point.set(width, height);
            return null;
        }).when(mDisplay).getRealSize(any(Point.class));
    }
}
