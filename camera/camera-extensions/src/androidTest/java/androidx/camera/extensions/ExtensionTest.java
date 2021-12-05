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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(Parameterized.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public class ExtensionTest {

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest();

    @Parameterized.Parameters(name = "effect = {0}, facing = {1}")
    public static Collection<Object[]> getParameters() {
        return ExtensionsTestUtil.getAllExtensionsLensFacingCombinations();
    }

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @ExtensionMode.Mode
    private final int mExtensionMode;
    @CameraSelector.LensFacing
    private final int mLensFacing;

    private ProcessCameraProvider mProcessCameraProvider = null;
    private FakeLifecycleOwner mFakeLifecycleOwner;
    private CameraSelector mBaseCameraSelector;
    private CameraSelector mExtensionsCameraSelector;
    private ExtensionsManager mExtensionsManager;

    public ExtensionTest(@ExtensionMode.Mode int extensionMode,
            @CameraSelector.LensFacing int lensFacing) {
        mExtensionMode = extensionMode;
        mLensFacing = lensFacing;
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue(CameraUtil.deviceHasCamera());
        assumeTrue(CameraUtil.hasCameraWithLensFacing(mLensFacing));

        mProcessCameraProvider = ProcessCameraProvider.getInstance(mContext).get(10000,
                TimeUnit.MILLISECONDS);
        mExtensionsManager = ExtensionsManager.getInstanceAsync(mContext,
                mProcessCameraProvider).get(10000, TimeUnit.MILLISECONDS);
        assumeTrue(isTargetDeviceAvailableForExtensions());
        mBaseCameraSelector = new CameraSelector.Builder().requireLensFacing(mLensFacing).build();
        assumeTrue(mExtensionsManager.isExtensionAvailable(mBaseCameraSelector, mExtensionMode));

        mExtensionsCameraSelector = mExtensionsManager.getExtensionEnabledCameraSelector(
                mBaseCameraSelector, mExtensionMode);

        mFakeLifecycleOwner = new FakeLifecycleOwner();
        mFakeLifecycleOwner.startAndResume();
    }

    @After
    public void cleanUp() throws InterruptedException, ExecutionException, TimeoutException {
        if (mProcessCameraProvider != null) {
            mProcessCameraProvider.shutdown().get(10000, TimeUnit.MILLISECONDS);
            mExtensionsManager.shutdown().get(10000, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void testCanBindToLifeCycleAndTakePicture() {
        ImageCapture.OnImageCapturedCallback mockOnImageCapturedCallback = mock(
                ImageCapture.OnImageCapturedCallback.class);

        // To test bind/unbind and take picture.
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        Preview preview = new Preview.Builder().build();

        mInstrumentation.runOnMainSync(
                () -> {
                    // To set the update listener and Preview will change to active state.
                    preview.setSurfaceProvider(createSurfaceTextureProvider(
                            new SurfaceTextureProvider.SurfaceTextureCallback() {
                                @Override
                                public void onSurfaceTextureReady(
                                        @NonNull SurfaceTexture surfaceTexture,
                                        @NonNull Size resolution) {
                                    // No-op.
                                }

                                @Override
                                public void onSafeToRelease(
                                        @NonNull SurfaceTexture surfaceTexture) {
                                    // No-op.
                                }
                            }));

                    mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner,
                            mExtensionsCameraSelector, preview, imageCapture);

                    imageCapture.takePicture(CameraXExecutors.mainThreadExecutor(),
                            mockOnImageCapturedCallback);
                });

        // Verify the image captured.
        ArgumentCaptor<ImageProxy> imageProxy = ArgumentCaptor.forClass(ImageProxy.class);
        verify(mockOnImageCapturedCallback, timeout(10000)).onCaptureSuccess(
                imageProxy.capture());
        assertNotNull(imageProxy.getValue());
        imageProxy.getValue().close(); // Close the image after verification.

        // Verify the take picture should not have any error happen.
        verify(mockOnImageCapturedCallback, never()).onError(any(ImageCaptureException.class));
    }

    @Test
    public void testEventCallbackInConfig() {
        Preview preview = new Preview.Builder().build();
        ImageCapture imageCapture = new ImageCapture.Builder().build();

        mInstrumentation.runOnMainSync(
                () -> mProcessCameraProvider.bindToLifecycle(mFakeLifecycleOwner,
                        mExtensionsCameraSelector, preview, imageCapture));

        // Verify Preview config should have related callback.
        PreviewConfig previewConfig = (PreviewConfig) preview.getCurrentConfig();
        assertNotNull(previewConfig.getUseCaseEventCallback());
        CameraEventCallbacks callback1 = new Camera2ImplConfig(
                previewConfig).getCameraEventCallback(
                null);
        assertNotNull(callback1);
        assertEquals(callback1.getAllItems().size(), 1);
        assertThat(callback1.getAllItems().get(0)).isInstanceOf(CameraEventCallback.class);

        // Verify ImageCapture config should have related callback.
        ImageCaptureConfig imageCaptureConfig =
                (ImageCaptureConfig) imageCapture.getCurrentConfig();
        assertNotNull(imageCaptureConfig.getUseCaseEventCallback());
        assertNotNull(imageCaptureConfig.getCaptureBundle());
        CameraEventCallbacks callback2 = new Camera2ImplConfig(
                imageCaptureConfig).getCameraEventCallback(null);
        assertNotNull(callback2);
        assertEquals(callback2.getAllItems().size(), 1);
        assertThat(callback2.getAllItems().get(0)).isInstanceOf(CameraEventCallback.class);
    }

    /**
     * Returns whether the target camera device can support the test cases.
     */
    private boolean isTargetDeviceAvailableForExtensions() {
        return isLimitedAboveDevice() && !isSpecificSkippedDevice();
    }

    /**
     * Returns whether the device is LIMITED hardware level above.
     *
     * <p>The test cases bind both ImageCapture and Preview. In the test lib implementation for
     * HDR mode, both use cases will occupy YUV_420_888 format of stream. Therefore, the testing
     * target devices need to be LIMITED hardware level at least to support two YUV_420_888
     * streams at the same time.
     *
     * @return true if the testing target camera device is LIMITED hardware level at least.
     * @throws IllegalArgumentException if unable to retrieve {@link CameraCharacteristics} for
     * given lens facing.
     */
    private boolean isLimitedAboveDevice() {
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                mLensFacing);

        if (cameraCharacteristics != null) {
            Integer keyValue = cameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            if (keyValue != null) {
                return keyValue != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
            }
        } else {
            throw new IllegalArgumentException(
                    "Unable to retrieve info for " + mLensFacing + " camera.");
        }

        return false;
    }

    /**
     * Returns that whether the device should be skipped for the test.
     */
    private boolean isSpecificSkippedDevice() {
        if (Build.BRAND.equalsIgnoreCase("SONY") && (Build.MODEL.equalsIgnoreCase("G8142")
                || Build.MODEL.equalsIgnoreCase("G8342"))) {
            return true;
        }

        return false;
    }
}
