/*
 * Copyright 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageWriter;

import androidx.camera.core.impl.ImageReaderProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Instrument test for {@link ImageReaderProxy}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public final class ImageReaderProxysTest {

    private ExecutorService mConsumerExecutor;
    private ExecutorService mProducerExecutor;
    private ImageReaderProxy mReader;

    private static final int NUM_TOTAL_FRAMES = 5;
    private static final int IMAGE_QUEUE_DEPTH = 2;

    private static ImageReaderProxy.OnImageAvailableListener createMockListener() {
        ImageReaderProxy.OnImageAvailableListener mockListener =
                mock(ImageReaderProxy.OnImageAvailableListener.class);
        doAnswer(args -> {
            ImageReaderProxy reader = args.getArgument(0);
            ImageProxy image = reader.acquireLatestImage();
            if (image != null) {
                image.close();
            }

            return null;
        }).when(mockListener).onImageAvailable(any(ImageReaderProxy.class));

        return mockListener;
    }

    @Before
    public void setUp() {
        mProducerExecutor = Executors.newSingleThreadExecutor();
        mConsumerExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        if (mProducerExecutor != null) {
            mProducerExecutor.shutdown();
        }

        if (mConsumerExecutor != null) {
            mConsumerExecutor.shutdown();
        }

        if (mReader != null) {
            mReader.close();
        }
    }

    @Test
    public void isolatedReaderGetsFrames() {
        mReader = ImageReaderProxys.createIsolatedReader(
                640, 480, ImageFormat.YUV_420_888, IMAGE_QUEUE_DEPTH);
        ImageReaderProxy.OnImageAvailableListener mockListener = createMockListener();
        mReader.setOnImageAvailableListener(mockListener, mConsumerExecutor);

        produceFrames(mReader);

        verify(mockListener, timeout(3000).times(NUM_TOTAL_FRAMES)).onImageAvailable(
                any(ImageReaderProxy.class));
    }

    private void produceFrames(@NonNull ImageReaderProxy imageReader) {
        mProducerExecutor.execute(() -> {
            try (ImageWriter writer = ImageWriter.newInstance(imageReader.getSurface(),
                    IMAGE_QUEUE_DEPTH)) {
                for (int i = 0; i < ImageReaderProxysTest.NUM_TOTAL_FRAMES; ++i) {
                    Image image = writer.dequeueInputImage();
                    writer.queueInputImage(image);
                }
            }
        });
    }
}
