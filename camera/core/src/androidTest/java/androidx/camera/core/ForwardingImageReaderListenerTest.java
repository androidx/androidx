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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ForwardingImageReaderListenerTest {
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 480;
    private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private static final int MAX_IMAGES = 10;

    private final ImageReader mImageReader = mock(ImageReader.class);
    private final Surface mSurface = mock(Surface.class);
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private List<QueuedImageReaderProxy> mImageReaderProxies;
    private ForwardingImageReaderListener mForwardingListener;

    private static Image createMockImage() {
        Image image = mock(Image.class);
        when(image.getWidth()).thenReturn(IMAGE_WIDTH);
        when(image.getHeight()).thenReturn(IMAGE_HEIGHT);
        when(image.getFormat()).thenReturn(IMAGE_FORMAT);
        return image;
    }

    private static ImageReaderProxy.OnImageAvailableListener createMockListener() {
        return mock(ImageReaderProxy.OnImageAvailableListener.class);
    }

    /**
     * Returns a listener which immediately acquires the next image, closes the image, and releases
     * a semaphore.
     */
    private static ImageReaderProxy.OnImageAvailableListener
            createSemaphoreReleasingClosingListener(final Semaphore semaphore) {
        return new ImageReaderProxy.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReaderProxy imageReaderProxy) {
                imageReaderProxy.acquireNextImage().close();
                semaphore.release();
            }
        };
    }

    @Before
    public void setUp() {
        mHandlerThread = new HandlerThread("listener");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mImageReaderProxies = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            mImageReaderProxies.add(
                    new QueuedImageReaderProxy(
                            IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, MAX_IMAGES, mSurface));
        }
        mForwardingListener = new ForwardingImageReaderListener(mImageReaderProxies);
    }

    @After
    public void tearDown() {
        mHandlerThread.quitSafely();
    }

    @Test
    public void newImageIsForwardedToAllListeners() {
        Image baseImage = createMockImage();
        when(mImageReader.acquireNextImage()).thenReturn(baseImage);
        List<ImageReaderProxy.OnImageAvailableListener> listeners = new ArrayList<>();
        for (ImageReaderProxy imageReaderProxy : mImageReaderProxies) {
            ImageReaderProxy.OnImageAvailableListener listener = createMockListener();
            imageReaderProxy.setOnImageAvailableListener(listener, mHandler);
            listeners.add(listener);
        }

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            mForwardingListener.onImageAvailable(mImageReader);
        }

        for (int i = 0; i < mImageReaderProxies.size(); ++i) {
            // Listener should be notified about every available image.
            verify(listeners.get(i), timeout(2000).times(availableImages))
                    .onImageAvailable(mImageReaderProxies.get(i));
        }
    }

    @Test
    public void baseImageIsClosed_allQueuesAreCleared_whenAllForwardedCopiesAreClosed()
            throws InterruptedException {
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        Image baseImage = createMockImage();
        when(mImageReader.acquireNextImage()).thenReturn(baseImage);
        for (ImageReaderProxy imageReaderProxy : mImageReaderProxies) {
            // Close the image for every listener.
            imageReaderProxy.setOnImageAvailableListener(
                    createSemaphoreReleasingClosingListener(onCloseSemaphore), mHandler);
        }

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            mForwardingListener.onImageAvailable(mImageReader);
        }
        onCloseSemaphore.acquire(availableImages * mImageReaderProxies.size());

        // Base image should be closed every time.
        verify(baseImage, times(availableImages)).close();
        // All queues should be cleared.
        for (QueuedImageReaderProxy imageReaderProxy : mImageReaderProxies) {
            assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(0);
        }
    }

    @Test
    public void baseImageIsNotClosed_someQueuesAreCleared_whenNotAllForwardedCopiesAreClosed()
            throws InterruptedException {
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        Image baseImage = createMockImage();
        when(mImageReader.acquireNextImage()).thenReturn(baseImage);
        // Don't close the image for the first listener.
        mImageReaderProxies.get(0).setOnImageAvailableListener(createMockListener(), mHandler);
        // Close the image for the other listeners.
        mImageReaderProxies
                .get(1)
                .setOnImageAvailableListener(
                        createSemaphoreReleasingClosingListener(onCloseSemaphore), mHandler);
        mImageReaderProxies
                .get(2)
                .setOnImageAvailableListener(
                        createSemaphoreReleasingClosingListener(onCloseSemaphore), mHandler);

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            mForwardingListener.onImageAvailable(mImageReader);
        }
        onCloseSemaphore.acquire(availableImages * (mImageReaderProxies.size() - 1));

        // Base image should not be closed every time.
        verify(baseImage, never()).close();
        // First reader's queue should not be cleared.
        assertThat(mImageReaderProxies.get(0).getCurrentImages()).isEqualTo(availableImages);
        // Other readers' queues should be cleared.
        assertThat(mImageReaderProxies.get(1).getCurrentImages()).isEqualTo(0);
        assertThat(mImageReaderProxies.get(2).getCurrentImages()).isEqualTo(0);
    }
}
