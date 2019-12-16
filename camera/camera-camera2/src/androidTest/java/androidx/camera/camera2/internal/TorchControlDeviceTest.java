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

import android.app.Instrumentation;
import android.content.Context;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class TorchControlDeviceTest {
    @CameraSelector.LensFacing
    private static final int LENS_FACING = CameraSelector.LENS_FACING_BACK;

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    private FakeLifecycleOwner mLifecycleOwner;
    private Camera mCamera;
    private TorchControl mTorchControl;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(LENS_FACING));
        assumeTrue(CameraUtil.hasFlashUnitWithLensFacing(LENS_FACING));

        // Init CameraX
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig config = Camera2Config.defaultConfig();
        CameraX.initialize(context, config);

        // Prepare TorchControl
        mLifecycleOwner = new FakeLifecycleOwner();
        mLifecycleOwner.startAndResume();
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(LENS_FACING).build();
        // To get a functional Camera2CameraControl, it needs to bind an active UseCase and the
        // UseCase must have repeating surface. Create and bind ImageAnalysis as repeating surface.
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
        // Make ImageAnalysis active.
        imageAnalysis.setAnalyzer(CameraXExecutors.mainThreadExecutor(), (image) -> image.close());
        mInstrumentation.runOnMainSync(() ->
                mCamera =
                        CameraX.bindToLifecycle(mLifecycleOwner, cameraSelector, imageAnalysis));
        Camera2CameraControl cameraControl = (Camera2CameraControl) mCamera.getCameraControl();
        mTorchControl = cameraControl.getTorchControl();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }
        CameraX.shutdown().get();
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
