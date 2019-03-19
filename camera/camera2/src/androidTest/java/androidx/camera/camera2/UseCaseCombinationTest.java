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

import android.Manifest;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraRepository;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysisUseCase;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImmediateSurface;
import androidx.camera.core.SessionConfiguration;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
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
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseCombinationTest {
    private static final String TAG = "UseCaseCombinationTest";
    private static final Size DEFAULT_RESOLUTION = new Size(1920, 1080);
    private static final LensFacing DEFAULT_LENS_FACING = LensFacing.BACK;
    private final MutableLiveData<Long> mAnalysisResult = new MutableLiveData<>();
    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
    private Semaphore mSemaphore;
    private FakeLifecycleOwner mLifecycle;
    private HandlerThread mHandlerThread;
    private Handler mMainThreadHandler;
    private CallbackAttachingImageCaptureUseCase mImageCaptureUseCase;
    private ImageAnalysisUseCase mImageAnalysisUseCase;
    private ViewFinderUseCase mViewFinderUseCase;
    private ImageAnalysisUseCase.Analyzer mImageAnalyzer;
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
        Context context = ApplicationProvider.getApplicationContext();
        CameraX.init(context, Camera2AppConfiguration.create(context));
        mLifecycle = new FakeLifecycleOwner();
        mHandlerThread = new HandlerThread("ErrorHandlerThread");
        mHandlerThread.start();
        mMainThreadHandler = new Handler(Looper.getMainLooper());

        mSemaphore = new Semaphore(0);
    }

    @After
    public void tearDown() throws InterruptedException {
        CameraX.unbindAll();
        mHandlerThread.quitSafely();

        // Wait some time for the cameras to close. We need the cameras to close to bring CameraX
        // back to the initial state.
        Thread.sleep(3000);
    }

    /**
     * Test Combination: ViewFinder + ImageCaptureUseCase
     */
    @Test
    public void viewFinderCombinesImageCapture() throws InterruptedException {
        initViewFinderUseCase();
        initImageCaptureUseCase();

        mUseCaseGroup.addUseCase(mImageCaptureUseCase);
        mUseCaseGroup.addUseCase(mViewFinderUseCase);

        CameraX.bindToLifecycle(mLifecycle, mViewFinderUseCase, mImageCaptureUseCase);
        mLifecycle.startAndResume();

        mImageCaptureUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        // Wait for the CameraCaptureSession.onConfigured callback.
        mImageCaptureUseCase.mSessionStateCallback.waitForOnConfigured(1);

        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
        assertThat(CameraX.isBound(mViewFinderUseCase)).isTrue();
        assertThat(CameraX.isBound(mImageCaptureUseCase)).isTrue();
    }

    /**
     * Test Combination: ViewFinder + ImageAnalysisUseCase
     */
    @Test
    public void viewFinderCombinesImageAnalysis() throws InterruptedException {
        initImageAnalysisUseCase();
        initViewFinderUseCase();

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageAnalysisUseCase.setAnalyzer(mImageAnalyzer);

                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());

                CameraX.bindToLifecycle(mLifecycle, mViewFinderUseCase, mImageAnalysisUseCase);
                mLifecycle.startAndResume();
            }
        });

        // Wait for 10 frames to be analyzed.
        mSemaphore.acquire(10);

        assertThat(CameraX.isBound(mViewFinderUseCase)).isTrue();
        assertThat(CameraX.isBound(mImageAnalysisUseCase)).isTrue();
    }

    /**
     * Test Combination: ViewFinder + ImageAnalysis + ImageCaptureUseCase
     */
    @Test
    public void viewFinderCombinesImageAnalysisAndImageCapture() throws InterruptedException {
        initViewFinderUseCase();
        initImageAnalysisUseCase();
        initImageCaptureUseCase();

        mUseCaseGroup.addUseCase(mImageCaptureUseCase);
        mUseCaseGroup.addUseCase(mImageAnalysisUseCase);
        mUseCaseGroup.addUseCase(mViewFinderUseCase);

        mImageCaptureUseCase.doNotifyActive();
        mCameraRepository.onGroupActive(mUseCaseGroup);

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageAnalysisUseCase.setAnalyzer(mImageAnalyzer);

                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());
            }
        });

        CameraX.bindToLifecycle(mLifecycle, mViewFinderUseCase, mImageAnalysisUseCase,
                mImageCaptureUseCase);
        mLifecycle.startAndResume();

        // Wait for 10 frames to be analyzed.
        mSemaphore.acquire(10);

        // Wait for the CameraCaptureSession.onConfigured callback.
        try {
            mImageCaptureUseCase.mSessionStateCallback.waitForOnConfigured(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(CameraX.isBound(mViewFinderUseCase)).isTrue();
        assertThat(CameraX.isBound(mImageAnalysisUseCase)).isTrue();
        assertThat(CameraX.isBound(mImageCaptureUseCase)).isTrue();
    }

    private void initImageAnalysisUseCase() {
        ImageAnalysisUseCaseConfiguration imageAnalysisUseCaseConfiguration =
                new ImageAnalysisUseCaseConfiguration.Builder()
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .setTargetName("ImageAnalysis")
                        .setCallbackHandler(new Handler(Looper.getMainLooper()))
                        .build();
        mImageAnalyzer =
                new ImageAnalysisUseCase.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        mAnalysisResult.postValue(image.getTimestamp());
                    }
                };
        mImageAnalysisUseCase = new ImageAnalysisUseCase(imageAnalysisUseCaseConfiguration);
    }

    private void initImageCaptureUseCase() {
        mCameraRepository = new CameraRepository();
        mCameraFactory = new Camera2CameraFactory(ApplicationProvider.getApplicationContext());
        mCameraRepository.init(mCameraFactory);
        mUseCaseGroup = new UseCaseGroup();

        ImageCaptureUseCaseConfiguration imageCaptureUseCaseConfiguration =
                new ImageCaptureUseCaseConfiguration.Builder().setLensFacing(
                        LensFacing.BACK).build();
        String cameraId = getCameraIdForLensFacingUnchecked(
                imageCaptureUseCaseConfiguration.getLensFacing());
        mImageCaptureUseCase = new CallbackAttachingImageCaptureUseCase(
                imageCaptureUseCaseConfiguration, cameraId);

        mImageCaptureUseCase.addStateChangeListener(
                mCameraRepository.getCamera(
                        getCameraIdForLensFacingUnchecked(
                                imageCaptureUseCaseConfiguration.getLensFacing())));
    }

    private void initViewFinderUseCase() {
        ViewFinderUseCaseConfiguration viewFinderUseCaseConfiguration =
                new ViewFinderUseCaseConfiguration.Builder()
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .setTargetName("ViewFinder")
                        .build();

        mViewFinderUseCase = new ViewFinderUseCase(viewFinderUseCaseConfiguration);
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
    private static class CallbackAttachingImageCaptureUseCase extends ImageCaptureUseCase {
        private final SemaphoreReleasingCamera2Callbacks.SessionStateCallback
                mSessionStateCallback =
                new SemaphoreReleasingCamera2Callbacks.SessionStateCallback();
        private final SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);

        CallbackAttachingImageCaptureUseCase(ImageCaptureUseCaseConfiguration configuration,
                String cameraId) {
            super(configuration);

            SessionConfiguration.Builder builder = new SessionConfiguration.Builder();
            builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
            builder.addSurface(new ImmediateSurface(new Surface(mSurfaceTexture)));
            builder.setSessionStateCallback(mSessionStateCallback);

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
