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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@MediumTest
@RunWith(AndroidJUnit4.class)
public final class AndroidImageReaderProxyTest {
    private final ImageReader mImageReader = mock(ImageReader.class);
    private ImageReaderProxy mImageReaderProxy;

    @Before
    public void setUp() {
        mImageReaderProxy = new AndroidImageReaderProxy(mImageReader);
        when(mImageReader.acquireLatestImage()).thenReturn(mock(Image.class));
        when(mImageReader.acquireNextImage()).thenReturn(mock(Image.class));
    }

    @Test
    public void acquireLatestImage_invokesMethodOnWrappedReader() {
        mImageReaderProxy.acquireLatestImage();

        verify(mImageReader, times(1)).acquireLatestImage();
    }

    @Test
    public void acquireNextImage_invokesMethodOnWrappedReader() {
        mImageReaderProxy.acquireNextImage();

        verify(mImageReader, times(1)).acquireNextImage();
    }

    @Test
    public void close_invokesMethodOnWrappedReader() {
        mImageReaderProxy.close();

        verify(mImageReader, times(1)).close();
    }

    @Test
    public void getWidth_returnsWidthOfWrappedReader() {
        when(mImageReader.getWidth()).thenReturn(640);

        assertThat(mImageReaderProxy.getWidth()).isEqualTo(640);
    }

    @Test
    public void getHeight_returnsHeightOfWrappedReader() {
        when(mImageReader.getHeight()).thenReturn(480);

        assertThat(mImageReaderProxy.getHeight()).isEqualTo(480);
    }

    @Test
    public void getImageFormat_returnsImageFormatOfWrappedReader() {
        when(mImageReader.getImageFormat()).thenReturn(ImageFormat.YUV_420_888);

        assertThat(mImageReaderProxy.getImageFormat()).isEqualTo(ImageFormat.YUV_420_888);
    }

    @Test
    public void getMaxImages_returnsMaxImagesOfWrappedReader() {
        when(mImageReader.getMaxImages()).thenReturn(8);

        assertThat(mImageReaderProxy.getMaxImages()).isEqualTo(8);
    }

    @Test
    public void getSurface_returnsSurfaceOfWrappedReader() {
        Surface surface = mock(Surface.class);
        when(mImageReader.getSurface()).thenReturn(surface);

        assertThat(mImageReaderProxy.getSurface()).isSameInstanceAs(surface);
    }

    @Test
    public void setOnImageAvailableListener_setsListenerOfWrappedReader() {
        ImageReaderProxy.OnImageAvailableListener listener =
                mock(ImageReaderProxy.OnImageAvailableListener.class);

        mImageReaderProxy.setOnImageAvailableListener(listener,
                CameraXExecutors.directExecutor());

        ArgumentCaptor<ImageReader.OnImageAvailableListener> transformedListenerCaptor =
                ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener.class);
        ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mImageReader, times(1))
                .setOnImageAvailableListener(
                        transformedListenerCaptor.capture(), handlerCaptor.capture());

        transformedListenerCaptor.getValue().onImageAvailable(mImageReader);
        verify(listener, times(1)).onImageAvailable(mImageReaderProxy);
    }

    @Test
    public void returnNullWhenImageReaderIsClosed() {
        ImageReader imageReader =
                ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);
        ImageReaderProxy imageReaderProxy = new AndroidImageReaderProxy(imageReader);

        imageReaderProxy.close();

        assertThat(imageReaderProxy.acquireLatestImage()).isNull();
        assertThat(imageReaderProxy.acquireNextImage()).isNull();
    }
}
