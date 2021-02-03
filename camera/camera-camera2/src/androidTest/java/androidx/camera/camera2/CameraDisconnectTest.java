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

import static androidx.camera.testing.CoreAppTestUtil.ForegroundOccupiedError;
import static androidx.camera.testing.CoreAppTestUtil.assumeCanTestCameraDisconnect;
import static androidx.camera.testing.CoreAppTestUtil.assumeCompatibleDevice;
import static androidx.camera.testing.CoreAppTestUtil.prepareDeviceUI;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.camera.core.CameraX;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.activity.Camera2TestActivity;
import androidx.camera.testing.activity.CameraXTestActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CameraDisconnectTest {

    @Rule
    public TestRule mCameraRule = CameraUtil.grantCameraPermissionAndPreTest();

    @Before
    public void setUp() throws ForegroundOccupiedError {
        assumeCompatibleDevice();
        assumeCanTestCameraDisconnect();

        final Context context = ApplicationProvider.getApplicationContext();
        CameraX.initialize(context, Camera2Config.defaultConfig());

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        prepareDeviceUI(InstrumentationRegistry.getInstrumentation());
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10_000, TimeUnit.MILLISECONDS);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Known issue, checkout b/147393563.
    public void testDisconnect_launchCamera2App() {
        // Launch CameraX test activity
        final ActivityScenario<CameraXTestActivity> cameraXActivity = ActivityScenario.launch(
                CameraXTestActivity.class);

        // Wait for preview to become active
        final AtomicReference<CountingIdlingResource> cameraXPreviewReady = new AtomicReference<>();
        cameraXActivity.onActivity(activity -> cameraXPreviewReady.set(activity.getPreviewReady()));
        waitFor(cameraXPreviewReady.get());

        // Get id of camera opened by CameraX test activity
        final AtomicReference<String> cameraId = new AtomicReference<>();
        cameraXActivity.onActivity(activity -> cameraId.set(activity.getCameraId()));
        assertThat(cameraId.get()).isNotNull();

        // Launch Camera2 test activity. It should cause the camera to disconnect from CameraX.
        final Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                Camera2TestActivity.class);
        intent.putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, cameraId.get());
        final ActivityScenario<Camera2TestActivity> camera2Activity = ActivityScenario.launch(
                intent);

        // Wait for preview to become active
        final AtomicReference<CountingIdlingResource> camera2PreviewReady = new AtomicReference<>();
        camera2Activity.onActivity(activity -> camera2PreviewReady.set(activity.mPreviewReady));
        waitFor(camera2PreviewReady.get());

        // Close Camera2 test activity, and verify the CameraX Preview resumes successfully.
        camera2Activity.close();
        waitFor(cameraXPreviewReady.get());

        // Close CameraX test activity
        cameraXActivity.close();
    }

    private static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

}
