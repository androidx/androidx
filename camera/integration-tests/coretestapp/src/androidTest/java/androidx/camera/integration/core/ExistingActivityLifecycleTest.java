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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.camera.core.CameraSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ExistingActivityLifecycleTest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int LAUNCH_TIMEOUT_MS = 3000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class, true, false);

    @Before
    public void setup() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();
        assertThat(mLauncherPackageName, notNullValue());

        // Clear the device UI before start each test.
        clearDeviceUI(InstrumentationRegistry.getInstrumentation());

        // First time to launch the CameraXActivity.
        mActivityRule.launchActivity(mIntent);
    }

    @After
    public void tearDown() {
        // When test failed, really unregister the idling resource and don't impact next test.
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());

        mActivityRule.finishActivity();
        pressHomeButton();
    }

    // Check if Preview screen is updated or not, after DestroyRecreate lifecycle(press
    // BACK button and launch again).
    @Test
    public void checkPreviewUpdatedAfterDestroyRecreate() {
        // Check the activity launched and Preview display well.
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());

        pressBackButton();

        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivityRule.launchActivity(mIntent);

        // It create new activity and recheck the Preview UI.
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());
    }

    // Check if Preview screen is updated or not, after StopResume lifecycle(press
    // HOME button and launch again).
    @Test
    public void checkPreviewUpdatedAfterStopResume() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());
        // Check the activity launched and Preview display well.
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        pressHomeButton();
        // Check there is the preview frame update on texture view after activity resume.
        mActivityRule.getActivity().resetViewIdlingResource();
        mActivityRule.getActivity().startActivity(mIntent);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        pressHomeButton();
        // 2nd check the preview frame update on texture view after activity resume.
        mActivityRule.getActivity().resetViewIdlingResource();
        mActivityRule.getActivity().startActivity(mIntent);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());
    }

    // Check if Preview screen is updated or not, after toggle camera, and StopResume lifecycle.
    @Test
    public void checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        // check have front camera
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());

        // Switch camera
        onView(withId(R.id.direction_toggle)).perform(click());
        Assert.assertNotNull(mActivityRule.getActivity().getPreview());
        // check the Preview display well after switching camera
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        pressHomeButton();
        // Check there is the preview frame update on texture view after activity resume.
        mActivityRule.getActivity().resetViewIdlingResource();
        mActivityRule.getActivity().startActivity(mIntent);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());
    }

    // Check if Preview screen is updated or not, after rotate device, and StopResume lifecycle.
    @Test
    public void checkPreviewUpdatedAfterRotateDeviceAndStopResume() {
        // Rotate to Landscape and the activity will be recreated.
        mActivityRule.getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // Check the activity recreated and show up.
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        mIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        pressHomeButton();
        // Check there is the preview frame update on texture view after activity resume.
        mActivityRule.getActivity().resetViewIdlingResource();
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());

        mActivityRule.getActivity().startActivity(mIntent);
        onView(withId(R.id.textureView)).check(matches(isDisplayed()));

        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());
    }

    private void pressHomeButton() {
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }

    private void pressBackButton() {
        mDevice.pressBack();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }


}
