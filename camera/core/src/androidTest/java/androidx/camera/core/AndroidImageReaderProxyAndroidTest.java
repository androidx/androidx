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
import androidx.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public final class AndroidImageReaderProxyAndroidTest {
  private final ImageReader imageReader = mock(ImageReader.class);
  private ImageReaderProxy imageReaderProxy;

  @Before
  public void setUp() {
    imageReaderProxy = new AndroidImageReaderProxy(imageReader);
    when(imageReader.acquireLatestImage()).thenReturn(mock(Image.class));
    when(imageReader.acquireNextImage()).thenReturn(mock(Image.class));
  }

  @Test
  public void acquireLatestImage_invokesMethodOnWrappedReader() {
    imageReaderProxy.acquireLatestImage();

    verify(imageReader, times(1)).acquireLatestImage();
  }

  @Test
  public void acquireNextImage_invokesMethodOnWrappedReader() {
    imageReaderProxy.acquireNextImage();

    verify(imageReader, times(1)).acquireNextImage();
  }

  @Test
  public void close_invokesMethodOnWrappedReader() {
    imageReaderProxy.close();

    verify(imageReader, times(1)).close();
  }

  @Test
  public void getWidth_returnsWidthOfWrappedReader() {
    when(imageReader.getWidth()).thenReturn(640);

    assertThat(imageReaderProxy.getWidth()).isEqualTo(640);
  }

  @Test
  public void getHeight_returnsHeightOfWrappedReader() {
    when(imageReader.getHeight()).thenReturn(480);

    assertThat(imageReaderProxy.getHeight()).isEqualTo(480);
  }

  @Test
  public void getImageFormat_returnsImageFormatOfWrappedReader() {
    when(imageReader.getImageFormat()).thenReturn(ImageFormat.YUV_420_888);

    assertThat(imageReaderProxy.getImageFormat()).isEqualTo(ImageFormat.YUV_420_888);
  }

  @Test
  public void getMaxImages_returnsMaxImagesOfWrappedReader() {
    when(imageReader.getMaxImages()).thenReturn(8);

    assertThat(imageReaderProxy.getMaxImages()).isEqualTo(8);
  }

  @Test
  public void getSurface_returnsSurfaceOfWrappedReader() {
    Surface surface = mock(Surface.class);
    when(imageReader.getSurface()).thenReturn(surface);

    assertThat(imageReaderProxy.getSurface()).isSameAs(surface);
  }

  @Test
  public void setOnImageAvailableListener_setsListenerOfWrappedReader() {
    ImageReaderProxy.OnImageAvailableListener listener =
        mock(ImageReaderProxy.OnImageAvailableListener.class);

    imageReaderProxy.setOnImageAvailableListener(listener, /*handler=*/ null);

    ArgumentCaptor<ImageReader.OnImageAvailableListener> transformedListenerCaptor =
        ArgumentCaptor.forClass(ImageReader.OnImageAvailableListener.class);
    ArgumentCaptor<Handler> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
    verify(imageReader, times(1))
        .setOnImageAvailableListener(transformedListenerCaptor.capture(), handlerCaptor.capture());

    transformedListenerCaptor.getValue().onImageAvailable(imageReader);
    verify(listener, times(1)).onImageAvailable(imageReaderProxy);
  }
}
