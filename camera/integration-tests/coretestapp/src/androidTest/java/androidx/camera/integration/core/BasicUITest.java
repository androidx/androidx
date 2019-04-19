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

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.integration.core.idlingresource.ElapsedTimeIdlingResource;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Tests basic UI operation when using CoreTest app.
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class BasicUITest {

    private static final int LAUNCH_TIMEOUT_MS = 5000;
    private static final int IDLE_TIMEOUT_MS = 1000;

    private final UiDevice mDevice =
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String mLauncherPackageName = mDevice.getLauncherPackageName();

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class);

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
        checkViewReady();
    }

    @After
    public void tearDown() {
        pressBackAndReturnHome();
    }

    @Test
    public void testAnalysisButton() {

        ImageAnalysis imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // Click to disable the imageAnalysis use case.
        if (imageAnalysis != null) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
            waitForIdlingRegistry();
        }

        imageAnalysis = mActivityRule.getActivity().getImageAnalysis();
        // Click to enable use imageAnalysis case.
        if (imageAnalysis == null) {
            IdlingRegistry.getInstance().register(
                    mActivityRule.getActivity().mAnalysisIdlingResource);
            onView(withId(R.id.AnalysisToggle)).perform(click());
            IdlingRegistry.getInstance().unregister(
                    mActivityRule.getActivity().mAnalysisIdlingResource);
        }

    }

    @Test
    public void testPreviewButton() {

        Preview preview = mActivityRule.getActivity().getPreview();
        // Click to disable the preview use case.
        if (preview != null) {
            onView(withId(R.id.PreviewToggle)).perform(click());
            waitForIdlingRegistry();
        }

        preview = mActivityRule.getActivity().getPreview();
        // Click to enable preview use case.
        if (preview == null) {
            IdlingRegistry.getInstance().register(mActivityRule.getActivity().mViewIdlingResource);
            onView(withId(R.id.PreviewToggle)).perform(click());
            IdlingRegistry.getInstance().unregister(
                    mActivityRule.getActivity().mViewIdlingResource);
        }

    }

    private void checkViewReady() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mViewIdlingResource);
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(mActivityRule.getActivity().mViewIdlingResource);
    }

    private void waitForIdlingRegistry() {
        // Idles Espresso thread and make activity complete each action.
        IdlingResource idlingResource = new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS);
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    private void pressBackAndReturnHome() {
        mDevice.pressBack();

        // Returns to Home to restart next test.
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }
}

