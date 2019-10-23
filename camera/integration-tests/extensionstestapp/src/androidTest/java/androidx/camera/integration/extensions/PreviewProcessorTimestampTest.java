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

package androidx.camera.integration.extensions;

import static androidx.camera.core.PreviewUtil.createPreviewSurfaceCallback;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Instrumentation;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.ExperimentalCamera2Interop;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureProcessor;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.LensFacing;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.PreviewUtil;
import androidx.camera.extensions.AutoImageCaptureExtender;
import androidx.camera.extensions.AutoPreviewExtender;
import androidx.camera.extensions.BeautyImageCaptureExtender;
import androidx.camera.extensions.BeautyPreviewExtender;
import androidx.camera.extensions.BokehImageCaptureExtender;
import androidx.camera.extensions.BokehPreviewExtender;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.extensions.HdrPreviewExtender;
import androidx.camera.extensions.ImageCaptureExtender;
import androidx.camera.extensions.NightImageCaptureExtender;
import androidx.camera.extensions.NightPreviewExtender;
import androidx.camera.extensions.PreviewExtender;
import androidx.camera.testing.GLUtil;
import androidx.camera.testing.TimestampCaptureProcessor;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests that the {@link androidx.camera.extensions.impl.PreviewImageProcessorImpl} properly
 * populates the timestamp for the {@link SurfaceTexture}.
 */
