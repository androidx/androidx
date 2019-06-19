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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
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
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;
    private Preview mPreview;
    private ImageAnalysis.Analyzer mImageAnalyzer;

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
        AppConfig config = Camera2AppConfig.create(context);

        CameraX.init(context, config);

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
    public void previewCombinesImageCapture() {
        initPreview();
        initImageCapture();

        CameraX.bindToLifecycle(mLifecycle, mPreview, mImageCapture);
        mLifecycle.startAndResume();

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
                CameraX.bindToLifecycle(mLifecycle, mPreview, mImageAnalysis);
                mImageAnalysis.setAnalyzer(mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());
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

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mLifecycle, mPreview, mImageAnalysis, mImageCapture);
                mImageAnalysis.setAnalyzer(mImageAnalyzer);
                mAnalysisResult.observe(mLifecycle,
                        createCountIncrementingObserver());
                mLifecycle.startAndResume();
            }
        });

        // Wait for 10 frames to be analyzed.
        mSemaphore.acquire(10);

        assertThat(mLifecycle.getObserverCount()).isEqualTo(3);
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
        ImageCaptureConfig imageCaptureConfig =
                new ImageCaptureConfig.Builder().setLensFacing(LensFacing.BACK).build();

        mImageCapture = new ImageCapture(imageCaptureConfig);
    }

    private void initPreview() {
        PreviewConfig previewConfig =
                new PreviewConfig.Builder()
                        .setLensFacing(DEFAULT_LENS_FACING)
                        .setTargetName("Preview")
                        .build();

        mPreview = new Preview(previewConfig);
    }
}
