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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BasicUITest {
    private static final int TEST_COUNT = 10;

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class);

    // Test the Analysis (image analysis) button.
    @Test
    public void testAnalysisButton() {
        checkViewReady();

        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.AnalysisToggle)).perform(click());
            onView(withId(R.id.AnalysisToggle)).perform(click());
        }
    }

    // Test the Preview (view finder) button.
    @Test
    public void testPreviewButton() {
        checkViewReady();

        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.PreviewToggle)).perform(click());
            onView(withId(R.id.PreviewToggle)).perform(click());
        }
    }

    // Test the Photo (image capture) button.
    @Test
    public void testPhotoButton() {
        checkViewReady();
        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.PhotoToggle)).perform(click());
            onView(withId(R.id.PhotoToggle)).perform(click());
        }
    }

    // Test the Video (video capture) button
    @Test
    public void testVideoButton() {
        checkViewReady();
        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.VideoToggle)).perform(click());
            onView(withId(R.id.VideoToggle)).perform(click());
        }
    }

    // Test the Torch button
    @Test
    public void testTorchButton() {
        checkViewReady();
        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.torch_toggle)).perform(click());
            onView(withId(R.id.torch_toggle)).perform(click());
        }
    }


    // Test the ToggleCamera button.
    @Test
    public void testToggleCameraButton() {
        checkViewReady();
        // multiple pair click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.direction_toggle)).perform(click());
            onView(withId(R.id.direction_toggle)).perform(click());
        }
    }

    // Test the photo capture button
    @Test
    public void testPhotoCaptureButton() {
        checkViewReady();
        // multiple click test
        for (int i = 0; i < TEST_COUNT; i++) {
            onView(withId(R.id.Picture)).perform(click());
        }
    }

    private void checkViewReady() {
        IdlingRegistry.getInstance().register(mActivityRule.getActivity().mIdlingResource);
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        onView(withId(R.id.textureView)).perform(click()).check(matches(isDisplayed()));
        IdlingRegistry.getInstance().unregister(mActivityRule.getActivity().mIdlingResource);
    }

}

