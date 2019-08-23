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

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;

import androidx.camera.camera2.Camera2AppConfig;
import androidx.camera.core.CameraDeviceConfig;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.BeautyPreviewExtenderImpl;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class PreviewExtenderTest {

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FakeLifecycleOwner mFakeLifecycle;

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraX.LensFacing.BACK));

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.init(context, Camera2AppConfig.create(context));

        mFakeLifecycle = new FakeLifecycleOwner();
        mFakeLifecycle.startAndResume();
    }

    @After
    public void cleanUp() throws ExecutionException, InterruptedException {
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.unbindAll();
            }
        });

        // Wait for CameraX to finish deinitializing before the next test.
        CameraX.deinit().get();
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreInvokeBeforeAndAfterInitDeInit() {
        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR);
        when(mockPreviewExtenderImpl.getProcessor()).thenReturn(
                mock(PreviewImageProcessorImpl.class));
        when(mockPreviewExtenderImpl.isExtensionAvailable(any(String.class),
                any(CameraCharacteristics.class))).thenReturn(true);

        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setLensFacing(
                CameraX.LensFacing.BACK);

        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        fakePreviewExtender.enableExtension();

        Preview useCase = new Preview(configBuilder.build());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mFakeLifecycle, useCase);

                // To set the update listener and Preview will change to active state.
                useCase.setOnPreviewOutputUpdateListener(
                        mock(Preview.OnPreviewOutputUpdateListener.class));
            }
        });

        // To verify the call in order after bind to life cycle, and to verification of the
        // getCaptureStages() is also used to wait for the capture session created. The test for
        // the unbind would come after the capture session was created. Ignore any of the calls
        // unrelated to the ExtenderStateListener.
        verify(mockPreviewExtenderImpl, timeout(3000)).init(any(String.class),
                any(CameraCharacteristics.class));
        verify(mockPreviewExtenderImpl, timeout(3000)).getProcessorType();
        verify(mockPreviewExtenderImpl, timeout(3000)).getProcessor();
        verify(mockPreviewExtenderImpl, timeout(3000)).getSupportedResolutions();

        InOrder inOrder = inOrder(ignoreStubs(mockPreviewExtenderImpl));

        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onPresetSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onEnableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).getCaptureStage();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Unbind the use case to test the onDisableSession and onDeInit.
                CameraX.unbind(useCase);
            }
        });

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDisableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockPreviewExtenderImpl);
    }

    @Test
    @MediumTest
    public void getCaptureStagesTest_shouldSetToRepeatingRequest() {
        // Set up a result for getCaptureStages() testing.
        CaptureStageImpl fakeCaptureStageImpl = new FakeCaptureStageImpl();

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        RequestUpdateProcessorImpl mockRequestUpdateProcessorImpl = mock(
                RequestUpdateProcessorImpl.class);

        // The mock an RequestUpdateProcessorImpl to capture the returned TotalCaptureResult
        when(mockPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_REQUEST_UPDATE_ONLY);
        when(mockPreviewExtenderImpl.getProcessor()).thenReturn(mockRequestUpdateProcessorImpl);
        when(mockPreviewExtenderImpl.isExtensionAvailable(any(String.class),
                any(CameraCharacteristics.class))).thenReturn(true);

        when(mockPreviewExtenderImpl.getCaptureStage()).thenReturn(fakeCaptureStageImpl);

        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setLensFacing(
                CameraX.LensFacing.BACK);

        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        fakePreviewExtender.enableExtension();

        Preview preview = new Preview(configBuilder.build());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mFakeLifecycle, preview);

                // To set the update listener and Preview will change to active state.
                preview.setOnPreviewOutputUpdateListener(
                        mock(Preview.OnPreviewOutputUpdateListener.class));
            }
        });

        ArgumentCaptor<TotalCaptureResult> captureResultArgumentCaptor = ArgumentCaptor.forClass(
                TotalCaptureResult.class);

        verify(mockRequestUpdateProcessorImpl, timeout(3000).atLeastOnce()).process(
                captureResultArgumentCaptor.capture());

        // TotalCaptureResult might be captured multiple times. Only care to get one instance of
        // it, since they should all have the same value for the tested key
        TotalCaptureResult totalCaptureResult = captureResultArgumentCaptor.getValue();

        // To verify the capture result should include the parameter of the getCaptureStages().
        List<Pair<CaptureRequest.Key, Object>> parameters = fakeCaptureStageImpl.getParameters();
        for (Pair<CaptureRequest.Key, Object> parameter : parameters) {
            assertThat(totalCaptureResult.getRequest().get(
                    (CaptureRequest.Key<Object>) parameter.first).equals(
                    parameter.second));
        }
    }

    @Test
    @MediumTest
    public void processShouldBeInvoked_typeImageProcessor() {
        // The type image processor will invoke PreviewImageProcessor.process()
        PreviewImageProcessorImpl mockPreviewImageProcessorImpl = mock(
                PreviewImageProcessorImpl.class);

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.getProcessor()).thenReturn(mockPreviewImageProcessorImpl);
        when(mockPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR);
        when(mockPreviewExtenderImpl.isExtensionAvailable(any(String.class),
                any(CameraCharacteristics.class))).thenReturn(true);

        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setLensFacing(
                CameraX.LensFacing.BACK);
        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        fakePreviewExtender.enableExtension();
        Preview preview = new Preview(configBuilder.build());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                CameraX.bindToLifecycle(mFakeLifecycle, preview);
                // To set the update listener and Preview will change to active state.
                preview.setOnPreviewOutputUpdateListener(
                        mock(Preview.OnPreviewOutputUpdateListener.class));
            }
        });

        // To verify the process() method was invoked with non-null TotalCaptureResult input.
        verify(mockPreviewImageProcessorImpl, timeout(3000).atLeastOnce()).process(any(Image.class),
                any(TotalCaptureResult.class));
    }

    @Test
    @SmallTest
    public void canSetSupportedResolutionsToConfigTest()
            throws CameraInfoUnavailableException, CameraAccessException {
        CameraX.LensFacing lensFacing = CameraX.LensFacing.BACK;
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing));
        PreviewConfig.Builder configBuilder = new PreviewConfig.Builder().setLensFacing(lensFacing);

        String cameraId = androidx.camera.extensions.CameraUtil.getCameraId(
                ((CameraDeviceConfig) configBuilder.build()));
        CameraCharacteristics cameraCharacteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(
                        CameraX.getCameraWithLensFacing(lensFacing));

        // Only BeautyPreviewExtenderImpl has sample implementation for Preview.
        // Retrieves the target format/resolutions pair list directly from
        // BeautyPreviewExtenderImpl.
        BeautyPreviewExtenderImpl impl = new BeautyPreviewExtenderImpl();

        impl.init(cameraId, cameraCharacteristics);
        List<Pair<Integer, Size[]>> targetFormatResolutionsPairList =
                impl.getSupportedResolutions();

        assertThat(targetFormatResolutionsPairList).isNotNull();

        // Retrieves the target format/resolutions pair list from builder after applying beauty
        // mode.
        BeautyPreviewExtender extender = BeautyPreviewExtender.create(configBuilder);
        assertThat(configBuilder.build().getSupportedResolutions(null)).isNull();
        extender.enableExtension();

        List<Pair<Integer, Size[]>> resultFormatResolutionsPairList =
                configBuilder.build().getSupportedResolutions(null);

        assertThat(resultFormatResolutionsPairList).isNotNull();

        // Checks the result and target pair lists are the same
        for (Pair<Integer, Size[]> resultPair : resultFormatResolutionsPairList) {
            Size[] targetSizes = null;
            for (Pair<Integer, Size[]> targetPair : targetFormatResolutionsPairList) {
                if (targetPair.first.equals(resultPair.first)) {
                    targetSizes = targetPair.second;
                    break;
                }
            }

            assertThat(Arrays.asList(resultPair.second).equals(
                    Arrays.asList(targetSizes))).isTrue();
        }
    }

    private class FakePreviewExtender extends PreviewExtender {
        FakePreviewExtender(PreviewConfig.Builder builder, PreviewExtenderImpl impl) {
            init(builder, impl, ExtensionsManager.EffectMode.NORMAL);
        }
    }

    private class FakeCaptureStageImpl implements CaptureStageImpl {
        @Override
        public int getId() {
            return 0;
        }

        @Override
        public List<Pair<CaptureRequest.Key, Object>> getParameters() {
            List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
            parameters.add(Pair.create(CaptureRequest.CONTROL_EFFECT_MODE,
                    CaptureRequest.CONTROL_EFFECT_MODE_SEPIA));

            return parameters;
        }
    }
}
