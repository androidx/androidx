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

import static androidx.camera.integration.core.ToggleButtonUITest.waitFor;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.integration.core.idlingresource.ElapsedTimeIdlingResource;
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

// Tests basic UI operation when using CoreTest app.
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class BasicUITest {
    private static final int IDLE_TIMEOUT_MS = 1000;
    private static final String BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core";
    private static final int DISMISS_LOCK_SCREEN_CODE = 82;


    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = mContext.getPackageManager()
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE);


    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
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
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        // In case the lock screen on top, the action to dismiss it.
        mDevice.pressKeyCode(DISMISS_LOCK_SCREEN_CODE);
        mDevice.pressHome();

        // Launch Activity
        mActivityRule.launchActivity(mIntent);

        // Close system dialogs first to avoid interrupt.
        mActivityRule.getActivity().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @After
    public void tearDown() {
        pressBackAndReturnHome();
        mActivityRule.finishActivity();
    }


    @Test
    public void testAnalysisButton() {
        checkAnalysisReady();

        ImageAnalysis imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // Click to disable the imageAnalysis use case.
        if (imageAnalysis != null) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
            waitFor(new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS));
        }

        imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // It is null(disable) and do click to enable use imageAnalysis case.
        if (imageAnalysis == null) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
            checkAnalysisReady();
        }
    }

    @Test
    public void testPreviewButton() {
        checkPreviewReady();

        Preview preview = mActivityRule.getActivity().getPreview();
        // Click to disable the preview use case.
        if (preview != null) {
            onView(withId(R.id.PreviewToggle)).perform(click());
            waitFor(new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS));
        }

        preview = mActivityRule.getActivity().getPreview();
        // It is null(disable) and do click to enable preview use case.
        if (preview == null) {
            onView(withId(R.id.PreviewToggle)).perform(click());
            checkPreviewReady();
        }

    }

    private void checkPreviewReady() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mViewIdlingResource);
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(mActivityRule.getActivity().mViewIdlingResource);
    }

    private void checkAnalysisReady() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mAnalysisIdlingResource);
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().mAnalysisIdlingResource);
    }

    private void pressBackAndReturnHome() {
        mDevice.pressBack();

        // Returns to Home to restart next test.
        mDevice.pressHome();
    }
}

