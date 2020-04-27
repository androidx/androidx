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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;

import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
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

// Test new activity lifecycle when using CameraX.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class NewActivityLifecycleTest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int IDLE_TIMEOUT_MS = 1000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class, true, false);

    @Rule
    public ActivityTestRule<CameraXActivity> mNewActivityRule =
            new ActivityTestRule<>(CameraXActivity.class, true, false);

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

        mActivityRule.launchActivity(mIntent);
    }

    @After
    public void tearDown() {
        mNewActivityRule.finishActivity();

        mActivityRule.finishActivity();
        pressHomeButton();
    }

    @Test
    public void checkPreviewUpdatedWithNewInstance() {

        // check the 1st activity Preview
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());
        onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());

        pressHomeButton();

        // Make the 1st Activity stoped and create new Activity.
        Intent newIntent = new Intent(Intent.ACTION_MAIN);
        newIntent.setClass(mContext, CameraXActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mNewActivityRule.launchActivity(newIntent);

        // check the New activity Preview
        IdlingRegistry.getInstance().register(
                mNewActivityRule.getActivity().getViewIdlingResource());
        onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(
                mNewActivityRule.getActivity().getViewIdlingResource());

    }

    private void pressHomeButton() {
        mDevice.pressHome();
        mDevice.waitForIdle(LAUNCH_TIMEOUT_MS);
    }

}



