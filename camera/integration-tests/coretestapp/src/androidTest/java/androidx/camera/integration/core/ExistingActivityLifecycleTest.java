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

package androidx.camera.integration.core;

import static androidx.camera.testing.CoreAppTestUtil.clearDeviceUI;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assume.assumeTrue;

import android.content.pm.ActivityInfo;

import androidx.camera.core.CameraSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ExistingActivityLifecycleTest {
    private static final int HOME_TIMEOUT_MS = 3000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Before
    public void setup() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();
        assertThat(mLauncherPackageName, notNullValue());

        // Clear the device UI before start each test.
        clearDeviceUI(InstrumentationRegistry.getInstrumentation());
    }

    @After
    public void tearDown() {
        mDevice.pressHome();
        mDevice.waitForIdle(HOME_TIMEOUT_MS);
    }

    // Check if Preview screen is updated or not, after Destroy-Create lifecycle.
    @Test
    public void checkPreviewUpdatedAfterDestroyRecreate() {
        // Launch activity.
        try (ActivityScenario<CameraXActivity> activityScenario =
                     ActivityScenario.launch(CameraXActivity.class)) {
            // Check for view idle, then destroy it.
            checkForViewIdle(activityScenario);
            // Launch new activity and check for view idle.
            checkForViewIdle(activityScenario.recreate());
        }
    }

    // Check if Preview screen is updated or not, after Stop-Resume lifecycle.
    @Test
    public void checkPreviewUpdatedAfterStopResume() {
        // Launch activity.
        try (ActivityScenario<CameraXActivity> activityScenario =
                     ActivityScenario.launch(CameraXActivity.class)) {
            // Check view gets to idle.
            checkForViewIdle(activityScenario);
            // Go through pause/resume then check again for view to get frames then idle.
            activityScenario.moveToState(Lifecycle.State.CREATED).onActivity(activity -> {
                activity.resetViewIdlingResource();
            });
            checkForViewIdle(activityScenario.moveToState(Lifecycle.State.RESUMED));

            // Go through pause/resume then check again for view to get frames then idle, the
            // second pass is used to protect against previous observed issues.
            activityScenario.moveToState(Lifecycle.State.CREATED).onActivity(activity -> {
                activity.resetViewIdlingResource();
            });
            checkForViewIdle(activityScenario.moveToState(Lifecycle.State.RESUMED));
        }
    }

    // Check if Preview screen is updated or not, after toggling camera, then a Destroy-Create
    // lifecycle.
    @Test
    public void checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        // check have front camera
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        try (ActivityScenario<CameraXActivity> activityScenario =
                     ActivityScenario.launch(CameraXActivity.class)) {
            try {
                activityScenario.onActivity(activity -> {
                    IdlingRegistry.getInstance().register(activity.getViewIdlingResource());
                });
                onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
                // Switch camera.
                onView(withId(R.id.direction_toggle)).perform(click());
                // Go through pause/resume then check again for view to get frames then idle.
                activityScenario.moveToState(Lifecycle.State.CREATED);
                activityScenario.onActivity(activity -> {
                    activity.resetViewIdlingResource();
                });
                activityScenario.moveToState(Lifecycle.State.RESUMED);
                onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
            } finally {
                activityScenario.onActivity(activity -> {
                    IdlingRegistry.getInstance().unregister(activity.getViewIdlingResource());
                });
            }
        }
    }

    // Check if Preview screen is updated or not, after rotate device, and Stop-Resume lifecycle.
    @Test
    public void checkPreviewUpdatedAfterRotateDeviceAndStopResume() {
        // Launch activity.
        try (ActivityScenario<CameraXActivity> activityScenario =
                checkForViewIdle(ActivityScenario.launch(CameraXActivity.class))) {
            // Check view gets to idle.
            checkForViewIdle(activityScenario);
            // Rotate to Landscape and the activity will be recreated.
            activityScenario.onActivity(activity -> {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            });
            // Get idling from the re-created activity.
            activityScenario.onActivity(activity -> {
                activity.resetViewIdlingResource();
            });
            checkForViewIdle(activityScenario);

            activityScenario.moveToState(Lifecycle.State.CREATED);
            activityScenario.onActivity(activity -> {
                activity.resetViewIdlingResource();
            });
            activityScenario.moveToState(Lifecycle.State.RESUMED);

            checkForViewIdle(activityScenario);
        }
    }

    private ActivityScenario<CameraXActivity>
            checkForViewIdle(ActivityScenario<CameraXActivity> activityScenario) {
        try {
            activityScenario.onActivity(activity -> {
                IdlingRegistry.getInstance().register(activity.getViewIdlingResource());
            });
            // Check the activity launched and Preview displays frames.
            onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
        } finally {
            // Always release the idling resource, in case of timeout exceptions.
            activityScenario.onActivity(activity -> {
                IdlingRegistry.getInstance().unregister(activity.getViewIdlingResource());
            });
        }
        return activityScenario;
    }
}
