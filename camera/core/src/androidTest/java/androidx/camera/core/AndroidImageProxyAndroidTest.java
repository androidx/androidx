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

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

@RunWith(AndroidJUnit4.class)
public final class AndroidImageProxyAndroidTest {
    private static final long INITIAL_TIMESTAMP = 138990020L;

    private final Image image = mock(Image.class);
    private final Image.Plane yPlane = mock(Image.Plane.class);
    private final Image.Plane uPlane = mock(Image.Plane.class);
    private final Image.Plane vPlane = mock(Image.Plane.class);
    private ImageProxy imageProxy;

    @Before
    public void setUp() {
        when(image.getPlanes()).thenReturn(new Image.Plane[]{yPlane, uPlane, vPlane});
        when(yPlane.getRowStride()).thenReturn(640);
        when(yPlane.getPixelStride()).thenReturn(1);
        when(yPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(640 * 480));
        when(uPlane.getRowStride()).thenReturn(320);
        when(uPlane.getPixelStride()).thenReturn(1);
        when(uPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(320 * 240));
        when(vPlane.getRowStride()).thenReturn(320);
        when(vPlane.getPixelStride()).thenReturn(1);
        when(vPlane.getBuffer()).thenReturn(ByteBuffer.allocateDirect(320 * 240));

        when(image.getTimestamp()).thenReturn(INITIAL_TIMESTAMP);
        imageProxy = new AndroidImageProxy(image);
    }

    @Test
    public void close_closesWrappedImage() {
        imageProxy.close();

        verify(image).close();
    }

    @Test
    public void getCropRect_returnsCropRectForWrappedImage() {
        when(image.getCropRect()).thenReturn(new Rect(0, 0, 20, 20));

        assertThat(imageProxy.getCropRect()).isEqualTo(new Rect(0, 0, 20, 20));
    }

    @Test
    public void setCropRect_setsCropRectForWrappedImage() {
        imageProxy.setCropRect(new Rect(0, 0, 40, 40));

        verify(image).setCropRect(new Rect(0, 0, 40, 40));
    }

    @Test
    public void getFormat_returnsFormatForWrappedImage() {
        when(image.getFormat()).thenReturn(ImageFormat.YUV_420_888);

        assertThat(imageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
    }

    @Test
    public void getHeight_returnsHeightForWrappedImage() {
        when(image.getHeight()).thenReturn(480);

        assertThat(imageProxy.getHeight()).isEqualTo(480);
    }

    @Test
    public void getWidth_returnsWidthForWrappedImage() {
        when(image.getWidth()).thenReturn(640);

        assertThat(imageProxy.getWidth()).isEqualTo(640);
    }

    @Test
    public void getTimestamp_returnsTimestampForWrappedImage() {
        assertThat(imageProxy.getTimestamp()).isEqualTo(INITIAL_TIMESTAMP);
    }

    public void setTimestamp_setsTimestampForWrappedImage() {
        imageProxy.setTimestamp(INITIAL_TIMESTAMP + 10);

        assertThat(imageProxy.getTimestamp()).isEqualTo(INITIAL_TIMESTAMP + 10);
    }

    @Test
    public void getPlanes_returnsPlanesForWrappedImage() {
        ImageProxy.PlaneProxy[] wrappedPlanes = imageProxy.getPlanes();

        Image.Plane[] originalPlanes = new Image.Plane[]{yPlane, uPlane, vPlane};
        assertThat(wrappedPlanes.length).isEqualTo(3);
        for (int i = 0; i < 3; ++i) {
            assertThat(wrappedPlanes[i].getRowStride()).isEqualTo(originalPlanes[i].getRowStride());
            assertThat(wrappedPlanes[i].getPixelStride())
                    .isEqualTo(originalPlanes[i].getPixelStride());
            assertThat(wrappedPlanes[i].getBuffer()).isEqualTo(originalPlanes[i].getBuffer());
        }
    }
}
