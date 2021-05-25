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

package androidx.camera.extensions;

import static androidx.camera.extensions.util.ExtensionsTestUtil.assumeCompatibleDevice;
import static androidx.camera.extensions.util.ExtensionsTestUtil.effectModeToExtensionMode;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.app.Instrumentation;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.extensions.ExtensionsErrorListener.ExtensionsErrorCode;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@MediumTest
@RunWith(Parameterized.class)
/**
 * Unit tests for {@link androidx.camera.extensions.ExtensionsErrorListener}.
 * */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public final class ExtensionsErrorListenerTest {
    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    private static final int TIMEOUT_MILLISECONDS = 10000;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return ExtensionsTestUtil.getAllEffectLensFacingCombinations();
    }

    private ExtensionsInfo mExtensionsInfo;
    private CameraSelector mCameraSelector;
    private ExtensionsManager.EffectMode mEffectMode;
    @ExtensionMode.Mode
    private int mExtensionMode;
    @CameraSelector.LensFacing
    private int mLensFacing;
    private CountDownLatch mLatch;
    private ProcessCameraProvider mProcessCameraProvider = null;
    private FakeLifecycleOwner mFakeLifecycleOwner;
    private CameraSelector mExtensionsCameraSelector;
    private CameraUseCaseAdapter mCamera;

    final AtomicReference<ExtensionsErrorCode> mErrorCode = new AtomicReference<>();
    ExtensionsErrorListener mExtensionsErrorListener = new ExtensionsErrorListener() {
        @Override
        public void onError(@NonNull ExtensionsErrorCode errorCode) {
            mErrorCode.set(errorCode);
            mLatch.countDown();
        }
    };

    public ExtensionsErrorListenerTest(ExtensionsManager.EffectMode effectMode,
            @CameraSelector.LensFacing int lensFacing) {
        mEffectMode = effectMode;
        mExtensionMode = effectModeToExtensionMode(effectMode);
        mLensFacing = lensFacing;
        mCameraSelector =
                mLensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.DEFAULT_BACK_CAMERA
                        : CameraSelector.DEFAULT_FRONT_CAMERA;
    }

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        assumeCompatibleDevice();
        assumeTrue(CameraUtil.deviceHasCamera());

        mProcessCameraProvider = ProcessCameraProvider.getInstance(mContext).get(
                TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);

        assumeTrue(CameraUtil.hasCameraWithLensFacing(mLensFacing));
        assumeTrue(ExtensionsTestUtil.initExtensions(mContext));
        assumeTrue(ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing));

        mExtensionsInfo = ExtensionsManager.getExtensionsInfo(mContext);
        mLatch = new CountDownLatch(1);

        mFakeLifecycleOwner = new FakeLifecycleOwner();
        mFakeLifecycleOwner.startAndResume();
        mExtensionsCameraSelector =
                mExtensionsInfo.getExtensionCameraSelector(mCameraSelector, mExtensionMode);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (mCamera != null) {
            mInstrumentation.runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        if (mProcessCameraProvider != null) {
            mProcessCameraProvider.shutdown().get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
            ExtensionsManager.deinit().get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void receiveErrorCode_whenOnlyEnableImageCapture_ByExtenderAPI()
            throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);
        Preview noEffectPreview = ExtensionsTestUtil.createPreviewWithEffect(
                ExtensionsManager.EffectMode.NORMAL, mLensFacing);
        mErrorCode.set(null);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageCapture,
                noEffectPreview);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
    }

    @Test
    public void receiveErrorCode_whenOnlyBindImageCapture_ByExtenderAPI()
            throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);
        mErrorCode.set(null);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageCapture);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
    }

    @Test
    public void receiveErrorCode_whenOnlyEnablePreview_ByExtenderAPI() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture noEffectImageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(
                ExtensionsManager.EffectMode.NORMAL, mLensFacing);
        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mEffectMode, mLensFacing);
        mErrorCode.set(null);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector,
                noEffectImageCapture, preview);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(
                ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
    }

    @Test
    public void receiveErrorCode_whenOnlyBindPreview_ByExtenderAPI() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mEffectMode, mLensFacing);
        mErrorCode.set(null);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, preview);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(
                ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
    }

    @Test
    public void notReceiveErrorCode_whenEnableBothImageCapturePreview_ByExtenderAPI()
            throws InterruptedException, CameraAccessException, CameraInfoUnavailableException {
        assumeTrue(canSupportImageCaptureTogetherWithPreview(mEffectMode, mEffectMode));
        ExtensionsErrorListener mockExtensionsErrorListener = mock(ExtensionsErrorListener.class);
        ExtensionsManager.setExtensionsErrorListener(mockExtensionsErrorListener);

        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);
        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mEffectMode, mLensFacing);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageCapture,
                preview);

        // Waits for one second to get error code.
        Thread.sleep(1000);
        verifyZeroInteractions(mockExtensionsErrorListener);
    }

    @Test
    public void receiveErrorCode_whenEnableMismatchedImageCapturePreview_ByExtenderAPI()
            throws InterruptedException, CameraAccessException, CameraInfoUnavailableException {
        ExtensionsManager.EffectMode mismatchedEffectMode;

        if (mEffectMode != ExtensionsManager.EffectMode.BOKEH) {
            assumeTrue(ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.BOKEH,
                    mLensFacing));
            mismatchedEffectMode = ExtensionsManager.EffectMode.BOKEH;
        } else {
            assumeTrue(ExtensionsManager.isExtensionAvailable(ExtensionsManager.EffectMode.HDR,
                    mLensFacing));
            mismatchedEffectMode = ExtensionsManager.EffectMode.HDR;
        }

        assumeTrue(canSupportImageCaptureTogetherWithPreview(mEffectMode, mismatchedEffectMode));
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        // Creates ImageCapture
        ImageCapture imageCapture = ExtensionsTestUtil.createImageCaptureWithEffect(mEffectMode,
                mLensFacing);

        // Creates mismatched Preview
        Preview preview = ExtensionsTestUtil.createPreviewWithEffect(mismatchedEffectMode,
                mLensFacing);

        List<UseCase> useCaseList = Arrays.asList(imageCapture, preview);

        mErrorCode.set(null);
        // Will receive error code twice
        mLatch = new CountDownLatch(1);

        mCamera = CameraUtil.createCameraAndAttachUseCase(mContext, mCameraSelector, imageCapture,
                preview);

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.MISMATCHED_EXTENSIONS_ENABLED);
    }

    @Test
    public void receiveErrorCode_whenOnlyBindImageCapture() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        ImageCapture imageCapture = new ImageCapture.Builder().build();

        mErrorCode.set(null);

        mInstrumentation.runOnMainSync(
                () -> mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner,
                        mExtensionsCameraSelector, imageCapture));

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(ExtensionsErrorCode.PREVIEW_EXTENSION_REQUIRED);
    }

    @Test
    public void receiveErrorCode_whenOnlyBindPreview() throws InterruptedException {
        ExtensionsManager.setExtensionsErrorListener(mExtensionsErrorListener);

        Preview preview = new Preview.Builder().build();

        mErrorCode.set(null);

        mInstrumentation.runOnMainSync(
                () -> mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner,
                        mExtensionsCameraSelector, preview));

        // Waits for one second to get error code.
        mLatch.await(1, TimeUnit.SECONDS);
        assertThat(mErrorCode.get()).isEqualTo(
                ExtensionsErrorCode.IMAGE_CAPTURE_EXTENSION_REQUIRED);
    }

    @Test
    public void notReceiveErrorCode_whenBindBothImageCapturePreview()
            throws InterruptedException, CameraAccessException, CameraInfoUnavailableException {
        assumeTrue(canSupportImageCaptureTogetherWithPreview(mEffectMode, mEffectMode));
        ExtensionsErrorListener mockExtensionsErrorListener = mock(ExtensionsErrorListener.class);
        ExtensionsManager.setExtensionsErrorListener(mockExtensionsErrorListener);

        ImageCapture imageCapture = new ImageCapture.Builder().build();
        Preview preview = new Preview.Builder().build();

        mInstrumentation.runOnMainSync(
                () -> mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner,
                        mExtensionsCameraSelector, preview, imageCapture));

        // Waits for one second to get error code.
        Thread.sleep(1000);
        verifyZeroInteractions(mockExtensionsErrorListener);
    }

    private boolean canSupportImageCaptureTogetherWithPreview(
            @NonNull ExtensionsManager.EffectMode imageCaptureEffectMode,
            @NonNull ExtensionsManager.EffectMode previewEffectMode)
            throws CameraAccessException, CameraInfoUnavailableException {

        CameraUseCaseAdapter camera = CameraUtil.createCameraUseCaseAdapter(mContext,
                mCameraSelector);
        String type = ((CameraInfoInternal) camera.getCameraInfo()).getImplementationType();

        // Non-Legacy devices can support ImageCapture together with Preview
        if (!type.equals(CameraInfoInternal.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)) {
            return true;
        }

        ImageCaptureExtenderImpl imageCaptureExtenderImpl =
                ExtensionsTestUtil.createImageCaptureExtenderImpl(imageCaptureEffectMode,
                        mLensFacing);

        PreviewExtenderImpl previewExtenderImpl =
                ExtensionsTestUtil.createPreviewExtenderImpl(previewEffectMode, mLensFacing);

        // If the device is Legacy level and both ImageCapture and Preview need YUV streams, it
        // can't be supported.
        if (imageCaptureExtenderImpl.getCaptureProcessor() != null
                && previewExtenderImpl.getProcessorType()
                == PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR) {
            return false;
        }

        return true;
    }
}
