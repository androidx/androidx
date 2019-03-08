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

import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

// Test basic UI operation when using CoreTest app.
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BasicUITest {
    private static final int TEST_COUNT = 10;

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class);

    @Rule
    public GrantPermissionRule cameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule storagePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule audioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    @Before
    public void setup() {
        checkViewReady();
    }

    // Test the Analysis (image analysis) button.
    @Test
    public void testAnalysisButton() {

        IdlingRegistry.getInstance().register(
                mActivityRule.getActivity().mAnalysisIdlingResource);
        for (int i = 0; i < TEST_COUNT; i++) {
            // Click to disable the use case.
            onView(withId(R.id.AnalysisToggle)).perform(click());

            // Click to enable use case and check use case if ready.
            onView(withId(R.id.AnalysisToggle)).perform(click());
        }

        IdlingRegistry.getInstance().unregister(
                mActivityRule.getActivity().mAnalysisIdlingResource);

    }

    // Test the Preview (view finder) button.
    @Test
    public void testPreviewButton() {

        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mViewIdlingResource);
        // Click once to disable, then once to re-enable and check view is ready.
        for (int i = 0; i < TEST_COUNT; i++) {
            // Disable Preview.
            onView(withId(R.id.PreviewToggle)).perform(click());
            // Enable Preview.
            onView(withId(R.id.PreviewToggle)).perform(click());
            onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        }

        IdlingRegistry.getInstance().unregister(mActivityRule.getActivity().mViewIdlingResource);

    }

    private void checkViewReady() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mViewIdlingResource);
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(mActivityRule.getActivity().mViewIdlingResource);
    }

}