@MediumTest
@RunWith(Parameterized.class)
public class PreviewProcessorTimestampTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FakeLifecycleOwner mLifecycleOwner;
    private CameraDevice.StateCallback mCameraStatusCallback;
    private ExtensionsManager.EffectMode mEffectMode;
    private LensFacing mLensFacing;
    private CountDownLatch mLatch;
    private CountDownLatch mInputTimestampsLatch;
    private CountDownLatch mOutputTimestampsLatch;
    private CountDownLatch mSurfaceTextureLatch;
    private Set<Long> mInputTimestamps = new HashSet<>();
    private Set<Long> mOutputTimestamps = new HashSet<>();

    TimestampCaptureProcessor.TimestampListener mTimestampListener;
    SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener;

    private ImageCaptureConfig.Builder mImageCaptureConfigBuilder;
    private PreviewConfig.Builder mPreviewConfigBuilder;
    private ImageAnalysisConfig.Builder mImageAnalysisConfigBuilder;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {ExtensionsManager.EffectMode.BOKEH, LensFacing.FRONT},
                {ExtensionsManager.EffectMode.BOKEH, LensFacing.BACK},
                {ExtensionsManager.EffectMode.HDR, LensFacing.FRONT},
                {ExtensionsManager.EffectMode.HDR, LensFacing.BACK},
                {ExtensionsManager.EffectMode.BEAUTY, LensFacing.FRONT},
                {ExtensionsManager.EffectMode.BEAUTY, LensFacing.BACK},
                {ExtensionsManager.EffectMode.NIGHT, LensFacing.FRONT},
                {ExtensionsManager.EffectMode.NIGHT, LensFacing.BACK},
                {ExtensionsManager.EffectMode.AUTO, LensFacing.FRONT},
                {ExtensionsManager.EffectMode.AUTO, LensFacing.BACK}
        });
    }

    public PreviewProcessorTimestampTest(ExtensionsManager.EffectMode effectMode,
            LensFacing lensFacing) {
        mEffectMode = effectMode;
        mLensFacing = lensFacing;
    }

    @Before
    @UseExperimental(markerClass = ExperimentalCamera2Interop.class)
    public void setUp() {
        mProcessingHandlerThread =
                new HandlerThread("Processing");
        mProcessingHandlerThread.start();
        mProcessingHandler = new Handler(mProcessingHandlerThread.getLooper());

        assumeTrue(androidx.camera.testing.CameraUtil.deviceHasCamera());

        mLifecycleOwner = new FakeLifecycleOwner();

        mImageCaptureConfigBuilder = new ImageCaptureConfig.Builder();
        mPreviewConfigBuilder = new PreviewConfig.Builder();
        mImageAnalysisConfigBuilder = new ImageAnalysisConfig.Builder();
        mCameraStatusCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                mLatch = new CountDownLatch(1);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {

            }

            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                mLatch.countDown();
            }
        };

        mInputTimestampsLatch = new CountDownLatch(1);

        mTimestampListener = new TimestampCaptureProcessor.TimestampListener() {
            boolean mComplete = false;

            @Override
            public void onTimestampAvailable(long timestamp) {
                if (mComplete) {
                    return;
                }

                mInputTimestamps.add(timestamp);
                if (mInputTimestamps.size() > 10) {
                    mInputTimestampsLatch.countDown();
                    mComplete = true;
                }
            }
        };

        mOutputTimestampsLatch = new CountDownLatch(1);

        mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
            boolean mComplete = false;

            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mComplete) {
                    return;
                }
                surfaceTexture.updateTexImage();

                mOutputTimestamps.add(surfaceTexture.getTimestamp());
                if (mOutputTimestamps.size() > 10) {
                    mOutputTimestampsLatch.countDown();
                    mComplete = true;
                }
            }
        };

        mSurfaceTextureLatch = new CountDownLatch(1);

        new Camera2Config.Extender(mImageCaptureConfigBuilder).setDeviceStateCallback(
                mCameraStatusCallback);
    }

    @After
    public void cleanUp() throws InterruptedException {
        if (mLatch != null) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);

            // Make sure camera was closed.
            mLatch.await(3000, TimeUnit.MILLISECONDS);
        }
    }

    private HandlerThread mProcessingHandlerThread;
    private Handler mProcessingHandler;

    @Test
    public void timestampIsCorrect() throws InterruptedException {
        assumeTrue(androidx.camera.testing.CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing));

        enableExtension(mEffectMode, mLensFacing);

        // To test bind/unbind and take picture.
        ImageCapture imageCapture = new ImageCapture(mImageCaptureConfigBuilder.build());

        PreviewConfig previewConfig = mPreviewConfigBuilder
                .build();
        CaptureProcessor previewCaptureProcessor = previewConfig.getCaptureProcessor(null);
        assumeNotNull(previewCaptureProcessor);
        mPreviewConfigBuilder.setCaptureProcessor(
                new TimestampCaptureProcessor(previewCaptureProcessor, mTimestampListener));

        Preview preview = new Preview(mPreviewConfigBuilder.build());

        // To set the update listener and Preview will change to active state.
        preview.setPreviewSurfaceCallback(createPreviewSurfaceCallback(
                new PreviewUtil.SurfaceTextureCallback() {
                    @Override
                    public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
                        surfaceTexture.attachToGLContext(GLUtil.getTexIdFromGLContext());
                        surfaceTexture.setOnFrameAvailableListener(
                                mOnFrameAvailableListener, mProcessingHandler);
                        mSurfaceTextureLatch.countDown();
                    }

                    @Override
                    public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                        // No-op.
                    }
                }));

        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, preview, imageCapture);

            mLifecycleOwner.startAndResume();
        });

        assertTrue(mSurfaceTextureLatch.await(1000, TimeUnit.MILLISECONDS));

        assertTrue(mInputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(mOutputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS));

        assertEquals(mInputTimestamps, mOutputTimestamps);
    }

    /**
     * To invoke the enableExtension() method for different effect.
     */
    private void enableExtension(ExtensionsManager.EffectMode effectMode, LensFacing lensFacing) {

        mImageCaptureConfigBuilder.setLensFacing(lensFacing);
        mPreviewConfigBuilder.setLensFacing(lensFacing);
        mImageAnalysisConfigBuilder.setLensFacing(lensFacing);

        ImageCaptureExtender imageCaptureExtender = null;
        PreviewExtender previewExtender = null;

        switch (effectMode) {
            case HDR:
                imageCaptureExtender = HdrImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = HdrPreviewExtender.create(mPreviewConfigBuilder);
                break;
            case BOKEH:
                imageCaptureExtender = BokehImageCaptureExtender.create(
                        mImageCaptureConfigBuilder);
                previewExtender = BokehPreviewExtender.create(mPreviewConfigBuilder);
                break;
            case BEAUTY:
                imageCaptureExtender = BeautyImageCaptureExtender.create(
                        mImageCaptureConfigBuilder);
                previewExtender = BeautyPreviewExtender.create(mPreviewConfigBuilder);
                break;
            case NIGHT:
                imageCaptureExtender = NightImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = NightPreviewExtender.create(mPreviewConfigBuilder);
                break;
            case AUTO:
                imageCaptureExtender = AutoImageCaptureExtender.create(mImageCaptureConfigBuilder);
                previewExtender = AutoPreviewExtender.create(mPreviewConfigBuilder);
                break;
        }

        assertNotNull(imageCaptureExtender);
        assertNotNull(previewExtender);

        assertTrue(previewExtender.isExtensionAvailable());
        previewExtender.enableExtension();
        assertTrue(imageCaptureExtender.isExtensionAvailable());
        imageCaptureExtender.enableExtension();
    }
}
