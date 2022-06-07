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

import static androidx.camera.extensions.util.ExtensionsTestUtil.isTargetDeviceAvailableForExtensions;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.util.ExtensionsTestUtil;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.CameraUtil;
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

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(Parameterized.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public class ExtensionTest {

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

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
        assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions(mLensFacing,
                mExtensionMode));

        mProcessCameraProvider = ProcessCameraProvider.getInstance(mContext).get(10000,
                TimeUnit.MILLISECONDS);
        mExtensionsManager = ExtensionsManager.getInstanceAsync(mContext,
                mProcessCameraProvider).get(10000, TimeUnit.MILLISECONDS);
        assumeTrue(isTargetDeviceAvailableForExtensions(mLensFacing, mExtensionMode));
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
            mInstrumentation.runOnMainSync(() -> mProcessCameraProvider.unbindAll());
            mProcessCameraProvider.shutdown().get(10000, TimeUnit.MILLISECONDS);
            mExtensionsManager.shutdown().get(10000, TimeUnit.MILLISECONDS);
        }
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
}
