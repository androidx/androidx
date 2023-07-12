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

package androidx.camera.camera2.internal;

import static org.junit.Assume.assumeTrue;

import android.content.Context;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.internal.util.TestUtil;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CameraXUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class TorchControlDeviceTest {
    @CameraSelector.LensFacing
    private static final int LENS_FACING = CameraSelector.LENS_FACING_BACK;

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

    private TorchControl mTorchControl;
    private CameraUseCaseAdapter mCamera;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING));
        assumeTrue(CameraUtil.hasFlashUnitWithLensFacing(LENS_FACING));

        // Init CameraX
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();
        CameraXUtil.initialize(context, config);

        // Prepare TorchControl
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(LENS_FACING).build();
        // To get a functional Camera2CameraControlImpl, it needs to bind an active UseCase and the
        // UseCase must have repeating surface. Create and bind ImageAnalysis as repeating surface.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        // Make ImageAnalysis active.
        imageAnalysis.setAnalyzer(CameraXExecutors.mainThreadExecutor(), ImageProxy::close);
        mCamera = CameraUtil.createCameraAndAttachUseCase(context, cameraSelector, imageAnalysis);
        Camera2CameraControlImpl cameraControl =
                TestUtil.getCamera2CameraControlImpl(mCamera.getCameraControl());

        mTorchControl = cameraControl.getTorchControl();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (mCamera != null) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    //TODO: The removeUseCases() call might be removed after clarifying the
                    // abortCaptures() issue in b/162314023.
                    mCamera.removeUseCases(mCamera.getUseCases())
            );
        }

        CameraXUtil.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test(timeout = 5000L)
    public void enableDisableTorch_futureWillCompleteSuccessfully()
            throws ExecutionException, InterruptedException {
        ListenableFuture<Void> future = mTorchControl.enableTorch(true);
        // Future should return with no exception
        future.get();

        future = mTorchControl.enableTorch(false);
        // Future should return with no exception
        future.get();
    }
}
