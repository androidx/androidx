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

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@RunWith(AndroidJUnit4.class)
public final class ForwardingImageReaderListenerAndroidTest {
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 480;
    private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private static final int MAX_IMAGES = 10;

    private final ImageReader imageReader = mock(ImageReader.class);
    private final Surface surface = mock(Surface.class);
    private HandlerThread handlerThread;
    private Handler handler;
    private List<QueuedImageReaderProxy> imageReaderProxys;
    private ForwardingImageReaderListener forwardingListener;

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
    createSemaphoreReleasingClosingListener(Semaphore semaphore) {
        return imageReaderProxy -> {
            imageReaderProxy.acquireNextImage().close();
            semaphore.release();
        };
    }

    @Before
    public void setUp() {
        handlerThread = new HandlerThread("listener");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        imageReaderProxys = new ArrayList<>(3);
        for (int i = 0; i < 3; ++i) {
            imageReaderProxys.add(
                    new QueuedImageReaderProxy(
                            IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, MAX_IMAGES, surface));
        }
        forwardingListener = new ForwardingImageReaderListener(imageReaderProxys);
    }

    @After
    public void tearDown() {
        handlerThread.quitSafely();
    }

    @Test
    public void newImageIsForwardedToAllListeners() {
        Image baseImage = createMockImage();
        when(imageReader.acquireNextImage()).thenReturn(baseImage);
        List<ImageReaderProxy.OnImageAvailableListener> listeners = new ArrayList<>();
        for (ImageReaderProxy imageReaderProxy : imageReaderProxys) {
            ImageReaderProxy.OnImageAvailableListener listener = createMockListener();
            imageReaderProxy.setOnImageAvailableListener(listener, handler);
            listeners.add(listener);
        }

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            forwardingListener.onImageAvailable(imageReader);
        }

        for (int i = 0; i < imageReaderProxys.size(); ++i) {
            // Listener should be notified about every available image.
            verify(listeners.get(i), timeout(2000).times(availableImages))
                    .onImageAvailable(imageReaderProxys.get(i));
        }
    }

    @Test(timeout = 2000)
    public void baseImageIsClosed_allQueuesAreCleared_whenAllForwardedCopiesAreClosed()
            throws InterruptedException {
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        Image baseImage = createMockImage();
        when(imageReader.acquireNextImage()).thenReturn(baseImage);
        for (ImageReaderProxy imageReaderProxy : imageReaderProxys) {
            // Close the image for every listener.
            imageReaderProxy.setOnImageAvailableListener(
                    createSemaphoreReleasingClosingListener(onCloseSemaphore), handler);
        }

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            forwardingListener.onImageAvailable(imageReader);
        }
        onCloseSemaphore.acquire(availableImages * imageReaderProxys.size());

        // Base image should be closed every time.
        verify(baseImage, times(availableImages)).close();
        // All queues should be cleared.
        for (QueuedImageReaderProxy imageReaderProxy : imageReaderProxys) {
            assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(0);
        }
    }

    @Test(timeout = 2000)
    public void baseImageIsNotClosed_someQueuesAreCleared_whenNotAllForwardedCopiesAreClosed()
            throws InterruptedException {
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        Image baseImage = createMockImage();
        when(imageReader.acquireNextImage()).thenReturn(baseImage);
        // Don't close the image for the first listener.
        imageReaderProxys.get(0).setOnImageAvailableListener(createMockListener(), handler);
        // Close the image for the other listeners.
        imageReaderProxys
                .get(1)
                .setOnImageAvailableListener(
                        createSemaphoreReleasingClosingListener(onCloseSemaphore), handler);
        imageReaderProxys
                .get(2)
                .setOnImageAvailableListener(
                        createSemaphoreReleasingClosingListener(onCloseSemaphore), handler);

        final int availableImages = 5;
        for (int i = 0; i < availableImages; ++i) {
            forwardingListener.onImageAvailable(imageReader);
        }
        onCloseSemaphore.acquire(availableImages * (imageReaderProxys.size() - 1));

        // Base image should not be closed every time.
        verify(baseImage, never()).close();
        // First reader's queue should not be cleared.
        assertThat(imageReaderProxys.get(0).getCurrentImages()).isEqualTo(availableImages);
        // Other readers' queues should be cleared.
        assertThat(imageReaderProxys.get(1).getCurrentImages()).isEqualTo(0);
        assertThat(imageReaderProxys.get(2).getCurrentImages()).isEqualTo(0);
    }
}
