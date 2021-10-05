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

package androidx.camera.core.internal.utils;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Base64;
import android.util.Rational;
import android.util.Size;

import androidx.camera.core.ImageProxy;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.nio.ByteBuffer;

/**
 * Unit tests for {@link ImageUtil}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageUtilTest {
    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final Rational ASPECT_RATIO = new Rational(WIDTH, HEIGHT);
    private static final int CROP_WIDTH = 100;
    private static final int CROP_HEIGHT = 100;
    private static final int DEFAULT_JPEG_QUALITY = 100;
    private static final String JPEG_IMAGE_DATA_BASE_64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB"
                    + "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEB"
                    + "AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAB4AKADASIA"
                    + "AhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQA"
                    + "AAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3"
                    + "ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWm"
                    + "p6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEA"
                    + "AwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSEx"
                    + "BhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElK"
                    + "U1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3"
                    + "uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD/AD/6"
                    + "KKK/8/8AP/P/AAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
                    + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKA"
                    + "CiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAK"
                    + "KKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAoo"
                    + "ooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiii"
                    + "gAooooAKKKKACiiigAooooAKKKKACiiigAooooAKKKKACiiigAooooA//9k=";
    private FakeImageProxy mImage;
    @Mock
    private final ImageProxy.PlaneProxy mDataPlane = mock(ImageProxy.PlaneProxy.class);
    private final ByteBuffer mDataBuffer =
            ByteBuffer.wrap(Base64.decode(JPEG_IMAGE_DATA_BASE_64, Base64.DEFAULT));
    private byte[] mDataByteArray = new byte[mDataBuffer.capacity()];

    @Before
    public void setUp() {
        mImage = new FakeImageProxy(new FakeImageInfo());
        mImage.setFormat(ImageFormat.JPEG);
        mImage.setWidth(WIDTH);
        mImage.setHeight(HEIGHT);
        mImage.setCropRect(new Rect(0, 0, WIDTH, HEIGHT));

        when(mDataPlane.getBuffer()).thenReturn(mDataBuffer);
        mImage.setPlanes(new ImageProxy.PlaneProxy[]{mDataPlane});
        mDataBuffer.get(mDataByteArray);
        mDataBuffer.clear();
    }

    @Test
    public void rotateAspectRatioFor90Degrees_rotated() {
        // Arrange.
        Rational aspectRatio = new Rational(3, 4);

        // Assert: return a rotated value.
        assertThat(ImageUtil.getRotatedAspectRatio(90, aspectRatio)).isEqualTo(new Rational(4, 3));
    }

    @Test
    public void rotateRectFor180Degrees_rectUnchanged() {
        // Arrange.
        Rational aspectRatio = new Rational(3, 4);

        // Assert: return the original value.
        assertThat(ImageUtil.getRotatedAspectRatio(180, aspectRatio)).isEqualTo(aspectRatio);
    }

    @Test
    public void canTransformJpegImageToByteArray() {
        byte[] byteArray = ImageUtil.jpegImageToJpegByteArray(mImage);
        assertThat(byteArray).isEqualTo(mDataByteArray);
    }

    @Test
    public void canCropJpegByteArray() throws ImageUtil.CodecFailedException {
        byte[] byteArray = ImageUtil.jpegImageToJpegByteArray(mImage,
                new Rect(0, 0, CROP_WIDTH, CROP_HEIGHT), DEFAULT_JPEG_QUALITY);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        assertEquals(CROP_WIDTH, bitmap.getWidth());
        assertEquals(CROP_HEIGHT, bitmap.getHeight());
    }

    @Test
    public void canCheckInvalidAspectRatio() {
        Rational invalidAspectRatio = new Rational(-1, 2);
        assertThat(ImageUtil.isAspectRatioValid(invalidAspectRatio)).isFalse();
    }

    @Test
    public void canCheckAspectRatioHasEffect() {
        assertThat(
                ImageUtil.isAspectRatioValid(new Size(WIDTH, HEIGHT), ASPECT_RATIO)).isFalse();
    }

    @Test
    public void canComputeRectFromAspectRatio() {
        Rational targetAspectRatio = new Rational(2, 1);
        Rect resultRect = ImageUtil.computeCropRectFromAspectRatio(new Size(WIDTH, HEIGHT),
                targetAspectRatio);

        // Checks the result ratio.
        assertThat(new Rational(resultRect.width(), resultRect.height())).isEqualTo(
                targetAspectRatio);
        // Checks the result center.
        assertThat(new Point(resultRect.centerX(), resultRect.centerY())).isEqualTo(
                new Point(WIDTH / 2, HEIGHT / 2));
        // Checks the result width/height.
        if (targetAspectRatio.floatValue() >= new Rational(WIDTH, HEIGHT).floatValue()) {
            assertEquals(WIDTH, resultRect.width());
        } else {
            assertEquals(HEIGHT, resultRect.height());
        }
    }

    @Test
    public void computeCropRectFromDispatchInfo_dispatchBufferRotated90() {
        assertComputeCropRectFromDispatchInfo(90, new Size(4, 6), new Rect(3, 0, 4, 1));
    }

    @Test
    public void computeCropRectFromDispatchInfo_dispatchBufferRotated180() {
        assertComputeCropRectFromDispatchInfo(180, new Size(6, 4), new Rect(5, 3, 6, 4));
    }

    @Test
    public void computeCropRectFromDispatchInfo_dispatchBufferRotated270() {
        assertComputeCropRectFromDispatchInfo(270, new Size(4, 6), new Rect(0, 5, 1, 6));
    }

    @Test
    public void computeCropRectFromDispatchInfo_dispatchBufferRotated0() {
        assertComputeCropRectFromDispatchInfo(0, new Size(6, 4), new Rect(0, 0, 1, 1));
    }

    private void assertComputeCropRectFromDispatchInfo(int outputDegrees, Size dispatchResolution,
            Rect dispatchRect) {
        // Arrange:
        // Surface crop rect stays the same regardless of HAL rotations.
        Rect surfaceCropRect = new Rect(0, 0, 1, 1);
        // Exif degrees being 0 means HAL consumed the target rotation.
        int exifRotationDegrees = 0;

        // Act.
        Rect dispatchCropRect = ImageUtil.computeCropRectFromDispatchInfo(
                surfaceCropRect, outputDegrees, dispatchResolution, exifRotationDegrees);

        // Assert.
        assertThat(dispatchCropRect).isEqualTo(dispatchRect);
    }
}
