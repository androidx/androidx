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

package androidx.camera.camera2;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Test if camera control functionality can run well in real devices. Autofocus may not work well in
 * devices because the camera might be faced down to the desktop and the auto-focus will never
 * finish on some devices. Thus we don't test AF related functions.
 */
@LargeTest
@RunWith(Parameterized.class)
public class CameraControlDeviceTest {
    @Parameterized.Parameter(0)
    public CameraSelector mCameraSelector;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[]{CameraSelector.DEFAULT_BACK_CAMERA});
        result.add(new Object[]{CameraSelector.DEFAULT_FRONT_CAMERA});
        return result;
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);
    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FakeLifecycleOwner mLifecycleOwner;
    private Camera mCamera;
    private MeteringPoint mMeteringPoint1;
    private ImageAnalysis.Analyzer mAnalyzer = (image) -> {
        image.close();
    };

    @Before
    public void setUp()
            throws CameraInfoUnavailableException, ExecutionException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = Camera2Config.defaultConfig();
        CameraX.initialize(context, cameraXConfig).get();

        assumeTrue(CameraX.hasCamera(mCameraSelector));

        mLifecycleOwner = new FakeLifecycleOwner();

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1, 1);
        mMeteringPoint1 = factory.createPoint(0, 0);

        mInstrumentation.runOnMainSync(() -> {
            ImageAnalysis useCase = new ImageAnalysis.Builder().build();
            mCamera = CameraX.bindToLifecycle(mLifecycleOwner, mCameraSelector,
                    useCase);
            mLifecycleOwner.startAndResume();
            useCase.setAnalyzer(CameraXExecutors.ioExecutor(), mAnalyzer);
        });
    }

    private static CameraCharacteristics getCameraCharacteristicWithLensFacing(
            @CameraSelector.LensFacing int lensFacing) {
        return CameraUtil.getCameraCharacteristics(lensFacing);
    }

    private static boolean isSupportAeRegion(CameraSelector cameraSelector) {
        try {
            CameraCharacteristics characteristics = getCameraCharacteristicWithLensFacing(
                    cameraSelector.getLensFacing());
            return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSupportAwbRegion(CameraSelector cameraSelector) {
        try {
            CameraCharacteristics characteristics = getCameraCharacteristicWithLensFacing(
                    cameraSelector.getLensFacing());
            return characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        CameraX.shutdown().get();
    }


    @Test
    public void startFocusMeteringAe_futureCompletes() {
        assumeTrue(isSupportAeRegion(mCameraSelector));

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mMeteringPoint1,
                        FocusMeteringAction.FLAG_AE).build();
        ListenableFuture<FocusMeteringResult> future =
                mCamera.getCameraControl().startFocusAndMetering(action);

        assertFutureCompletes(future);
    }

    @Test
    public void startFocusMeteringAwb_futureCompletes() {
        assumeTrue(isSupportAwbRegion(mCameraSelector));

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mMeteringPoint1,
                        FocusMeteringAction.FLAG_AWB).build();
        ListenableFuture<FocusMeteringResult> future =
                mCamera.getCameraControl().startFocusAndMetering(action);

        assertFutureCompletes(future);
    }

    @Test
    public void startFocusMeteringAeAwb_futureCompletes() {
        assumeTrue(isSupportAeRegion(mCameraSelector) || isSupportAwbRegion(mCameraSelector));

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mMeteringPoint1,
                        FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB).build();
        ListenableFuture<FocusMeteringResult> future =
                mCamera.getCameraControl().startFocusAndMetering(action);

        assertFutureCompletes(future);
    }

    @Test
    public void startFocusMeteringMorePointThanSupported_futureCompletes() {
        assumeTrue(isSupportAeRegion(mCameraSelector) || isSupportAwbRegion(mCameraSelector));

        SurfaceOrientedMeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(1, 1);
        // Most devices don't support 4 AF/AE/AWB regions. but it should still complete.
        MeteringPoint point1 = factory.createPoint(0, 0);
        MeteringPoint point2 = factory.createPoint(1, 0);
        MeteringPoint point3 = factory.createPoint(0.2f, 0.2f);
        MeteringPoint point4 = factory.createPoint(0.3f, 0.4f);
        FocusMeteringAction action =
                new FocusMeteringAction.Builder(point1,
                        FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                        .addPoint(point2,
                                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                        .addPoint(point3,
                                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                        .addPoint(point4,
                                FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB)
                        .build();

        ListenableFuture<FocusMeteringResult> future =
                mCamera.getCameraControl().startFocusAndMetering(action);

        assertFutureCompletes(future);
    }

    @Test
    public void cancelFocusMetering_futureCompletes() {
        FocusMeteringAction action =
                new FocusMeteringAction.Builder(mMeteringPoint1).build();
        mCamera.getCameraControl().startFocusAndMetering(action);
        ListenableFuture<Void> result = mCamera.getCameraControl().cancelFocusAndMetering();

        assertFutureCompletes(result);
    }

    private <T> void assertFutureCompletes(ListenableFuture<T> future) {
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            fail("future fail:" + e);
        }
    }
}
