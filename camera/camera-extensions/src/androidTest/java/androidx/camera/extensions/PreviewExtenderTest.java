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

import static androidx.camera.testing.SurfaceTextureProvider.createSurfaceTextureProvider;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraRepository;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.extensions.impl.RequestUpdateProcessorImpl;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.testing.CameraAvailabilityUtil;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class PreviewExtenderTest {
    private static final String EXTENSION_AVAILABLE_CAMERA_ID = "0";
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CameraRepository mCameraRepository;

    private static final SurfaceTextureProvider.SurfaceTextureCallback
            NO_OP_SURFACE_TEXTURE_CALLBACK =
            new SurfaceTextureProvider.SurfaceTextureCallback() {
                @Override
                public void onSurfaceTextureReady(
                        @NonNull SurfaceTexture surfaceTexture,
                        @NonNull Size resolution) {
                    // No-op.
                }

                @Override
                public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                    // No-op.
                }
            };

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        assumeTrue(CameraUtil.deviceHasCamera());
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        CameraX.initialize(mContext, Camera2Config.defaultConfig()).get();
        CameraX cameraX = CameraX.getOrCreateInstance(mContext).get();
        mCameraRepository = cameraX.getCameraRepository();

        assumeTrue(ExtensionsTestUtil.initExtensions(mContext));
    }

    @After
    public void cleanUp() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
        ExtensionsManager.deinit().get();
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

        Preview.Builder configBuilder = new Preview.Builder();

        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        fakePreviewExtender.enableExtension(cameraSelector);

        Preview useCase = configBuilder.build();

        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector);
        mInstrumentation.runOnMainSync(() -> {
            // To set the update listener and Preview will change to active state.
            useCase.setSurfaceProvider(
                    createSurfaceTextureProvider(NO_OP_SURFACE_TEXTURE_CALLBACK));

            try {
                cameraUseCaseAdapter.addUseCases(Collections.singleton(useCase));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to bind use case " + useCase);
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

        // getSupportedResolutions supported since version 1.1
        if (ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0) {
            verify(mockPreviewExtenderImpl, timeout(3000)).getSupportedResolutions();
        }

        InOrder inOrder = inOrder(ignoreStubs(mockPreviewExtenderImpl));

        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onPresetSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onEnableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).getCaptureStage();

        mInstrumentation.runOnMainSync(() -> {
            // Unbind the use case to test the onDisableSession and onDeInit.
            cameraUseCaseAdapter.removeUseCases(Collections.singleton(useCase));
        });

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDisableSession();
        inOrder.verify(mockPreviewExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockPreviewExtenderImpl);
    }

    @SuppressWarnings("unchecked")
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

        Preview.Builder configBuilder = new Preview.Builder();

        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        fakePreviewExtender.enableExtension(cameraSelector);

        Preview preview = configBuilder.build();
        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector);
        mInstrumentation.runOnMainSync(() -> {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                    createSurfaceTextureProvider(NO_OP_SURFACE_TEXTURE_CALLBACK));

            try {
                cameraUseCaseAdapter.addUseCases(Collections.singleton(preview));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to bind use case " + preview);
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

        Preview.Builder configBuilder = new Preview.Builder();
        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        fakePreviewExtender.enableExtension(cameraSelector);
        Preview preview = configBuilder.build();

        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector);
        mInstrumentation.runOnMainSync(() -> {
            // To set the update listener and Preview will change to active state.
            preview.setSurfaceProvider(
                    createSurfaceTextureProvider(NO_OP_SURFACE_TEXTURE_CALLBACK));

            try {
                cameraUseCaseAdapter.addUseCases(Collections.singleton(preview));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to bind use case " + preview);
            }
        });

        // To verify the process() method was invoked with non-null TotalCaptureResult input.
        verify(mockPreviewImageProcessorImpl, timeout(3000).atLeastOnce()).process(any(Image.class),
                any(TotalCaptureResult.class));
    }

    @Test
    @MediumTest
    public void canSetSupportedResolutionsToConfigTest() throws CameraInfoUnavailableException {
        assumeTrue(CameraUtil.deviceHasCamera());
        // getSupportedResolutions supported since version 1.1
        assumeTrue(ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0);

        @CameraSelector.LensFacing int lensFacing =
                CameraAvailabilityUtil.getDefaultLensFacing(mCameraRepository);
        Preview.Builder configBuilder = new Preview.Builder();

        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.isExtensionAvailable(any(), any())).thenReturn(true);
        when(mockPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_NONE);

        List<Pair<Integer, Size[]>> targetFormatResolutionsPairList =
                generatePreviewSupportedResolutions(lensFacing);
        when(mockPreviewExtenderImpl.getSupportedResolutions()).thenReturn(
                targetFormatResolutionsPairList);

        PreviewExtender fakeExtender = new FakePreviewExtender(configBuilder,
                mockPreviewExtenderImpl);

        // Checks the config does not include supported resolutions before applying effect mode.
        assertThat(configBuilder.getUseCaseConfig().getSupportedResolutions(null)).isNull();

        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        // Checks the config includes supported resolutions after applying effect mode.
        fakeExtender.enableExtension(cameraSelector);
        List<Pair<Integer, Size[]>> resultFormatResolutionsPairList =
                configBuilder.getUseCaseConfig().getSupportedResolutions(null);
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

            assertThat(
                    Arrays.asList(resultPair.second).equals(Arrays.asList(targetSizes))).isTrue();
        }
    }

    @Test
    @MediumTest
    public void canAddCameraIdFilterToConfigBuilder() {
        PreviewExtenderImpl mockPreviewExtenderImpl = mock(PreviewExtenderImpl.class);
        when(mockPreviewExtenderImpl.getProcessorType()).thenReturn(
                PreviewExtenderImpl.ProcessorType.PROCESSOR_TYPE_IMAGE_PROCESSOR);
        when(mockPreviewExtenderImpl.isExtensionAvailable(eq(EXTENSION_AVAILABLE_CAMERA_ID),
                any())).thenReturn(true);
        when(mockPreviewExtenderImpl.isExtensionAvailable(
                not(eq(EXTENSION_AVAILABLE_CAMERA_ID)),
                any())).thenReturn(false);
        Preview.Builder previewBuilder = new Preview.Builder();

        FakePreviewExtender fakePreviewExtender = new FakePreviewExtender(previewBuilder,
                mockPreviewExtenderImpl);
        fakePreviewExtender.enableExtension(CameraSelector.DEFAULT_BACK_CAMERA);

        CameraSelector cameraSelector = previewBuilder.getUseCaseConfig().getCameraSelector(null);
        assertThat(cameraSelector).isNotNull();
        assertThat(CameraX.getCameraWithCameraSelector(
                cameraSelector).getCameraInfoInternal().getCameraId()).isEqualTo(
                EXTENSION_AVAILABLE_CAMERA_ID);
    }

    private List<Pair<Integer, Size[]>> generatePreviewSupportedResolutions(
            @CameraSelector.LensFacing int lensFacing) throws CameraInfoUnavailableException {
        List<Pair<Integer, Size[]>> formatResolutionsPairList = new ArrayList<>();
        String cameraId = androidx.camera.extensions.CameraUtil.getCameraIdWithLensFacingUnchecked(
                lensFacing);

        StreamConfigurationMap map =
                androidx.camera.extensions.CameraUtil.getCameraCharacteristics(cameraId).get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map != null) {
            // Retrieves originally supported resolutions from CameraCharacteristics for PRIVATE
            // format to return.
            Size[] outputSizes = map.getOutputSizes(ImageFormat.PRIVATE);

            if (outputSizes != null) {
                formatResolutionsPairList.add(Pair.create(ImageFormat.PRIVATE, outputSizes));
            }
        }

        return formatResolutionsPairList;
    }

    private class FakePreviewExtender extends PreviewExtender {
        FakePreviewExtender(Preview.Builder builder, PreviewExtenderImpl impl) {
            init(builder, impl, ExtensionMode.NONE);
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
