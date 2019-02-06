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
import androidx.test.runner.AndroidJUnit4;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class ForwardingImageProxyAndroidTest {

  private final ImageProxy baseImageProxy = mock(ImageProxy.class);
  private final ImageProxy.PlaneProxy yPlane = mock(ImageProxy.PlaneProxy.class);
  private final ImageProxy.PlaneProxy uPlane = mock(ImageProxy.PlaneProxy.class);
  private final ImageProxy.PlaneProxy vPlane = mock(ImageProxy.PlaneProxy.class);
  private ForwardingImageProxy imageProxy;

  @Before
  public void setUp() {
    imageProxy = new ConcreteImageProxy(baseImageProxy);
  }

  @Test
  public void close_closesWrappedImage() {
    imageProxy.close();

    verify(baseImageProxy).close();
  }

  @Test(timeout = 2000)
  public void close_notifiesOnImageCloseListener_afterSetOnImageCloseListener()
      throws InterruptedException {
    Semaphore closedImageSemaphore = new Semaphore(/*permits=*/ 0);
    AtomicReference<ImageProxy> closedImage = new AtomicReference<>();
    imageProxy.addOnImageCloseListener(
        image -> {
          closedImage.set(image);
          closedImageSemaphore.release();
        });

    imageProxy.close();

    closedImageSemaphore.acquire();
    assertThat(closedImage.get()).isSameAs(imageProxy);
  }

  @Test
  public void getCropRect_returnsCropRectForWrappedImage() {
    when(baseImageProxy.getCropRect()).thenReturn(new Rect(0, 0, 20, 20));

    assertThat(imageProxy.getCropRect()).isEqualTo(new Rect(0, 0, 20, 20));
  }

  @Test
  public void setCropRect_setsCropRectForWrappedImage() {
    imageProxy.setCropRect(new Rect(0, 0, 40, 40));

    verify(baseImageProxy).setCropRect(new Rect(0, 0, 40, 40));
  }

  @Test
  public void getFormat_returnsFormatForWrappedImage() {
    when(baseImageProxy.getFormat()).thenReturn(ImageFormat.YUV_420_888);

    assertThat(imageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
  }

  @Test
  public void getHeight_returnsHeightForWrappedImage() {
    when(baseImageProxy.getHeight()).thenReturn(480);

    assertThat(imageProxy.getHeight()).isEqualTo(480);
  }

  @Test
  public void getWidth_returnsWidthForWrappedImage() {
    when(baseImageProxy.getWidth()).thenReturn(640);

    assertThat(imageProxy.getWidth()).isEqualTo(640);
  }

  @Test
  public void getTimestamp_returnsTimestampForWrappedImage() {
    when(baseImageProxy.getTimestamp()).thenReturn(138990020L);

    assertThat(imageProxy.getTimestamp()).isEqualTo(138990020L);
  }

  @Test
  public void setTimestamp_setsTimestampForWrappedImage() {
    imageProxy.setTimestamp(138990020L);

    verify(baseImageProxy).setTimestamp(138990020L);
  }

  @Test
  public void getPlanes_returnsPlanesForWrappedImage() {
    when(baseImageProxy.getPlanes())
        .thenReturn(new ImageProxy.PlaneProxy[] {yPlane, uPlane, vPlane});

    ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
    assertThat(planes.length).isEqualTo(3);
    assertThat(planes[0]).isEqualTo(yPlane);
    assertThat(planes[1]).isEqualTo(uPlane);
    assertThat(planes[2]).isEqualTo(vPlane);
  }

  private static final class ConcreteImageProxy extends ForwardingImageProxy {
    private ConcreteImageProxy(ImageProxy baseImageProxy) {
      super(baseImageProxy);
    }
  }
}
