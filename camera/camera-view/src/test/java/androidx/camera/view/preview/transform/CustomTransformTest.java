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

import android.graphics.Matrix;
import android.view.View;

import androidx.camera.view.preview.transform.transformation.Transformation;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CustomTransformTest {

    private View mView;

    @Before
    public void setUp() {
        mView = mock(View.class);
        when(mView.getX()).thenReturn(50F);
        when(mView.getY()).thenReturn(20F);
    }

    @Test
    public void getScale() {
        final float scaleX = 2.5F;
        final float scaleY = -3F;
        final Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);

        final Transformation transformation = CustomTransform.getTransformation(mView, matrix);

        assertThat(transformation.getScaleX()).isEqualTo(scaleX);
        assertThat(transformation.getScaleY()).isEqualTo(scaleY);
    }

    @Test
    public void getRotation_lessThan360() {
        final float rotationDegrees = 47.5F;
        final Matrix matrix = new Matrix();
        matrix.setRotate(rotationDegrees);

        final Transformation transformation = CustomTransform.getTransformation(mView, matrix);

        assertThat(transformation.getRotation()).isEqualTo(rotationDegrees);
    }

    @Test
    public void getRotation_greaterThan360() {
        final float rotationDegrees = 47.5F;
        final Matrix matrix = new Matrix();
        matrix.setRotate(rotationDegrees + 360);

        final Transformation transformation = CustomTransform.getTransformation(mView, matrix);

        assertThat(transformation.getRotation()).isEqualTo(rotationDegrees);
    }
}
