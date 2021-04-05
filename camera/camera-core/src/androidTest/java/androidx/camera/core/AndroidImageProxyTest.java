/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.OptIn;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * Unit tests for {@link AndroidImageProxy}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class AndroidImageProxyTest {
    private static final long INITIAL_TIMESTAMP = 138990020L;

    private final Image mImage = mock(Image.class);
    private final Image.Plane mYPlane = mock(Image.Plane.class);
    private final Image.Plane mUPlane = mock(Image.Plane.class);
    private final Image.Plane mVPlane = mock(Image.Plane.class);
    private ImageProxy mImageProxy;

    @Before
    public void setUp() {
        when(mImage.getPlanes()).thenReturn(new Image.Plane[]{mYPlane, mUPlane, mVPlane});
        when(mYPlane.getRowStride()).thenReturn(640);
        when(mYPlane.getPixelStride()).thenReturn(1);
        when(mYPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(640 * 480));
        when(mUPlane.getRowStride()).thenReturn(320);
        when(mUPlane.getPixelStride()).thenReturn(1);
        when(mUPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(320 * 240));
        when(mVPlane.getRowStride()).thenReturn(320);
        when(mVPlane.getPixelStride()).thenReturn(1);
        when(mVPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(320 * 240));

        when(mImage.getTimestamp()).thenReturn(INITIAL_TIMESTAMP);
        mImageProxy = new AndroidImageProxy(mImage);
    }

    @Test
    public void close_closesWrappedImage() {
        mImageProxy.close();

        verify(mImage).close();
    }

    @Test
    public void getCropRect_returnsCropRectForWrappedImage() {
        when(mImage.getCropRect()).thenReturn(new Rect(0, 0, 20, 20));

        assertThat(mImageProxy.getCropRect()).isEqualTo(new Rect(0, 0, 20, 20));
    }

    @Test
    public void setCropRect_setsCropRectForWrappedImage() {
        mImageProxy.setCropRect(new Rect(0, 0, 40, 40));

        verify(mImage).setCropRect(new Rect(0, 0, 40, 40));
    }

    @Test
    public void getFormat_returnsFormatForWrappedImage() {
        when(mImage.getFormat()).thenReturn(ImageFormat.YUV_420_888);

        assertThat(mImageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
    }

    @Test
    public void getHeight_returnsHeightForWrappedImage() {
        when(mImage.getHeight()).thenReturn(480);

        assertThat(mImageProxy.getHeight()).isEqualTo(480);
    }

    @Test
    public void getWidth_returnsWidthForWrappedImage() {
        when(mImage.getWidth()).thenReturn(640);

        assertThat(mImageProxy.getWidth()).isEqualTo(640);
    }

    @Test
    public void getTimestamp_returnsTimestampForWrappedImage() {
        assertThat(mImageProxy.getImageInfo().getTimestamp()).isEqualTo(INITIAL_TIMESTAMP);
    }

    @Test
    public void getPlanes_returnsPlanesForWrappedImage() {
        ImageProxy.PlaneProxy[] wrappedPlanes = mImageProxy.getPlanes();

        Image.Plane[] originalPlanes = new Image.Plane[]{mYPlane, mUPlane, mVPlane};
        assertThat(wrappedPlanes.length).isEqualTo(3);
        for (int i = 0; i < 3; ++i) {
            assertThat(wrappedPlanes[i].getRowStride()).isEqualTo(originalPlanes[i].getRowStride());
            assertThat(wrappedPlanes[i].getPixelStride())
                    .isEqualTo(originalPlanes[i].getPixelStride());
            assertThat(wrappedPlanes[i].getBuffer()).isEqualTo(originalPlanes[i].getBuffer());
        }
    }

    @Test
    @OptIn(markerClass = ExperimentalGetImage.class)
    public void getImage_returnsWrappedImage() {
        assertThat(mImageProxy.getImage()).isEqualTo(mImage);
    }
}
