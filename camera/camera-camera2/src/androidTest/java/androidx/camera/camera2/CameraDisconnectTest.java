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


import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;

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
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@LargeTest
@FlakyTest
public class CameraDisconnectTest {

    private static final int DISMISS_LOCK_SCREEN_CODE = 82;

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public ActivityTestRule<CameraXTestActivity> mCameraXTestActivityRule =
            new ActivityTestRule<>(CameraXTestActivity.class, true, false);
    @Rule
    public ActivityTestRule<Camera2TestActivity> mCamera2ActivityRule =
            new ActivityTestRule<>(Camera2TestActivity.class, true, false);

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    private CameraXTestActivity mCameraXTestActivity;

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        // In case the lock screen on top, the action to dismiss it.
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressKeyCode(
                DISMISS_LOCK_SCREEN_CODE);

        // Close system dialogs first to avoid interrupt.
        ApplicationProvider.getApplicationContext().sendBroadcast(
                new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        mCameraXTestActivityRule.launchActivity(new Intent());
        mCameraXTestActivity = mCameraXTestActivityRule.getActivity();
    }

    @After
    public void tearDown() {
        mCameraXTestActivityRule.finishActivity();
        mCamera2ActivityRule.finishActivity();
    }

    @Test
    public void testDisconnect_launchNativeCameraApp() throws InterruptedException {
        waitFor(mCameraXTestActivity.mPreviewReady);

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (mCameraXTestActivity.mLensFacing == CameraX.LensFacing.FRONT) {
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        }
        PackageManager packageManager = mCameraXTestActivity.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        // Only continue the test when the device contains app which can record the video.
        assumeTrue(activities.size() > 0);

        if (activities.size() > 1) {
            // When the available app > 1, launch the first app directly instead of the
            // Intent chooser.
            ActivityInfo info = activities.get(0).activityInfo;
            intent.setComponent(new ComponentName(info.packageName, info.name));
        }
        mCameraXTestActivity.startActivity(intent);

        // Wait for the app start and opening the camera.
        Thread.sleep(3000);

        // Returning to CameraX activity by press the back key.
        mDevice.pressBack();

        // Verify the CameraX Preview can resume successfully.
        waitFor(mCameraXTestActivity.mPreviewReady);
    }

    @Test
    public void testDisconnect_launchCamera2App() {
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
