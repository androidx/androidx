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

import static androidx.camera.core.MirrorMode.MIRROR_MODE_OFF;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ImageAnalysisConfig;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CameraXUtil;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.camera.testing.fakes.FakeCameraFactory;
import androidx.camera.testing.fakes.FakeCameraInfoInternal;
import androidx.camera.testing.fakes.FakeImageReaderProxy;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Unit test for {@link ImageAnalysis}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageAnalysisTest {

    private static final Size APP_RESOLUTION = new Size(100, 200);
    private static final Size ANALYZER_RESOLUTION = new Size(300, 400);
    private static final Size FLIPPED_ANALYZER_RESOLUTION = new Size(400, 300);
    private static final Size DEFAULT_RESOLUTION = new Size(640, 480);

    private static final int QUEUE_DEPTH = 8;
    private static final int IMAGE_TAG = 0;
    private static final long TIMESTAMP_1 = 1;
    private static final long TIMESTAMP_2 = 2;
    private static final long TIMESTAMP_3 = 3;

    private Handler mCallbackHandler;
    private Handler mBackgroundHandler;
    private Executor mBackgroundExecutor;
    private List<ImageProxy> mImageProxiesReceived;
    private ImageAnalysis mImageAnalysis;
    private FakeImageReaderProxy mFakeImageReaderProxy;
    private HandlerThread mBackgroundThread;
    private HandlerThread mCallbackThread;
    private TagBundle mTagBundle;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        mCallbackThread = new HandlerThread("Callback");
        mCallbackThread.start();
        // Explicitly pause callback thread since we will control execution manually in tests
        shadowOf(mCallbackThread.getLooper()).pause();
        mCallbackHandler = new Handler(mCallbackThread.getLooper());

        mBackgroundThread = new HandlerThread("Background");
        mBackgroundThread.start();
        // Explicitly pause background thread since we will control execution manually in tests
        shadowOf(mBackgroundThread.getLooper()).pause();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundExecutor = CameraXExecutors.newHandlerExecutor(mBackgroundHandler);

        mImageProxiesReceived = new ArrayList<>();

        mTagBundle = TagBundle.create(new Pair<>("FakeCaptureStageId", IMAGE_TAG));

        CameraInternal camera = new FakeCamera();

        CameraFactory.Provider cameraFactoryProvider = (ignored1, ignored2, ignored3) -> {
            FakeCameraFactory cameraFactory = new FakeCameraFactory();
            cameraFactory.insertDefaultBackCamera(camera.getCameraInfoInternal().getCameraId(),
                    () -> camera);
            return cameraFactory;
        };
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).setCameraFactoryProvider(cameraFactoryProvider).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraXUtil.initialize(context, cameraXConfig).get();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        mImageProxiesReceived.clear();
        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS);
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
        }
        if (mCallbackThread != null) {
            mCallbackThread.quitSafely();
        }
    }

    @Test
    public void verifySupportedEffects() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        assertThat(imageAnalysis.isEffectTargetsSupported(CameraEffect.PREVIEW)).isFalse();
        assertThat(imageAnalysis.isEffectTargetsSupported(CameraEffect.IMAGE_CAPTURE)).isFalse();
        assertThat(imageAnalysis.isEffectTargetsSupported(CameraEffect.VIDEO_CAPTURE)).isFalse();
    }

    @Test
    public void canSetQueueDepth() {
        assertThat(getMergedImageAnalysisConfig(null, null, QUEUE_DEPTH,
                false).getImageQueueDepth()).isEqualTo(QUEUE_DEPTH);
    }

    @Test
    public void defaultMirrorModeIsOff() {
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        assertThat(imageAnalysis.getMirrorModeInternal()).isEqualTo(MIRROR_MODE_OFF);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setMirrorMode_throwException() {
        new ImageAnalysis.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY);
    }

    @Test
    public void setAnalyzerWithResolution_doesNotOverridesUseCaseResolution_legacyApi() {
        assertThat(getMergedImageAnalysisConfig(APP_RESOLUTION, ANALYZER_RESOLUTION, -1,
                false).getTargetResolution()).isEqualTo(APP_RESOLUTION);
    }

    @Test
    public void setAnalyzerWithResolution_doesNotOverridesUseCaseResolution_resolutionSelector() {
        ImageAnalysisConfig config = getMergedImageAnalysisConfig(APP_RESOLUTION,
                ANALYZER_RESOLUTION, -1, true);
        assertThat(config.getResolutionSelector().getResolutionStrategy().getBoundSize()).isEqualTo(
                APP_RESOLUTION);
    }

    @Test
    public void setAnalyzerWithResolution_usedAsDefaultUseCaseResolution_legacyApi() {
        assertThat(
                getMergedImageAnalysisConfig(null, ANALYZER_RESOLUTION, -1,
                        false).getTargetResolution()).isEqualTo(FLIPPED_ANALYZER_RESOLUTION);
    }

    @Test
    public void setAnalyzerWithResolution_usedAsDefaultUseCaseResolution_resolutionSelector() {
        ImageAnalysisConfig config = getMergedImageAnalysisConfig(null,
                ANALYZER_RESOLUTION, -1, true);
        assertThat(config.getResolutionSelector().getResolutionStrategy().getBoundSize()).isEqualTo(
                FLIPPED_ANALYZER_RESOLUTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noAppOrAnalyzerResolution_noMergedOption_legacyApi() {
        getMergedImageAnalysisConfig(null, null, -1, false).getTargetResolution();
    }

    @SuppressWarnings("deprecation") // legacy resolution API
    @NonNull
    private ImageAnalysisConfig getMergedImageAnalysisConfig(
            @Nullable Size appResolution,
            @Nullable Size analyzerResolution,
            int queueDepth,
            boolean useResolutionSelector) {
        // Arrange: set up ImageAnalysis.
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();

        // Sets preferred resolution by ResolutionSelector or legacy API
        if (useResolutionSelector) {
            ResolutionSelector.Builder selectorBuilder = new ResolutionSelector.Builder();
            if (appResolution != null) {
                selectorBuilder.setResolutionStrategy(new ResolutionStrategy(appResolution,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER));
            } else {
                selectorBuilder.setAspectRatioStrategy(
                        AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY);
            }
            builder.setResolutionSelector(selectorBuilder.build());
        } else {
            if (appResolution != null) {
                builder.setTargetResolution(appResolution);
            }
        }

        if (queueDepth >= 0) {
            builder.setImageQueueDepth(QUEUE_DEPTH);
        }

        mImageAnalysis = builder.build();
        // Analyzer that overrides the resolution.
        ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                // no-op
            }

            @Override
            public Size getDefaultTargetResolution() {
                return analyzerResolution;
            }
        };

        // Act: set the analyzer.
        mImageAnalysis.setAnalyzer(mBackgroundExecutor, analyzer);

        return (ImageAnalysisConfig) mImageAnalysis.mergeConfigs(
                new FakeCameraInfoInternal(90, CameraSelector.LENS_FACING_BACK), null,
                null);
    }

    @NonNull
    private ImageAnalysisConfig createDefaultConfig() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        builder.setDefaultResolution(DEFAULT_RESOLUTION);
        return builder.getUseCaseConfig();
    }

    @Test
    public void resultSize_isEqualToSurfaceSize() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);

        // Assert.
        assertThat(Iterables.getOnlyElement(mImageProxiesReceived).getHeight()).isEqualTo(
                mFakeImageReaderProxy.getHeight());
        assertThat(Iterables.getOnlyElement(mImageProxiesReceived).getWidth()).isEqualTo(
                mFakeImageReaderProxy.getWidth());
    }

    @Test
    public void nonBlockingAnalyzerClosed_imageNotAnalyzed() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);

        // Clear ImageAnalysis and flush both handlers. No more image should be received because
        // it's closed.
        mImageAnalysis.onUnbind();
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);
    }

    @Test
    public void blockingAnalyzerClosed_imageNotAnalyzed() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and it's still empty because it's close.
        mImageAnalysis.onUnbind();
        flushHandler(mCallbackHandler);
        assertThat(mImageProxiesReceived).isEmpty();
    }

    @Test
    public void keepOnlyLatestStrategy_doesNotBlock() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and image1 is received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1);

        // Flush both handlers and the previous cached image3 is received (image2 was dropped). The
        // code alternates the 2 threads so they have to be both flushed to proceed.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1, TIMESTAMP_3);

        // Flush both handlers and no more frame.
        flushHandler(mBackgroundHandler);
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived()).containsExactly(TIMESTAMP_1, TIMESTAMP_3);
    }

    @Test
    public void blockProducerStrategy_doesNotDropFrames() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        // Arrange.
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER);

        // Act.
        // Receive images from camera feed.
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_1);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_2);
        flushHandler(mBackgroundHandler);
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, TIMESTAMP_3);
        flushHandler(mBackgroundHandler);

        // Assert.
        // No image is received because callback handler is blocked.
        assertThat(mImageProxiesReceived).isEmpty();

        // Flush callback handler and 3 frames received.
        flushHandler(mCallbackHandler);
        assertThat(getImageTimestampsReceived())
                .containsExactly(TIMESTAMP_1, TIMESTAMP_2, TIMESTAMP_3);
    }

    /*
     *  Verify that ImageAnalysis#setAnalyzer won't cause any image leakage.
     */
    @Test
    public void analyzerSetMultipleTimesInKeepOnlyLatestMode() throws InterruptedException,
            CameraUseCaseAdapter.CameraException {
        setUpImageAnalysisWithStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        assertCanReceiveAnalysisImage(mImageAnalysis);

        List<ImageProxy> postedImageProxies = new ArrayList<>();
        ImageAnalysis.Analyzer slowImageAnalyzer = image -> {
            // image left as unclosed until we closed all images in postedImageProxies.
            postedImageProxies.add(image);
        };

        mImageAnalysis.setAnalyzer(CameraXExecutors.directExecutor(), slowImageAnalyzer);
        triggerNextImage();  // +1 unclosed image (mPostedImage)
        triggerNextImage();  // +1 unclosed image (mCachedImage) if the image leakage happens.

        // If setAnalysis does thing inappropriately(e.g, clear mCachedImage reference without
        // closing it), previous unclosed image(mCachedImage) won't be closed.
        mImageAnalysis.setAnalyzer(CameraXExecutors.directExecutor(), slowImageAnalyzer);
        triggerNextImage();  //+1 unclosed image (mCachedImage) if the image leakage happens.

        mImageAnalysis.setAnalyzer(CameraXExecutors.directExecutor(), slowImageAnalyzer);
        triggerNextImage(); // +1 unclosed image (mCachedImage) if the image leakage happens.

        mImageAnalysis.setAnalyzer(CameraXExecutors.directExecutor(), slowImageAnalyzer);
        triggerNextImage(); // +1 unclosed image (mCachedImage) if the image leakage happens.
        // If unclosed image reaches 5 (MaxImages == 4), it will throw
        // IllegalStateException(MaxImages) and no image can be received.

        // Closed all posted ImageProxies to ensure the analyzer can receive ImageProxy normally.
        for (ImageProxy imagePendingProxy : postedImageProxies) {
            imagePendingProxy.close();
        }

        // If image leakage happens, 4 unclosed image will never be closed. It means the analyzer
        // won't be able to receive images anymore.
        assertCanReceiveAnalysisImage(mImageAnalysis);
    }

    @SuppressWarnings("deprecation") // test for legacy resolution API
    @Test
    public void throwException_whenSetBothTargetResolutionAndAspectRatio() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImageAnalysis.Builder()
                        .setTargetResolution(SizeUtil.RESOLUTION_VGA)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .build());
    }

    @SuppressWarnings("deprecation") // test for legacy resolution API
    @Test
    public void throwException_whenSetTargetResolutionWithResolutionSelector() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImageAnalysis.Builder()
                        .setTargetResolution(SizeUtil.RESOLUTION_VGA)
                        .setResolutionSelector(new ResolutionSelector.Builder().build())
                        .build());
    }

    @SuppressWarnings("deprecation") // test for legacy resolution API
    @Test
    public void throwException_whenSetTargetAspectRatioWithResolutionSelector() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setResolutionSelector(new ResolutionSelector.Builder().build())
                        .build());
    }

    void assertCanReceiveAnalysisImage(ImageAnalysis imageAnalysis) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        imageAnalysis.setAnalyzer(CameraXExecutors.directExecutor(), image -> {
            image.close();
            latch.countDown();
        });
        triggerNextImage();
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    void triggerNextImage() throws InterruptedException {
        mFakeImageReaderProxy.triggerImageAvailable(mTagBundle, SystemClock.elapsedRealtime());
        flushHandler(mBackgroundHandler);
    }

    private void setUpImageAnalysisWithStrategy(
            @ImageAnalysis.BackpressureStrategy int backpressureStrategy) throws
            CameraUseCaseAdapter.CameraException {
        setUpImageAnalysisWithStrategy(backpressureStrategy, null);
    }

    private void setUpImageAnalysisWithStrategy(
            @ImageAnalysis.BackpressureStrategy int backpressureStrategy, ViewPort viewPort) throws
            CameraUseCaseAdapter.CameraException {
        mImageAnalysis = new ImageAnalysis.Builder()
                .setBackgroundExecutor(mBackgroundExecutor)
                .setTargetRotation(Surface.ROTATION_0)
                .setImageQueueDepth(QUEUE_DEPTH)
                .setBackpressureStrategy(backpressureStrategy)
                .setImageReaderProxyProvider(
                        (width, height, format, queueDepth, usage) -> {
                            mFakeImageReaderProxy = FakeImageReaderProxy.newInstance(width,
                                    height, format, queueDepth, usage);
                            return mFakeImageReaderProxy;
                        })
                .setSessionOptionUnpacker((resolution, config, builder) -> {
                })
                .setOnePixelShiftEnabled(false)
                .build();

        mImageAnalysis.setAnalyzer(CameraXExecutors.newHandlerExecutor(mCallbackHandler),
                (image) -> {
                    mImageProxiesReceived.add(image);
                    image.close();
                }
        );

        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(ApplicationProvider.getApplicationContext(),
                        CameraSelector.DEFAULT_BACK_CAMERA);
        cameraUseCaseAdapter.setViewPort(viewPort);
        cameraUseCaseAdapter.addUseCases(Collections.singleton(mImageAnalysis));
    }

    private List<Long> getImageTimestampsReceived() {
        List<Long> imagesReceived = new ArrayList<>();
        for (ImageProxy imageProxy : mImageProxiesReceived) {
            imagesReceived.add(imageProxy.getImageInfo().getTimestamp());
        }
        return imagesReceived;
    }

    /**
     * Flushes a {@link Handler} to run all pending tasks.
     *
     * @param handler the {@link Handler} to flush.
     */
    private static void flushHandler(Handler handler) {
        shadowOf(handler.getLooper()).idle();
    }
}
