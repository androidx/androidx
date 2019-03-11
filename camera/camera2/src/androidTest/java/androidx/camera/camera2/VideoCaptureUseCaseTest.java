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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.util.Size;

import androidx.camera.core.AppConfiguration;
import androidx.camera.core.BaseUseCase;
import androidx.camera.core.BaseUseCase.StateChangeListener;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.VideoCaptureUseCase;
import androidx.camera.core.VideoCaptureUseCase.OnVideoSavedListener;
import androidx.camera.core.VideoCaptureUseCaseConfiguration;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal unit test for the VideoCaptureUseCase because the {@link android.media.MediaRecorder}
 * class requires a valid preview surface in order to correctly function.
 *
 * <p>TODO(b/112325215): The VideoCaptureUseCase will be more thoroughly tested via integration
 * tests
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class VideoCaptureUseCaseTest {
    private static final Size DEFAULT_RESOLUTION = new Size(1920, 1080);

    private final Context mContext = InstrumentationRegistry.getTargetContext();
    private final StateChangeListener mListener = Mockito.mock(StateChangeListener.class);
    private final ArgumentCaptor<BaseUseCase> mBaseUseCaseCaptor =
            ArgumentCaptor.forClass(BaseUseCase.class);
    private final OnVideoSavedListener mMockVideoSavedListener =
            Mockito.mock(OnVideoSavedListener.class);
    private VideoCaptureUseCaseConfiguration mDefaultConfiguration;
    private String mCameraId;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.RECORD_AUDIO);

    @Before
    public void setUp() {
        mDefaultConfiguration = VideoCaptureUseCase.DEFAULT_CONFIG.getConfiguration(null);
        Context context = ApplicationProvider.getApplicationContext();
        AppConfiguration appConfiguration = Camera2AppConfiguration.create(context);
        CameraFactory cameraFactory = appConfiguration.getCameraFactory(/*valueIfMissing=*/ null);
        try {
            mCameraId = cameraFactory.cameraIdForLensFacing(LensFacing.BACK);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to attach to camera with LensFacing " + LensFacing.BACK, e);
        }
        CameraX.init(context, appConfiguration);
    }

    @Test
    public void useCaseBecomesActive_whenStartingVideoRecording() {
        VideoCaptureUseCase useCase = new VideoCaptureUseCase(mDefaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mListener);

        useCase.startRecording(
                new File(
                        mContext.getFilesDir()
                                + "/useCaseBecomesActive_whenStartingVideoRecording.mp4"),
                mMockVideoSavedListener);

        verify(mListener, times(1)).onUseCaseActive(mBaseUseCaseCaptor.capture());
        assertThat(mBaseUseCaseCaptor.getValue()).isSameAs(useCase);
    }

    @Test
    public void useCaseBecomesInactive_whenStoppingVideoRecording() {
        VideoCaptureUseCase useCase = new VideoCaptureUseCase(mDefaultConfiguration);
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mListener);

        useCase.startRecording(
                new File(
                        mContext.getFilesDir()
                                + "/useCaseBecomesInactive_whenStoppingVideoRecording.mp4"),
                mMockVideoSavedListener);

        try {
            useCase.stopRecording();
        } catch (RuntimeException e) {
            // Need to catch the RuntimeException because for certain devices MediaRecorder
            // contained
            // within the VideoCaptureUseCase requires a valid preview in order to run. This unit
            // test is
            // just to exercise the inactive state change that the use case should trigger
            // TODO(b/112324530): The try-catch should be removed after the bug fix
        }

        verify(mListener, times(1)).onUseCaseInactive(mBaseUseCaseCaptor.capture());
        assertThat(mBaseUseCaseCaptor.getValue()).isSameAs(useCase);
    }

    @Test
    public void updateSessionConfigurationWithSuggestedResolution() {
        VideoCaptureUseCase useCase = new VideoCaptureUseCase(mDefaultConfiguration);
        // Create video encoder with default 1920x1080 resolution
        Map<String, Size> suggestedResolutionMap = new HashMap<>();
        suggestedResolutionMap.put(mCameraId, DEFAULT_RESOLUTION);
        useCase.updateSuggestedResolution(suggestedResolutionMap);
        useCase.addStateChangeListener(mListener);

        // Recreate video encoder with new 640x480 resolution
        suggestedResolutionMap.put(mCameraId, new Size(640, 480));
        useCase.updateSuggestedResolution(suggestedResolutionMap);

        // Check it could be started to record and become active
        useCase.startRecording(
                new File(
                        mContext.getFilesDir()
                                + "/useCaseBecomesInactive_whenStoppingVideoRecording.mp4"),
                mMockVideoSavedListener);

        verify(mListener, times(1)).onUseCaseActive(mBaseUseCaseCaptor.capture());
        assertThat(mBaseUseCaseCaptor.getValue()).isSameAs(useCase);
    }
}
