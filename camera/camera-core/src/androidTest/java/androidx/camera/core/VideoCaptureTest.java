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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Minimal unit test for the VideoCapture because the {@link android.media.MediaRecorder}
 * class requires a valid preview surface in order to correctly function.
 *
 * <p>TODO(b/112325215): The VideoCapture will be more thoroughly tested via integration
 * tests
 */
// TODO(b/142915639): VideoCapture does not properly implement UseCase.clear(), so it cannot be
//  properly shutdown. Remove once fixed.
@Suppress
@LargeTest
@RunWith(AndroidJUnit4.class)
public class VideoCaptureTest {
    @Rule
    public GrantPermissionRule mRuntimeCameraPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mRuntimeAudioPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.RECORD_AUDIO);

    private final Context mContext = InstrumentationRegistry.getTargetContext();
    private final CameraInternal mMockCameraInternal = mock(CameraInternal.class);
    private final ArgumentCaptor<UseCase> mUseCaseCaptor = ArgumentCaptor.forClass(UseCase.class);
    private final VideoCapture.OnVideoSavedCallback mMockVideoSavedCallback =
            mock(VideoCapture.OnVideoSavedCallback.class);
    private VideoCaptureConfig mDefaultConfig;
    private FakeLifecycleOwner mLifecycleOwner;
    private CameraSelector mCameraSelector;

    private final Instrumentation
            mInstrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        assumeTrue(CameraUtil.deviceHasCamera());
        mDefaultConfig = VideoCapture.DEFAULT_CONFIG.getConfig(null);

        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();

        mLifecycleOwner = new FakeLifecycleOwner();
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(
                CameraSelector.LENS_FACING_BACK).build();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    public void useCaseBecomesActive_whenStartingVideoRecording() {
        VideoCapture useCase = new VideoCapture(mDefaultConfig);
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
            mLifecycleOwner.startAndResume();
        });
        useCase.onAttach(mMockCameraInternal);

        useCase.startRecording(
                new File(
                        mContext.getFilesDir()
                                + "/useCaseBecomesActive_whenStartingVideoRecording.mp4"),
                CameraXExecutors.mainThreadExecutor(),
                mMockVideoSavedCallback);

        verify(mMockCameraInternal, times(1)).onUseCaseActive(mUseCaseCaptor.capture());
        assertThat(mUseCaseCaptor.getValue()).isSameInstanceAs(useCase);
    }

    @Test
    public void useCaseBecomesInactive_whenStoppingVideoRecording() {
        VideoCapture useCase = new VideoCapture(mDefaultConfig);
        mInstrumentation.runOnMainSync(() -> {
            CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCase);
            mLifecycleOwner.startAndResume();
        });
        useCase.onAttach(mMockCameraInternal);

        useCase.startRecording(
                new File(
                        mContext.getFilesDir()
                                + "/useCaseBecomesInactive_whenStoppingVideoRecording.mp4"),
                CameraXExecutors.mainThreadExecutor(),
                mMockVideoSavedCallback);

        try {
            useCase.stopRecording();
        } catch (RuntimeException e) {
            // Need to catch the RuntimeException because for certain devices MediaRecorder
            // contained
            // within the VideoCapture requires a valid preview in order to run. This unit
            // test is
            // just to exercise the inactive state change that the use case should trigger
            // TODO(b/112324530): The try-catch should be removed after the bug fix
        }

        verify(mMockCameraInternal, times(1)).onUseCaseInactive(mUseCaseCaptor.capture());
        assertThat(mUseCaseCaptor.getValue()).isSameInstanceAs(useCase);
    }
}
