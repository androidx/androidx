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

import static androidx.camera.extensions.util.ExtensionsTestUtil.assumeCompatibleDevice;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;
import android.util.Size;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CameraRepository;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl;
import androidx.camera.extensions.internal.ExtensionVersion;
import androidx.camera.extensions.internal.Version;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.testing.CameraAvailabilityUtil;
import androidx.camera.testing.CameraUtil;
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
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class ImageCaptureExtenderTest {
    private static final String EXTENSION_AVAILABLE_CAMERA_ID = "0";

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CameraRepository mCameraRepository;

    @Before
    public void setUp() throws InterruptedException, ExecutionException, TimeoutException {
        assumeCompatibleDevice();
        assumeTrue(CameraUtil.deviceHasCamera());

        CameraX.initialize(mContext, Camera2Config.defaultConfig()).get();
        CameraX cameraX = CameraX.getOrCreateInstance(mContext).get();
        mCameraRepository = cameraX.getCameraRepository();

        assumeTrue(ExtensionsTestUtil.initExtensions(mContext));
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
        ExtensionsManager.deinit().get();
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreGetCaptureStagesBeforeAndAfterInitDeInit() {
        ImageCaptureExtenderImpl mockImageCaptureExtenderImpl = mock(
                ImageCaptureExtenderImpl.class);
        ArrayList<CaptureStageImpl> captureStages = new ArrayList<>();

        captureStages.add(new FakeCaptureStage());
        when(mockImageCaptureExtenderImpl.getCaptureStages()).thenReturn(captureStages);

        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mockImageCaptureExtenderImpl,
                        mContext);
        ImageCapture.Builder builder =
                new ImageCapture.Builder().setCaptureBundle(
                        imageCaptureAdapter).setUseCaseEventCallback(
                        imageCaptureAdapter).setCaptureProcessor(
                        mock(CaptureProcessor.class));

        ImageCapture useCase = builder.build();

        @CameraSelector.LensFacing int lensFacing =
                CameraAvailabilityUtil.getDefaultLensFacing(mCameraRepository);
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing).build();
        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector);
        mInstrumentation.runOnMainSync(() -> {
            try {
                cameraUseCaseAdapter.addUseCases(Collections.singleton(useCase));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to attach use case");
            }
        });

        // To verify the event callbacks in order, and to verification of the getCaptureStages()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mockImageCaptureExtenderImpl);
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class), any(
                CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).getCaptureStages();

        mInstrumentation.runOnMainSync(() -> {
            // Unbind the use case to test the onDeInit.
            cameraUseCaseAdapter.removeUseCases(Collections.singleton(useCase));
        });

        // To verify the deInit should been called.
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockImageCaptureExtenderImpl);
    }

    @Test
    @MediumTest
    public void extenderLifeCycleTest_noMoreCameraEventCallbacksBeforeAndAfterInitDeInit() {
        ImageCaptureExtenderImpl mockImageCaptureExtenderImpl = mock(
                ImageCaptureExtenderImpl.class);
        ArrayList<CaptureStageImpl> captureStages = new ArrayList<>();

        captureStages.add(new FakeCaptureStage());
        when(mockImageCaptureExtenderImpl.getCaptureStages()).thenReturn(captureStages);

        ImageCaptureExtender.ImageCaptureAdapter imageCaptureAdapter =
                new ImageCaptureExtender.ImageCaptureAdapter(mockImageCaptureExtenderImpl,
                        mContext);
        ImageCapture.Builder configBuilder = new ImageCapture.Builder().setCaptureBundle(
                imageCaptureAdapter).setUseCaseEventCallback(
                imageCaptureAdapter).setCaptureProcessor(
                mock(CaptureProcessor.class));
        new Camera2ImplConfig.Extender<>(configBuilder).setCameraEventCallback(
                new CameraEventCallbacks(imageCaptureAdapter));

        ImageCapture useCase = configBuilder.build();

        @CameraSelector.LensFacing int lensFacing =
                CameraAvailabilityUtil.getDefaultLensFacing(mCameraRepository);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing).build();
        CameraUseCaseAdapter cameraUseCaseAdapter =
                CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector);
        mInstrumentation.runOnMainSync(() -> {
            try {
                cameraUseCaseAdapter.addUseCases(Collections.singleton(useCase));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to attach use case");
            }
        });

        // To verify the event callbacks in order, and to verification of the onEnableSession()
        // is also used to wait for the capture session created. The test for the unbind
        // would come after the capture session was created.
        InOrder inOrder = inOrder(mockImageCaptureExtenderImpl);
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onInit(any(String.class),
                any(CameraCharacteristics.class), any(Context.class));
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce()).onPresetSession();
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000).atLeastOnce()).onEnableSession();

        mInstrumentation.runOnMainSync(() -> {
            // Unbind the use case to test the onDisableSession and onDeInit.
            cameraUseCaseAdapter.removeUseCases(Collections.singleton(useCase));
        });

        // To verify the onDisableSession and onDeInit.
        inOrder.verify(mockImageCaptureExtenderImpl,
                timeout(3000).atLeastOnce()).onDisableSession();
        inOrder.verify(mockImageCaptureExtenderImpl, timeout(3000)).onDeInit();

        // This test item only focus on onPreset, onEnable and onDisable callback testing,
        // ignore all the getCaptureStages callbacks.
        verify(mockImageCaptureExtenderImpl, atLeastOnce()).getCaptureStages();

        // To verify there is no any other calls on the mock.
        verifyNoMoreInteractions(mockImageCaptureExtenderImpl);
    }

    @Test
    @MediumTest
    public void canSetSupportedResolutionsToConfigTest() throws CameraInfoUnavailableException {
        assumeTrue(CameraUtil.deviceHasCamera());
        // getSupportedResolutions supported since version 1.1
        assumeTrue(ExtensionVersion.getRuntimeVersion().compareTo(Version.VERSION_1_1) >= 0);

        @CameraSelector.LensFacing int lensFacing =
                CameraAvailabilityUtil.getDefaultLensFacing(mCameraRepository);

        ImageCapture.Builder builder = new ImageCapture.Builder();

        ImageCaptureExtenderImpl mockImageCaptureExtenderImpl = mock(
                ImageCaptureExtenderImpl.class);
        when(mockImageCaptureExtenderImpl.isExtensionAvailable(any(), any())).thenReturn(true);
        List<Pair<Integer, Size[]>> targetFormatResolutionsPairList =
                generateImageCaptureSupportedResolutions(lensFacing);
        when(mockImageCaptureExtenderImpl.getSupportedResolutions()).thenReturn(
                targetFormatResolutionsPairList);

        ImageCaptureExtender fakeExtender = new FakeImageCaptureExtender(builder,
                mockImageCaptureExtenderImpl);

        // Checks the config does not include supported resolutions before applying effect mode.
        assertThat(builder.getUseCaseConfig().getSupportedResolutions(null)).isNull();

        // Checks the config includes supported resolutions after applying effect mode.
        CameraSelector selector =
                new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        fakeExtender.enableExtension(selector);
        List<Pair<Integer, Size[]>> resultFormatResolutionsPairList =
                builder.getUseCaseConfig().getSupportedResolutions(null);
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
        ImageCaptureExtenderImpl mockImageCaptureExtenderImpl = mock(
                ImageCaptureExtenderImpl.class);
        when(mockImageCaptureExtenderImpl.isExtensionAvailable(eq(EXTENSION_AVAILABLE_CAMERA_ID),
                any())).thenReturn(true);
        when(mockImageCaptureExtenderImpl.isExtensionAvailable(
                not(eq(EXTENSION_AVAILABLE_CAMERA_ID)),
                any())).thenReturn(false);
        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder();

        ImageCaptureExtender fakeImageCaptureExtender = new FakeImageCaptureExtender(
                imageCaptureBuilder, mockImageCaptureExtenderImpl);
        fakeImageCaptureExtender.enableExtension(CameraSelector.DEFAULT_BACK_CAMERA);

        CameraSelector cameraSelector =
                imageCaptureBuilder.getUseCaseConfig().getCameraSelector(null);
        assertThat(cameraSelector).isNotNull();
        assertThat(CameraX.getCameraWithCameraSelector(
                cameraSelector).getCameraInfoInternal().getCameraId()).isEqualTo(
                EXTENSION_AVAILABLE_CAMERA_ID);
    }

    private List<Pair<Integer, Size[]>> generateImageCaptureSupportedResolutions(
            @CameraSelector.LensFacing int lensFacing)
            throws CameraInfoUnavailableException {
        List<Pair<Integer, Size[]>> formatResolutionsPairList = new ArrayList<>();
        String cameraId = androidx.camera.extensions.CameraUtil.getCameraIdWithLensFacingUnchecked(
                lensFacing);

        StreamConfigurationMap map =
                androidx.camera.extensions.CameraUtil.getCameraCharacteristics(cameraId).get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map != null) {
            // Retrieves originally supported resolutions from CameraCharacteristics for JPEG and
            // YUV_420_888 formats to return.
            Size[] outputSizes = map.getOutputSizes(ImageFormat.JPEG);

            if (outputSizes != null) {
                formatResolutionsPairList.add(Pair.create(ImageFormat.JPEG, outputSizes));
            }

            outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);

            if (outputSizes != null) {
                formatResolutionsPairList.add(Pair.create(ImageFormat.YUV_420_888, outputSizes));
            }
        }

        return formatResolutionsPairList;
    }

    final class FakeImageCaptureExtender extends ImageCaptureExtender {
        FakeImageCaptureExtender(ImageCapture.Builder builder,
                ImageCaptureExtenderImpl impl) {
            init(builder, impl, ExtensionMode.NONE);
        }
    }

    final class FakeCaptureStage implements CaptureStageImpl {

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public List<Pair<CaptureRequest.Key, Object>> getParameters() {
            List<Pair<CaptureRequest.Key, Object>> parameters = new ArrayList<>();
            return parameters;
        }
    }
}
