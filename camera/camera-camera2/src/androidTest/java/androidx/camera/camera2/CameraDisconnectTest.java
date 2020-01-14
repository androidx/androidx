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


import static androidx.camera.testing.CoreAppTestUtil.clearDeviceUI;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.camera.core.CameraX;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.activity.Camera2TestActivity;
import androidx.camera.testing.activity.CameraXTestActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CameraDisconnectTest {

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public ActivityTestRule<CameraXTestActivity> mCameraXTestActivityRule =
            new ActivityTestRule<>(CameraXTestActivity.class, true, false);
    @Rule
    public ActivityTestRule<Camera2TestActivity> mCamera2ActivityRule =
            new ActivityTestRule<>(Camera2TestActivity.class, true, false);

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private CameraXTestActivity mCameraXTestActivity;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, Camera2Config.defaultConfig());

        // Clear the device UI before start each test.
        clearDeviceUI(InstrumentationRegistry.getInstrumentation());

        mCameraXTestActivityRule.launchActivity(new Intent());
        mCameraXTestActivity = mCameraXTestActivityRule.getActivity();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        mCameraXTestActivityRule.finishActivity();
        mCamera2ActivityRule.finishActivity();

        // Actively unbind all use cases to avoid lifecycle callback later to stop/clear use case
        // after shutdown() is complete.
        if (CameraX.isInitialized()) {
            mInstrumentation.runOnMainSync(CameraX::unbindAll);
        }

        CameraX.shutdown().get();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Known issue, checkout b/147393563.
    public void testDisconnect_launchCamera2App() {
        // Specific compatibility check for the test.
        CoreAppTestUtil.assumeCanTestCameraDisconnect();

        waitFor(mCameraXTestActivity.mPreviewReady);
        String cameraId = mCameraXTestActivity.mCameraId;
        assumeNotNull(cameraId);

        // Launch another activity and open the camera by camera2 API. It should cause the camera
        // disconnect from CameraX.
        mCamera2ActivityRule.launchActivity(
                new Intent().putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, cameraId));
        waitFor(mCamera2ActivityRule.getActivity().mPreviewReady);
        mCamera2ActivityRule.finishActivity();

        // Verify the CameraX Preview can resume successfully.
        waitFor(mCameraXTestActivity.mPreviewReady);
    }

    public static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

}
