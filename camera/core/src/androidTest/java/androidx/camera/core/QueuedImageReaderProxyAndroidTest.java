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

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;
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
public final class QueuedImageReaderProxyAndroidTest {
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 480;
    private static final int IMAGE_FORMAT = ImageFormat.YUV_420_888;
    private static final int MAX_IMAGES = 10;

    private final Surface surface = mock(Surface.class);
    private HandlerThread handlerThread;
    private Handler handler;
    private QueuedImageReaderProxy imageReaderProxy;

    private static ImageProxy createMockImageProxy() {
        ImageProxy image = mock(ImageProxy.class);
        when(image.getWidth()).thenReturn(IMAGE_WIDTH);
        when(image.getHeight()).thenReturn(IMAGE_HEIGHT);
        when(image.getFormat()).thenReturn(IMAGE_FORMAT);
        return image;
    }

    private static ConcreteImageProxy createSemaphoreReleasingOnCloseImageProxy(
            Semaphore semaphore) {
        ConcreteImageProxy image = createForwardingImageProxy();
        image.addOnImageCloseListener(
                closedImage -> {
                    semaphore.release();
                });
        return image;
    }

    private static ConcreteImageProxy createForwardingImageProxy() {
        return new ConcreteImageProxy(createMockImageProxy());
    }

    @Before
    public void setUp() {
        handlerThread = new HandlerThread("background");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        imageReaderProxy =
                new QueuedImageReaderProxy(
                        IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_FORMAT, MAX_IMAGES, surface);
    }

    @After
    public void tearDown() {
        handlerThread.quitSafely();
    }

    @Test
    public void enqueueImage_incrementsQueueSize() {
        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.enqueueImage(createForwardingImageProxy());

        assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(2);
    }

    @Test
    public void enqueueImage_doesNotIncreaseSizeBeyondMaxImages() {
        // Exceed the queue's capacity by 2.
        for (int i = 0; i < MAX_IMAGES + 2; ++i) {
            imageReaderProxy.enqueueImage(createForwardingImageProxy());
        }

        assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(MAX_IMAGES);
    }

    @Test
    public void enqueueImage_closesImagesWhichAreNotEnqueued_doesNotCloseOtherImages() {
        // Exceed the queue's capacity by 2.
        List<ConcreteImageProxy> images = new ArrayList<>(MAX_IMAGES + 2);
        for (int i = 0; i < MAX_IMAGES + 2; ++i) {
            images.add(createForwardingImageProxy());
            imageReaderProxy.enqueueImage(images.get(i));
        }

        // Last two images should not be enqueued and should be closed.
        assertThat(images.get(MAX_IMAGES).isClosed()).isTrue();
        assertThat(images.get(MAX_IMAGES + 1).isClosed()).isTrue();
        // All other images should be enqueued and open.
        for (int i = 0; i < MAX_IMAGES; ++i) {
            assertThat(images.get(i).isClosed()).isFalse();
        }
    }

    @Test(timeout = 2000)
    public void closedImages_reduceQueueSize() throws InterruptedException {
        // Fill up to the queue's capacity.
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        for (int i = 0; i < MAX_IMAGES; ++i) {
            ForwardingImageProxy image =
                    createSemaphoreReleasingOnCloseImageProxy(onCloseSemaphore);
            imageReaderProxy.enqueueImage(image);
        }

        imageReaderProxy.acquireNextImage().close();
        imageReaderProxy.acquireNextImage().close();
        onCloseSemaphore.acquire(/*permits=*/ 2);

        assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(MAX_IMAGES - 2);
    }

    @Test(timeout = 2000)
    public void closedImage_allowsNewImageToBeEnqueued() throws InterruptedException {
        // Fill up to the queue's capacity.
        Semaphore onCloseSemaphore = new Semaphore(/*permits=*/ 0);
        for (int i = 0; i < MAX_IMAGES; ++i) {
            ForwardingImageProxy image =
                    createSemaphoreReleasingOnCloseImageProxy(onCloseSemaphore);
            imageReaderProxy.enqueueImage(image);
        }

        imageReaderProxy.acquireNextImage().close();
        onCloseSemaphore.acquire();

        ConcreteImageProxy lastImageProxy = createForwardingImageProxy();
        imageReaderProxy.enqueueImage(lastImageProxy);

        // Last image should be enqueued and open.
        assertThat(lastImageProxy.isClosed()).isFalse();
    }

    @Test
    public void enqueueImage_invokesListenerCallback() {
        ImageReaderProxy.OnImageAvailableListener listener =
                mock(ImageReaderProxy.OnImageAvailableListener.class);
        imageReaderProxy.setOnImageAvailableListener(listener, handler);

        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.enqueueImage(createForwardingImageProxy());

        verify(listener, timeout(2000).times(2)).onImageAvailable(imageReaderProxy);
    }

