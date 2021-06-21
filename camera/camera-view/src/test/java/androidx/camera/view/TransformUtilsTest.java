/*
 * Copyright 2021 The Android Open Source Project
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

import static androidx.camera.view.TransformUtils.getExifTransform;
import static androidx.camera.view.TransformUtils.rectToVertices;

import static com.google.common.truth.Truth.assertThat;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Unit tests for {@link TransformUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class TransformUtilsTest {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;

    @Test
    public void exifOrientation_flipHorizontal() {
        verifyExifOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, new float[]{
                WIDTH, 0, 0, 0, 0, HEIGHT, WIDTH, HEIGHT
        });
    }

    @Test
    public void exifOrientation_flipVertical() {
        verifyExifOrientation(ExifInterface.ORIENTATION_FLIP_VERTICAL, new float[]{
                0, HEIGHT, WIDTH, HEIGHT, WIDTH, 0, 0, 0
        });
    }

    @Test
    public void exifOrientation_normal() {
        verifyExifOrientation(ExifInterface.ORIENTATION_NORMAL, new float[]{
                0, 0, WIDTH, 0, WIDTH, HEIGHT, 0, HEIGHT
        });
    }

    @Test
    public void exifOrientation_undefined() {
        verifyExifOrientation(ExifInterface.ORIENTATION_UNDEFINED, new float[]{
                0, 0, WIDTH, 0, WIDTH, HEIGHT, 0, HEIGHT
        });
    }

    @Test
    public void exifOrientation_rotate90() {
        verifyExifOrientation(ExifInterface.ORIENTATION_ROTATE_90, new float[]{
                HEIGHT, 0, HEIGHT, WIDTH, 0, WIDTH, 0, 0
        });
    }

    @Test
    public void exifOrientation_rotate180() {
        verifyExifOrientation(ExifInterface.ORIENTATION_ROTATE_180, new float[]{
                WIDTH, HEIGHT, 0, HEIGHT, 0, 0, WIDTH, 0
        });
    }

    @Test
    public void exifOrientation_rotate270() {
        verifyExifOrientation(ExifInterface.ORIENTATION_ROTATE_270, new float[]{
                0, WIDTH, 0, 0, HEIGHT, 0, HEIGHT, WIDTH
        });
    }

    @Test
    public void exifOrientation_transpose() {
        verifyExifOrientation(ExifInterface.ORIENTATION_TRANSPOSE, new float[]{
                0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT, 0
        });
    }

    @Test
    public void exifOrientation_transverse() {
        verifyExifOrientation(ExifInterface.ORIENTATION_TRANSVERSE, new float[]{
                HEIGHT, WIDTH, HEIGHT, 0, 0, 0, 0, WIDTH
        });
    }

    private void verifyExifOrientation(int orientationFlag, float[] mappedVertices) {
        float[] vertices = rectToVertices(new RectF(0, 0, WIDTH, HEIGHT));
        Matrix matrix = getExifTransform(orientationFlag, WIDTH, HEIGHT);
        matrix.mapPoints(vertices);
        for (int i = 0; i < vertices.length; i++) {
            assertThat(vertices[i]).isWithin(1E-4F).of(mappedVertices[i]);
        }
    }
}
