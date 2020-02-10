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

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class GravityTransformTest {

    private static final Rect BIG_RECT = new Rect(0, 0, 400, 600);
    private static final Rect SMALL_RECT = new Rect(0, 0, 100, 200);

    @Test
    public void start() {
        final Point point = GravityTransform.start();
        assertThat(point).isEqualTo(new Point(0, 0));
    }

    @Test
    public void center_whenContainerLargerThanView() {
        final View container = mock(View.class);
        setUpView(container, BIG_RECT);

        final View view = mock(View.class);
        setUpView(view, SMALL_RECT);

        final Point point = GravityTransform.center(container, view);
        assertThat(point).isEqualTo(new Point(150, 200));
    }

    @Test
    public void center_whenContainerSmallerThanView() {
        final View container = mock(View.class);
        setUpView(container, SMALL_RECT);

        final View view = mock(View.class);
        setUpView(view, BIG_RECT);

        final Point point = GravityTransform.center(container, view);
        assertThat(point).isEqualTo(new Point(-150, -200));
    }

    @Test
    public void end_whenContainerLargerThanView() {
        final View container = mock(View.class);
        setUpView(container, BIG_RECT);

        final View view = mock(View.class);
        setUpView(view, SMALL_RECT);

        final Point point = GravityTransform.end(container, view);
        assertThat(point).isEqualTo(new Point(300, 400));
    }

    @Test
    public void end_whenContainerSmallerThanView() {
        final View container = mock(View.class);
        setUpView(container, SMALL_RECT);

        final View view = mock(View.class);
        setUpView(view, BIG_RECT);

        final Point point = GravityTransform.end(container, view);
        assertThat(point).isEqualTo(new Point(-300, -400));
    }

    private void setUpView(@NonNull final View container, @NonNull final Rect size) {
        when(container.getWidth()).thenReturn(size.width());
        when(container.getHeight()).thenReturn(size.height());
    }
}
