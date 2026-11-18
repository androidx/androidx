/*
 * Copyright 2019 The Android Open Source Project
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

import static android.os.Looper.getMainLooper;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.MutableTagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link ImageAnalysisNonBlockingAnalyzer}
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Config.ALL_SDKS}, instrumentedPackages = { "androidx.camera.core" })
public class ImageAnalysisNonBlockingAnalyzerTest {
    private ImageAnalysisNonBlockingAnalyzer mImageAnalysisNonBlockingAnalyzer;
    private static final AtomicInteger ROTATION = new AtomicInteger(0);
    private ImageAnalysis.Analyzer mAnalyzer;
    private ImageReaderProxy mImageReaderProxy;
    private ImageProxy mImageProxy;
    private ImageInfo mImageInfo;
    private MutableTagBundle mTagBundle;
    private String mTagBundleKey = "FakeTagBundleKey";

    @Before
    public void setup() {
        mImageInfo = mock(ImageInfo.class);
        mImageProxy = mock(ImageProxy.class);
        mTagBundle = MutableTagBundle.create();
        mTagBundle.putTag(mTagBundleKey, 0);

        when(mImageProxy.getImageInfo()).thenReturn(mImageInfo);
        when(mImageInfo.getTagBundle()).thenReturn(mTagBundle);

        mImageReaderProxy = mock(ImageReaderProxy.class);
        when(mImageReaderProxy.acquireLatestImage()).thenReturn(mImageProxy);

        mAnalyzer = mock(ImageAnalysis.Analyzer.class);
        mImageAnalysisNonBlockingAnalyzer = spy(new ImageAnalysisNonBlockingAnalyzer(
                CameraXExecutors.directExecutor()));
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(CameraXExecutors.mainThreadExecutor(),
                mAnalyzer);
        mImageAnalysisNonBlockingAnalyzer.setRelativeRotation(ROTATION.get());
        mImageAnalysisNonBlockingAnalyzer.attach();
    }

    @Test
    public void imageClosedAfterAnalyzerDetached() {
        // Arrange.
        mImageAnalysisNonBlockingAnalyzer.detach();
        // Act.
        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);
        shadowOf(getMainLooper()).idle();
        // Assert.
        verify(mImageProxy, times(1)).close();
    }

    @Test
    public void imageNotClosedWhenAnalyzerAttached() {
        // Act.
        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);
        shadowOf(getMainLooper()).idle();
        // Assert.
        verify(mImageProxy, never()).close();
    }

    @Test
    public void analysisRunWhenAnalyzerAttached() {
        // Act.
        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);
        shadowOf(getMainLooper()).idle();

        // Assert.
        ArgumentCaptor<ImageProxy> imageProxyArgumentCaptor =
                ArgumentCaptor.forClass(ImageProxy.class);
        verify(mAnalyzer, times(1)).analyze(
                imageProxyArgumentCaptor.capture());
        // Check for equality of ImageInfo because it won't necessarily be same instance of
        // ImageProxy that is analyzed, but an equivalent instance.
        ImageInfo capturedImageInfo = imageProxyArgumentCaptor.getValue().getImageInfo();
        assertEquals(mImageInfo.getTagBundle().getTag(mTagBundleKey),
                capturedImageInfo.getTagBundle().getTag(mTagBundleKey));
        assertEquals(mImageInfo.getTimestamp(), capturedImageInfo.getTimestamp());
        assertEquals(ROTATION.get(), capturedImageInfo.getRotationDegrees());
    }

    @Test
    public void setAnalyzerNull_incomingImageClosed() {
        // Arrange.
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(null, null);
        // Act.
        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);
        shadowOf(getMainLooper()).idle();
        // Assert.
        verify(mImageProxy).close();
    }

    @Test
    public void closeAnalyzer_cachedImageBecomesNull() {
        // Arrange.
        mImageAnalysisNonBlockingAnalyzer.mCachedImage = mImageProxy;
        // Act.
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(null, null);
        // Assert.
        assertThat(mImageAnalysisNonBlockingAnalyzer.mCachedImage).isNull();
    }

    @Test
    public void detachAnalyzer_cachedImageBecomesNull() {
        // Arrange.
        mImageAnalysisNonBlockingAnalyzer.mCachedImage = mImageProxy;
        // Act.
        mImageAnalysisNonBlockingAnalyzer.detach();
        // Assert.
        assertThat(mImageAnalysisNonBlockingAnalyzer.mCachedImage).isNull();
    }

    @Test
    public void closingPostedImageMultipleTimes_doesNotClearSubsequentPostedImage() {
        // Arrange: capture analyzed ImageProxies without closing immediately.
        Queue<Runnable> pendingAnalyzerTasks = new ArrayDeque<>();
        AtomicReference<ImageProxy> capturedImage = new AtomicReference<>();
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(
                pendingAnalyzerTasks::add, image -> capturedImage.set(image));

        ImageProxy image1 = createMockImageProxy(1L);
        ImageProxy image2 = createMockImageProxy(2L);
        ImageProxy image3 = createMockImageProxy(3L);

        // Post image1 and cache image2.
        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image1);
        assertThat(pendingAnalyzerTasks).hasSize(1);
        pendingAnalyzerTasks.poll().run();
        ImageProxy posted1 = capturedImage.getAndSet(null);
        assertThat(posted1).isNotNull();

        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image2);

        // Act: double-close posted1; first close dispatches image2, second close must be a no-op.
        posted1.close();
        assertThat(pendingAnalyzerTasks).hasSize(1);
        pendingAnalyzerTasks.poll().run();
        ImageProxy posted2 = capturedImage.getAndSet(null);
        assertThat(posted2).isNotNull();

        posted1.close();

        // image3 arrives while posted2 is still unclosed; it must be cached, not posted
        // concurrently.
        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image3);
        assertThat(pendingAnalyzerTasks).isEmpty();
        assertThat(mImageAnalysisNonBlockingAnalyzer.mCachedImage).isSameInstanceAs(image3);

        // Closing posted2 dispatches image3 cleanly.
        posted2.close();
        assertThat(pendingAnalyzerTasks).hasSize(1);
        assertThat(mImageAnalysisNonBlockingAnalyzer.mCachedImage).isNull();
        pendingAnalyzerTasks.poll().run();
        ImageProxy posted3 = capturedImage.getAndSet(null);
        assertThat(posted3).isNotNull();
        posted3.close();
    }

    @Test
    public void analyzeCachedImage_catchesIllegalStateException_andRecoversOnNextFrame() {
        // Arrange.
        Queue<Runnable> pendingAnalyzerTasks = new ArrayDeque<>();
        AtomicReference<ImageProxy> capturedImage = new AtomicReference<>();
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(
                pendingAnalyzerTasks::add, image -> capturedImage.set(image));

        ImageProxy image1 = createMockImageProxy(1L);
        ImageProxy image2 = createMockImageProxy(2L);
        ImageProxy image3 = createMockImageProxy(3L);

        doAnswer(
                        invocation -> {
                            ImageProxy arg = invocation.getArgument(0);
                            if (arg.getImageInfo().getTimestamp() == 2L) {
                                throw new IllegalStateException(
                                        "maxImages (4) has already been acquired");
                            }
                            return invocation.callRealMethod();
                        })
                .when(mImageAnalysisNonBlockingAnalyzer)
                .analyzeImage(any());

        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image1);
        pendingAnalyzerTasks.poll().run();
        ImageProxy posted1 = capturedImage.getAndSet(null);

        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image2);

        // Act: closing posted1 triggers cached image2 which throws IllegalStateException
        // synchronously inside CacheAnalyzingImageProxy's background callback.
        posted1.close();

        // Assert: failed cached image2 was closed, no exception escaped, and image3 still analyzes.
        verify(image2, times(1)).close();
        mImageAnalysisNonBlockingAnalyzer.onValidImageAvailable(image3);
        assertThat(pendingAnalyzerTasks).hasSize(1);
        pendingAnalyzerTasks.poll().run();
        assertThat(capturedImage.get()).isNotNull();
        capturedImage.get().close();
    }

    @Test
    public void closingPostedImage_handlesRejectedBackgroundExecutorGracefully() {
        // Arrange.
        ImageAnalysisNonBlockingAnalyzer analyzer =
                new ImageAnalysisNonBlockingAnalyzer(
                        command -> {
                            throw new RejectedExecutionException("Background executor shut down");
                        });
        Queue<Runnable> pendingAnalyzerTasks = new ArrayDeque<>();
        AtomicReference<ImageProxy> capturedImage = new AtomicReference<>();
        analyzer.setAnalyzer(pendingAnalyzerTasks::add, image -> capturedImage.set(image));
        analyzer.attach();

        ImageProxy image1 = createMockImageProxy(1L);
        ImageProxy image2 = createMockImageProxy(2L);
        ImageProxy image3 = createMockImageProxy(3L);

        analyzer.onValidImageAvailable(image1);
        pendingAnalyzerTasks.poll().run();
        ImageProxy posted1 = capturedImage.getAndSet(null);
        analyzer.onValidImageAvailable(image2);

        // Act: closing posted1 must not throw RejectedExecutionException; clears gate and cache.
        posted1.close();

        // Assert.
        verify(image2, times(1)).close();
        assertThat(analyzer.mCachedImage).isNull();
        analyzer.onValidImageAvailable(image3);
        assertThat(pendingAnalyzerTasks).hasSize(1);
        pendingAnalyzerTasks.poll().run();
        assertThat(capturedImage.get()).isNotNull();
        assertThat(capturedImage.get().getImageInfo().getTimestamp()).isEqualTo(3L);
        capturedImage.get().close();
    }

    private ImageProxy createMockImageProxy(long timestamp) {
        ImageInfo imageInfo = mock(ImageInfo.class);
        when(imageInfo.getTagBundle()).thenReturn(mTagBundle);
        when(imageInfo.getTimestamp()).thenReturn(timestamp);
        ImageProxy imageProxy = mock(ImageProxy.class);
        when(imageProxy.getImageInfo()).thenReturn(imageInfo);
        return imageProxy;
    }
}
