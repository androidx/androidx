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

import static androidx.camera.testing.SurfaceTextureProvider.createSurfaceTextureProvider;

import static junit.framework.TestCase.assertEquals;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.GLUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.camera.testing.TimestampCaptureProcessor;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests that the {@link androidx.camera.extensions.impl.PreviewImageProcessorImpl} properly
 * populates the timestamp for the {@link SurfaceTexture}.
 */
@MediumTest
@RunWith(Parameterized.class)
public class PreviewProcessorTimestampTest {

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @ExtensionMode.Mode
    private int mExtensionMode;
    @CameraSelector.LensFacing
    private int mLensFacing;
    private CountDownLatch mInputTimestampsLatch;
    private CountDownLatch mOutputTimestampsLatch;
    private CountDownLatch mSurfaceTextureLatch;
    private Set<Long> mInputTimestamps = new HashSet<>();
    private Set<Long> mOutputTimestamps = new HashSet<>();

    TimestampCaptureProcessor.TimestampListener mTimestampListener;
    SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener;

    private ImageCapture.Builder mImageCaptureBuilder;
    private Preview.Builder mPreviewBuilder;

    private ProcessCameraProvider mProcessCameraProvider;
    private ExtensionsManager mExtensionsManager;

    private CameraSelector mCameraSelector;

    private FakeLifecycleOwner mFakeLifecycleOwner = new FakeLifecycleOwner();

    @Parameterized.Parameters(name = "effect = {0}, facing = {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {ExtensionMode.BOKEH, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.BOKEH, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.HDR, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.HDR, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.BEAUTY, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.BEAUTY, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.NIGHT, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.NIGHT, CameraSelector.LENS_FACING_BACK},
                {ExtensionMode.AUTO, CameraSelector.LENS_FACING_FRONT},
                {ExtensionMode.AUTO, CameraSelector.LENS_FACING_BACK}
        });
    }

    public PreviewProcessorTimestampTest(@ExtensionMode.Mode int extensionMode,
            @CameraSelector.LensFacing int lensFacing) {
        mExtensionMode = extensionMode;
        mLensFacing = lensFacing;
    }

    @Before
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    public void setUp() throws Exception {
        mProcessingHandlerThread =
                new HandlerThread("Processing");
        mProcessingHandlerThread.start();
        mProcessingHandler = new Handler(mProcessingHandlerThread.getLooper());

        assumeTrue(androidx.camera.testing.CameraUtil.deviceHasCamera());

        mProcessCameraProvider = ProcessCameraProvider.getInstance(mContext).get(10000,
                TimeUnit.MILLISECONDS);

        mExtensionsManager = ExtensionsManager.getInstance(mContext).get(10000,
                TimeUnit.MILLISECONDS);

        mCameraSelector = new CameraSelector.Builder().requireLensFacing(mLensFacing).build();
        mFakeLifecycleOwner.startAndResume();
        mImageCaptureBuilder = new ImageCapture.Builder();
        mPreviewBuilder = new Preview.Builder();
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

                synchronized (mIsSurfaceTextureReleasedLock) {
                    if (!mIsSurfaceTextureReleased) {
                        mInstrumentation.runOnMainSync(() -> {
                            surfaceTexture.updateTexImage();
                        });
                    }
                }

                mOutputTimestamps.add(surfaceTexture.getTimestamp());
                if (mOutputTimestamps.size() > 10) {
                    mOutputTimestampsLatch.countDown();
                    mComplete = true;
                }
            }
        };

        mSurfaceTextureLatch = new CountDownLatch(1);
    }

    @After
    public void cleanUp() throws InterruptedException, ExecutionException, TimeoutException {
        if (mProcessCameraProvider != null) {
            mProcessCameraProvider.shutdown().get(10000, TimeUnit.MILLISECONDS);
            mExtensionsManager.shutdown().get(10000, TimeUnit.MILLISECONDS);
        }
    }

    private HandlerThread mProcessingHandlerThread;
    private Handler mProcessingHandler;

    private boolean mIsSurfaceTextureReleased = false;
    private Object mIsSurfaceTextureReleasedLock = new Object();

    @Test
    public void timestampIsCorrect() throws InterruptedException {
        assumeTrue(androidx.camera.testing.CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(mExtensionsManager.isExtensionAvailable(mProcessCameraProvider, mCameraSelector,
                mExtensionMode));

        // To test bind/unbind and take picture.
        ImageCapture imageCapture = mImageCaptureBuilder.build();

        PreviewConfig previewConfig = mPreviewBuilder.getUseCaseConfig();
        CaptureProcessor previewCaptureProcessor = previewConfig.getCaptureProcessor(null);
        assumeNotNull(previewCaptureProcessor);
        mPreviewBuilder.setCaptureProcessor(
                new TimestampCaptureProcessor(previewCaptureProcessor, mTimestampListener));

        Preview preview = mPreviewBuilder.build();

        CameraSelector extensionCameraSelector =
                mExtensionsManager.getExtensionEnabledCameraSelector(mProcessCameraProvider,
                        mCameraSelector, mExtensionMode);
        mInstrumentation.runOnMainSync(() -> {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(createSurfaceTextureProvider(
                    new SurfaceTextureProvider.SurfaceTextureCallback() {
                        @Override
                        public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                                @NonNull Size resolution) {
                            surfaceTexture.attachToGLContext(GLUtil.getTexIdFromGLContext());
                            surfaceTexture.setOnFrameAvailableListener(
                                    mOnFrameAvailableListener, mProcessingHandler);
                            mSurfaceTextureLatch.countDown();
                        }

                        @Override
                        public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                            synchronized (mIsSurfaceTextureReleasedLock) {
                                mIsSurfaceTextureReleased = true;
                                surfaceTexture.release();
                            }
                        }
                    }));

            mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner, extensionCameraSelector,
                    preview, imageCapture);
        });

        assertTrue(mSurfaceTextureLatch.await(1000, TimeUnit.MILLISECONDS));

        assertTrue(mInputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(mOutputTimestampsLatch.await(1000, TimeUnit.MILLISECONDS));

        assertEquals(mInputTimestamps, mOutputTimestamps);
    }
}
