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

package androidx.camera.camera2;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.camera.camera2.impl.Camera2CameraFactory;
import androidx.camera.camera2.impl.util.SemaphoreReleasingCamera2Callbacks;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraRepository;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Contains tests for {@link androidx.camera.core.CameraX} which varies use case combinations to
 * run.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseCombinationTest {
    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    private final MutableLiveData<Long> mAnalysisResult = new MutableLiveData<>();
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
    private Semaphore mSemaphore;
    private FakeLifecycleOwner mLifecycle;
    private HandlerThread mHandlerThread;
    private Handler mMainThreadHandler;
    private CallbackAttachingImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Preview mPreview;
    private ImageAnalysis.Analyzer mImageAnalyzer;
    private CameraRepository mCameraRepository;
    private CameraFactory mCameraFactory;
    private UseCaseGroup mUseCaseGroup;

    private Observer<Long> createCountIncrementingObserver() {
        return new Observer<Long>() {
            @Override
            public void onChanged(Long value) {
                mSemaphore.release();
            }
        };
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.init(context, Camera2AppConfig.create(context));
        mLifecycle = new FakeLifecycleOwner();
        mHandlerThread = new HandlerThread("ErrorHandlerThread");
        mHandlerThread.start();
        mMainThreadHandler = new Handler(Looper.getMainLooper());

        mSemaphore = new Semaphore(0);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (mHandlerThread != null) {
            CameraX.unbindAll();
            mHandlerThread.quitSafely();

            // Wait some time for the cameras to close.
            // We need the cameras to close to bring CameraX
            // back to the initial state.
            Thread.sleep(3000);
        }
    }

    /**
     * Test Combination: Preview + ImageCapture
     */
    @Test
    public void previewCombinesImageCapture() throws InterruptedException {
        initPreview();
        initImageCapture();

        mUseCaseGroup.addUseCase(mImageCapture);
        mUseCaseGroup.addUseCase(mPreview);

        CameraX.bindToLifecycle(mLifecycle, mPreview, mImageCapture);
        mLifecycle.startAndResume();

        mImageCapture.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraCaptureSession.onConfigured callback.
        mImageCapture.mSessionStateCallback.waitForOnConfigured(1);

        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
        assertThat(CameraX.isBound(mPreview)).isTrue();
        assertThat(CameraX.isBound(mImageCapture)).isTrue();
    }

    /**
     * Test Combination: Preview + ImageAnalysis
     */
    @Test
    public void previewCombinesImageAnalysis() throws InterruptedException {
        initImageAnalysis();
        initPreview();

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageAnalysis.setAnalyzer(mImageAnalyzer);

                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());

                CameraX.bindToLifecycle(mLifecycle, mPreview, mImageAnalysis);
                mLifecycle.startAndResume();
            }
        });

        // Wait for 10 frames to be analyzed.
        mSemaphore.acquire(10);

        assertThat(CameraX.isBound(mPreview)).isTrue();
        assertThat(CameraX.isBound(mImageAnalysis)).isTrue();
    }

    /**
     * Test Combination: Preview + ImageAnalysis + ImageCapture
     */
    @Test
    public void previewCombinesImageAnalysisAndImageCapture() throws InterruptedException {
        initPreview();
        initImageAnalysis();
        initImageCapture();

        mUseCaseGroup.addUseCase(mImageCapture);
        mUseCaseGroup.addUseCase(mImageAnalysis);
        mUseCaseGroup.addUseCase(mPreview);

        mImageCapture.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageAnalysis.setAnalyzer(mImageAnalyzer);

                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());
            }
        });

        CameraX.bindToLifecycle(mLifecycle, mPreview, mImageAnalysis, mImageCapture);
        mLifecycle.startAndResume();

        // Wait for 10 frames to be analyzed.
        mSemaphore.acquire(10);

        // Wait for the CameraCaptureSession.onConfigured callback.
        try {
            mImageCapture.mSessionStateCallback.waitForOnConfigured(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(CameraX.isBound(mPreview)).isTrue();
        assertThat(CameraX.isBound(mImageAnalysis)).isTrue();
        assertThat(CameraX.isBound(mImageCapture)).isTrue();
    }

    private void initImageAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig =
                new ImageAnalysisConfig.Builder()
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .setTargetName("ImageAnalysis")
                        .setCallbackHandler(new Handler(Looper.getMainLooper()))
                        .build();
        mImageAnalyzer =
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        mAnalysisResult.postValue(image.getTimestamp());
                    }
                };
        mImageAnalysis = new ImageAnalysis(imageAnalysisConfig);
    }

    private void initImageCapture() {
        mCameraRepository = new CameraRepository();
        mCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
        mCameraRepository.init(mCameraFactory);
        mUseCaseGroup = new UseCaseGroup();

        ImageCaptureConfig imageCaptureConfig =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK).build();
        String cameraId = getCameraIdForLensFacingUnchecked(imageCaptureConfig.getLensFacing());
        mImageCapture = new CallbackAttachingImageCapture(imageCaptureConfig, cameraId);

        mImageCapture.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(
                                imageCaptureConfig.getLensFacing())));
    }

    private void initPreview() {
        PreviewConfig previewConfig =
                new PreviewConfig.Builder()
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .setTargetName("Preview")
                        .build();

        mPreview = new Preview(previewConfig);
    }

    private String getCameraIdForLensFacingUnchecked(LensFacing lensFacing) {
        try {
            return mCameraFactory.cameraIdForLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + lensFacing, e);
        }
    }

    /** A use case which attaches to a camera with various callbacks. */
    private static class CallbackAttachingImageCapture extends ImageCapture {
        private final SemaphoreReleasingCamera2Callbacks.SessionStateCallback
                mSessionStateCallback =
                new SemaphoreReleasingCamera2Callbacks.SessionStateCallback();
        private final SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);

        CallbackAttachingImageCapture(ImageCaptureConfig config, String cameraId) {
            super(config);
            // Use most supported resolution for different supported hardware level devices,
            // especially for legacy devices.
            mSurfaceTexture.setDefaultBufferSize(640, 480);
            SessionConfig.Builder builder = new SessionConfig.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(new Surface(mSurfaceTexture)));
            builder.addSessionStateCallback(mSessionStateCallback);

            attachToCamera(cameraId, builder.build());
        }

        @Override
        protected Map<String, Size> onSuggestedResolutionUpdated(
                Map<String, Size> suggestedResolutionMap) {
            return suggestedResolutionMap;
        }

        void doNotifyActive() {
            super.notifyActive();
        }
    }
}
