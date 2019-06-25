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

package androidx.camera.extensions;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageInfoProcessor;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PreviewExtenderTest {

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreInvokeBeforeAndAfterInitDeInit() {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);

        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter =
                new PreviewExtender.PreviewExtenderAdapter(mockPreviewExtenderImpl);
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setUseCaseEventListener(
                previewExtenderAdapter).setLensFacing(CameraX.LensFacing.BACK);
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));

        Preview useCase = new Preview(configBuilder.build());

        CameraX.bindToLifecycle(lifecycle, useCase);
        lifecycle.startAndResume();

        // To set the update listener and Preview will change to active state.
        useCase.setOnPreviewOutputUpdateListener(mock(Preview.OnPreviewOutputUpdateListener.class));

        // To verify the call in order after bind to life cycle, and to verification of the
        // getCaptureStages() is also used to wait for the capture session created. The test for
        // the unbind would come after the capture session was created.
        InOrder inOrder = inOrder(mockPreviewExtenderImpl);
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onPresetSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onEnableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).getCaptureStage();

        // Unbind the use case to test the onDisableSession and onDeInit.
        CameraX.unbind(useCase);

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDisableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockPreviewExtenderImpl);
    }

    @Test
    @MediumTest
    public void getCaptureStagesTest_shouldSetToRepeatingRequest() {
        FakeLifecycleOwner lifecycle = new FakeLifecycleOwner();

        ImageInfoProcessor mockImageInfoProcessor = mock(ImageInfoProcessor.class);

        // Set up a result for getCaptureStages() testing.
        List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
        parameters.add(Pair.create(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD));
        CaptureStageImpl mockCaptureStageImpl = mock(CaptureStageImpl.class);
        when(mockCaptureStageImpl.getParameters()).thenReturn(parameters);

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.getCaptureStage()).thenReturn(mockCaptureStageImpl);

        PreviewExtender.PreviewExtenderAdapter previewExtenderAdapter =
                new PreviewExtender.PreviewExtenderAdapter(mockPreviewExtenderImpl);
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setUseCaseEventListener(
                previewExtenderAdapter).setLensFacing(CameraX.LensFacing.BACK);
        new Camera2Config.Extender(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        configBuilder.setImageInfoProcessor(mockImageInfoProcessor);

        Preview useCase = new Preview(configBuilder.build());

        CameraX.bindToLifecycle(lifecycle, useCase);
        lifecycle.startAndResume();

        // To set the update listener and Preview will change to active state.
        useCase.setOnPreviewOutputUpdateListener(mock(Preview.OnPreviewOutputUpdateListener.class));

        ArgumentCaptor<ImageInfo> imageInfo = ArgumentCaptor.forClass(ImageInfo.class);
        verify(mockImageInfoProcessor, timeout(3000)).process(imageInfo.capture());
        CameraCaptureResult result = CameraCaptureResults.retrieveCameraCaptureResult(
                imageInfo.getValue());
        assertNotNull(result);

        CaptureResult captureResult = Camera2CameraCaptureResultConverter.getCaptureResult(result);
        assertNotNull(captureResult);

        // To verify the capture result should include the parameter of the getCaptureStages().
        assertThat(captureResult.getRequest().get(CaptureRequest.CONTROL_EFFECT_MODE)).isEqualTo(
                CaptureRequest.CONTROL_EFFECT_MODE_BLACKBOARD);

        CameraX.unbind(useCase);
        verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit();
    }

}
