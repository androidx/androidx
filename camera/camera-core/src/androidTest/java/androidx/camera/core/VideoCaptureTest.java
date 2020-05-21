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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.provider.MediaStore;

import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeAppConfig;
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private CameraUseCaseAdapter mCameraUseCaseAdapter;
    private CameraX mCameraX;

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        assumeTrue(CameraUtil.deviceHasCamera());
        mDefaultConfig = VideoCapture.DEFAULT_CONFIG.getConfig(null);

        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).build();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, cameraXConfig).get();
        mCameraX = CameraX.getOrCreateInstance(context).get();

        mCameraUseCaseAdapter =
                new CameraUseCaseAdapter(mMockCameraInternal,
                        new LinkedHashSet<>(Collections.singleton(mMockCameraInternal)),
                        mCameraX.getCameraDeviceSurfaceManager());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void useCaseBecomesActive_whenStartingVideoRecording() {
        VideoCapture useCase = VideoCapture.Builder.fromConfig(mDefaultConfig).build();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(useCase));
                mCameraUseCaseAdapter.attachUseCases();
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });


        useCase.startRecording(
                getVideoOutputOptions(),
                CameraXExecutors.mainThreadExecutor(),
                mMockVideoSavedCallback);

        verify(mMockCameraInternal, times(1)).onUseCaseActive(mUseCaseCaptor.capture());
        assertThat(mUseCaseCaptor.getValue()).isSameInstanceAs(useCase);
    }

    @Test
    public void useCaseBecomesInactive_whenStoppingVideoRecording() {
        VideoCapture useCase = new VideoCapture(mDefaultConfig);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                mCameraUseCaseAdapter.addUseCases(Collections.singleton(useCase));
                mCameraUseCaseAdapter.attachUseCases();
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException(e);
            }
        });

        useCase.startRecording(
                getVideoOutputOptions(),
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

    private VideoCapture.OutputFileOptions getVideoOutputOptions() {
        String videoFileName = "video_" + System.currentTimeMillis();
        ContentResolver resolver = mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName);
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
        VideoCapture.OutputFileOptions output = new VideoCapture.OutputFileOptions.Builder(resolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues).build();
        return output;
    }
}
