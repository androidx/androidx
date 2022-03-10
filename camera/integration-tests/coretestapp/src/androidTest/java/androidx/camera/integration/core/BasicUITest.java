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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.testing.CoreAppTestUtil.ForegroundOccupiedError;
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
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import leakcanary.FailTestOnLeak;

// Tests basic UI operation when using CoreTest app.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class BasicUITest {
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class, true, false);

    @Rule
    public TestRule mUseCamera = CameraUtil.grantCameraPermissionAndPreTest(
            new CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    );

    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Before
    public void setUp() throws ForegroundOccupiedError {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation());

        // Launch Activity
        mActivityRule.launchActivity(mIntent);
    }

    @After
    public void tearDown() {
        pressBackAndReturnHome();
        mActivityRule.finishActivity();
    }

    @Test
    @FailTestOnLeak
    public void testAnalysisButton() {
        IdlingRegistry.getInstance().register(
                mActivityRule.getActivity().getAnalysisIdlingResource());

        ImageAnalysis imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // Click to disable the imageAnalysis use case.
        if (imageAnalysis != null) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
        }

        imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // It is null(disable) and do click to enable use imageAnalysis case.
        if (imageAnalysis == null) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
        }
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getAnalysisIdlingResource());
    }

    @Test
    @FailTestOnLeak
    public void testPreviewButton() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().getViewIdlingResource());

        Preview preview = mActivityRule.getActivity().getPreview();
        // Click to disable the preview use case.
        if (preview != null) {
            // Check preview started.
            onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
            // Click toggle.
            onView(withId(R.id.PreviewToggle)).perform(click());
        }

        // It is null(disable) and do click to enable preview use case.
        if (preview == null) {
            onView(withId(R.id.PreviewToggle)).perform(click());
            // Check preview started.
            onView(withId(R.id.viewFinder)).check(matches(isDisplayed()));
        }
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().getViewIdlingResource());
    }

    private void pressBackAndReturnHome() {
        mDevice.pressBack();

        // Returns to Home to restart next test.
        mDevice.pressHome();
        mDevice.waitForIdle(3000);
    }
}