    @Test
    public void acquireLatestImage_returnsNull_whenQueueIsEmpty() {
        assertThat(imageReaderProxy.acquireLatestImage()).isNull();
    }

    @Test
    public void acquireLatestImage_returnsLastImage_reducesQueueSizeToOne() {
        final int availableImages = 5;
        List<ForwardingImageProxy> images = new ArrayList<>(availableImages);
        for (int i = 0; i < availableImages; ++i) {
            images.add(createForwardingImageProxy());
            imageReaderProxy.enqueueImage(images.get(i));
        }

        ImageProxy lastImage = images.get(availableImages - 1);
        assertThat(imageReaderProxy.acquireLatestImage()).isEqualTo(lastImage);
        assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(1);
    }

    @Test
    public void acquireLatestImage_throwsException_whenAllImagesWerePreviouslyAcquired() {
        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.acquireNextImage();

        assertThrows(IllegalStateException.class, () -> imageReaderProxy.acquireLatestImage());
    }

    @Test
    public void acquireNextImage_returnsNull_whenQueueIsEmpty() {
        assertThat(imageReaderProxy.acquireNextImage()).isNull();
    }

    @Test
    public void acquireNextImage_returnsNextImage_doesNotChangeQueueSize() {
        final int availableImages = 5;
        List<ForwardingImageProxy> images = new ArrayList<>(availableImages);
        for (int i = 0; i < availableImages; ++i) {
            images.add(createForwardingImageProxy());
            imageReaderProxy.enqueueImage(images.get(i));
        }

        for (int i = 0; i < availableImages; ++i) {
            assertThat(imageReaderProxy.acquireNextImage()).isEqualTo(images.get(i));
        }
        assertThat(imageReaderProxy.getCurrentImages()).isEqualTo(availableImages);
    }

    @Test
    public void acquireNextImage_throwsException_whenAllImagesWerePreviouslyAcquired() {
        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.acquireNextImage();

        assertThrows(IllegalStateException.class, () -> imageReaderProxy.acquireNextImage());
    }

    @Test
    public void close_closesAnyImagesStillInQueue() {
        ConcreteImageProxy image0 = createForwardingImageProxy();
        ConcreteImageProxy image1 = createForwardingImageProxy();
        imageReaderProxy.enqueueImage(image0);
        imageReaderProxy.enqueueImage(image1);

        imageReaderProxy.close();

        assertThat(image0.isClosed()).isTrue();
        assertThat(image1.isClosed()).isTrue();
    }

    @Test
    public void close_notifiesOnCloseListeners() {
        QueuedImageReaderProxy.OnReaderCloseListener listenerA =
                mock(QueuedImageReaderProxy.OnReaderCloseListener.class);
        QueuedImageReaderProxy.OnReaderCloseListener listenerB =
                mock(QueuedImageReaderProxy.OnReaderCloseListener.class);
        imageReaderProxy.addOnReaderCloseListener(listenerA);
        imageReaderProxy.addOnReaderCloseListener(listenerB);

        imageReaderProxy.close();

        verify(listenerA, times(1)).onReaderClose(imageReaderProxy);
        verify(listenerB, times(1)).onReaderClose(imageReaderProxy);
    }

    @Test
    public void acquireLatestImage_throwsException_afterReaderIsClosed() {
        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.close();

        assertThrows(IllegalStateException.class, () -> imageReaderProxy.acquireLatestImage());
    }

    @Test
    public void acquireNextImage_throwsException_afterReaderIsClosed() {
        imageReaderProxy.enqueueImage(createForwardingImageProxy());
        imageReaderProxy.close();

        assertThrows(IllegalStateException.class, () -> imageReaderProxy.acquireNextImage());
    }

    @Test
    public void getHeight_returnsFixedHeight() {
        assertThat(imageReaderProxy.getHeight()).isEqualTo(IMAGE_HEIGHT);
    }

    @Test
    public void getWidth_returnsFixedWidth() {
        assertThat(imageReaderProxy.getWidth()).isEqualTo(IMAGE_WIDTH);
    }

    @Test
    public void getImageFormat_returnsFixedFormat() {
        assertThat(imageReaderProxy.getImageFormat()).isEqualTo(IMAGE_FORMAT);
    }

    @Test
    public void getMaxImages_returnsFixedCapacity() {
        assertThat(imageReaderProxy.getMaxImages()).isEqualTo(MAX_IMAGES);
    }

    private static final class ConcreteImageProxy extends ForwardingImageProxy {
        private boolean isClosed = false;

        ConcreteImageProxy(ImageProxy image) {
            super(image);
        }

        @Override
        public synchronized void close() {
            super.close();
            isClosed = true;
        }

        public synchronized boolean isClosed() {
            return isClosed;
        }
    }
}
