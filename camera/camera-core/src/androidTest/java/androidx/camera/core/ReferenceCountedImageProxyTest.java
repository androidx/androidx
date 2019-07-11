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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ReferenceCountedImageProxyTest {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    // Assume the image has YUV_420_888 format.
    private final ImageProxy mImage = mock(ImageProxy.class);
    private final ImageProxy.PlaneProxy mYPlane = mock(ImageProxy.PlaneProxy.class);
    private final ImageProxy.PlaneProxy mUPlane = mock(ImageProxy.PlaneProxy.class);
    private final ImageProxy.PlaneProxy mVPlane = mock(ImageProxy.PlaneProxy.class);
    private final ByteBuffer mYBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT);
    private final ByteBuffer mUBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
    private final ByteBuffer mVBuffer = ByteBuffer.allocateDirect(WIDTH * HEIGHT / 4);
    private ReferenceCountedImageProxy mImageProxy;

    @Before
    public void setUp() {
        when(mImage.getWidth()).thenReturn(WIDTH);
        when(mImage.getHeight()).thenReturn(HEIGHT);
        when(mYPlane.getBuffer()).thenReturn(mYBuffer);
        when(mUPlane.getBuffer()).thenReturn(mUBuffer);
        when(mVPlane.getBuffer()).thenReturn(mVBuffer);
        when(mImage.getPlanes()).thenReturn(new ImageProxy.PlaneProxy[]{mYPlane, mUPlane, mVPlane});
        mImageProxy = new ReferenceCountedImageProxy(mImage);
    }

    @Test
    public void getReferenceCount_returnsOne_afterConstruction() {
        assertThat(mImageProxy.getReferenceCount()).isEqualTo(1);
    }

    @Test
    public void fork_incrementsReferenceCount() {
        mImageProxy.fork();
        mImageProxy.fork();

        assertThat(mImageProxy.getReferenceCount()).isEqualTo(3);
    }

    @Test
    public void close_decrementsReferenceCount() {
        ImageProxy forkedImage0 = mImageProxy.fork();
        ImageProxy forkedImage1 = mImageProxy.fork();

        forkedImage0.close();
        forkedImage1.close();

        assertThat(mImageProxy.getReferenceCount()).isEqualTo(1);
        verify(mImage, never()).close();
    }

    @Test
    public void close_closesBaseImage_whenReferenceCountHitsZero() {
        ImageProxy forkedImage0 = mImageProxy.fork();
        ImageProxy forkedImage1 = mImageProxy.fork();

        forkedImage0.close();
        forkedImage1.close();
        mImageProxy.close();

        assertThat(mImageProxy.getReferenceCount()).isEqualTo(0);
        verify(mImage, times(1)).close();
    }

    @Test
    public void close_decrementsReferenceCountOnlyOnce() {
        ImageProxy forkedImage = mImageProxy.fork();

        forkedImage.close();
        forkedImage.close();

        assertThat(mImageProxy.getReferenceCount()).isEqualTo(1);
    }

    @Test
    public void fork_returnsNull_whenBaseImageIsClosed() {
        mImageProxy.close();

        ImageProxy forkedImage = mImageProxy.fork();

        assertThat(forkedImage).isNull();
    }

    @Test
    public void concurrentAccessForTwoForkedImagesOnTwoThreads() throws InterruptedException {
        final ImageProxy forkedImage0 = mImageProxy.fork();
        final ImageProxy forkedImage1 = mImageProxy.fork();

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
