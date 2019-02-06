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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.runner.AndroidJUnit4;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReferenceCountedImageProxyAndroidTest {
  private static final int WIDTH = 640;
  private static final int HEIGHT = 480;

  // Assume the image has YUV_420_888 format.
  private final ImageProxy image = mock(ImageProxy.class);
  private final ImageProxy.PlaneProxy yPlane = mock(ImageProxy.PlaneProxy.class);
  private final ImageProxy.PlaneProxy uPlane = mock(ImageProxy.PlaneProxy.class);
  private final ImageProxy.PlaneProxy vPlane = mock(ImageProxy.PlaneProxy.class);
  private final ByteBuffer yBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
  private final ByteBuffer uBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
  private final ByteBuffer vBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
  private ReferenceCountedImageProxy imageProxy;

  @Before
  public void setUp() {
    when(image.getWidth()).thenReturn(WIDTH);
    when(image.getHeight()).thenReturn(HEIGHT);
    when(yPlane.getBuffer()).thenReturn(yBuffer);
    when(uPlane.getBuffer()).thenReturn(uBuffer);
    when(vPlane.getBuffer()).thenReturn(vBuffer);
    when(image.getPlanes()).thenReturn(new ImageProxy.PlaneProxy[] {yPlane, uPlane, vPlane});
    imageProxy = new ReferenceCountedImageProxy(image);
  }

  @Test
  public void getReferenceCount_returnsOne_afterConstruction() {
    assertThat(imageProxy.getReferenceCount()).isEqualTo(1);
  }

  @Test
  public void fork_incrementsReferenceCount() {
    imageProxy.fork();
    imageProxy.fork();

    assertThat(imageProxy.getReferenceCount()).isEqualTo(3);
  }

  @Test
  public void close_decrementsReferenceCount() {
    ImageProxy forkedImage0 = imageProxy.fork();
    ImageProxy forkedImage1 = imageProxy.fork();

    forkedImage0.close();
    forkedImage1.close();

    assertThat(imageProxy.getReferenceCount()).isEqualTo(1);
    verify(image, never()).close();
  }

  @Test
  public void close_closesBaseImage_whenReferenceCountHitsZero() {
    ImageProxy forkedImage0 = imageProxy.fork();
    ImageProxy forkedImage1 = imageProxy.fork();

    forkedImage0.close();
    forkedImage1.close();
    imageProxy.close();

    assertThat(imageProxy.getReferenceCount()).isEqualTo(0);
    verify(image, times(1)).close();
  }

  @Test
  public void close_decrementsReferenceCountOnlyOnce() {
    ImageProxy forkedImage = imageProxy.fork();

    forkedImage.close();
    forkedImage.close();

    assertThat(imageProxy.getReferenceCount()).isEqualTo(1);
  }

  @Test
  public void fork_returnsNull_whenBaseImageIsClosed() {
    imageProxy.close();

    ImageProxy forkedImage = imageProxy.fork();

    assertThat(forkedImage).isNull();
  }

  @Test
  public void concurrentAccessForTwoForkedImagesOnTwoThreads() throws InterruptedException {
    final ImageProxy forkedImage0 = imageProxy.fork();
    final ImageProxy forkedImage1 = imageProxy.fork();

    Thread thread0 =
        new Thread() {
          @Override
          public void run() {
            forkedImage0.getWidth();
            forkedImage0.getHeight();
            ImageProxy.PlaneProxy[] planes = forkedImage0.getPlanes();
            for (ImageProxy.PlaneProxy plane : planes) {
              ByteBuffer buffer = plane.getBuffer();
              for (int i = 0; i < buffer.capacity(); ++i) {
                buffer.get(i);
              }
            }
          }
        };
    Thread thread1 =
        new Thread() {
          @Override
          public void run() {
            forkedImage1.getWidth();
            forkedImage1.getHeight();
            ImageProxy.PlaneProxy[] planes = forkedImage1.getPlanes();
            for (ImageProxy.PlaneProxy plane : planes) {
              ByteBuffer buffer = plane.getBuffer();
              for (int i = 0; i < buffer.capacity(); ++i) {
                buffer.get(i);
              }
            }
          }
        };

    thread0.start();
    thread1.start();
    thread0.join();
    thread1.join();
  }
}
