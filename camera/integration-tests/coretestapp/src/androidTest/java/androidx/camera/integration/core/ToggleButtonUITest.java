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
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.integration.core.idlingresource.ElapsedTimeIdlingResource;
import androidx.camera.integration.core.idlingresource.WaitForViewToShow;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test toggle buttons in CoreTestApp. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ToggleButtonUITest {

    @Rule
    public ActivityTestRule<CameraXActivity> mActivityRule =
            new ActivityTestRule<>(CameraXActivity.class);

    public static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    // Test switch flash mode toggle button.
    @Test
    public void testFlashToggleButton() {
        waitFor(new WaitForViewToShow(R.id.flash_toggle));

        ImageCaptureUseCase useCase = mActivityRule.getActivity().getImageCaptureUseCase();
        assertNotNull(useCase);

        // There are 3 different states of flash mode: ON, OFF and AUTO.
        // By pressing flash mode toggle button, the flash mode would switch to the next state.
        // The flash mode would loop in following sequence: OFF -> AUTO -> ON -> OFF.
        FlashMode mode1 = useCase.getFlashMode();

        onView(withId(R.id.flash_toggle)).perform(click());
        FlashMode mode2 = useCase.getFlashMode();
        // After the switch, the mode2 should be different from mode1.
        assertNotEquals(mode2, mode1);

        onView(withId(R.id.flash_toggle)).perform(click());
        FlashMode mode3 = useCase.getFlashMode();
        // The mode3 should be different from first and second time.
        assertNotEquals(mode3, mode2);
        assertNotEquals(mode3, mode1);
    }

    // Test torch toggle button.
    // To press the torch toggle button to switch torch between ON/OFF state.
    @Test
    public void testTorchToggleButton() {
        waitFor(new WaitForViewToShow(R.id.torch_toggle));

        ViewFinderUseCase useCase = mActivityRule.getActivity().getViewFinderUseCase();
        assertNotNull(useCase);
        boolean isTorchOn = useCase.isTorchOn();

        onView(withId(R.id.torch_toggle)).perform(click());
        assertNotEquals(useCase.isTorchOn(), isTorchOn);

        // By pressing the torch toggle button two times, it should switch back to original state.
        onView(withId(R.id.torch_toggle)).perform(click());
        assertEquals(useCase.isTorchOn(), isTorchOn);
    }

    // Test camera switch toggle button.
    @Test
    public void testSwitchCameraToggleButton() {
        waitFor(new WaitForViewToShow(R.id.direction_toggle));

        boolean isViewFinderExist = mActivityRule.getActivity().getViewFinderUseCase() != null;
        boolean isImageCaptureExist = mActivityRule.getActivity().getImageCaptureUseCase() != null;
        boolean isVideoCaptureExist = mActivityRule.getActivity().getVideoCaptureUseCase() != null;
        boolean isImageAnalysisExist =
                mActivityRule.getActivity().getImageAnalysisUseCase() != null;

        for (int i = 0; i < 2; i++) {
            onView(withId(R.id.direction_toggle)).perform(click());
            waitFor(new ElapsedTimeIdlingResource(2000));
            if (isImageCaptureExist) {
                assertNotNull(mActivityRule.getActivity().getImageCaptureUseCase());
            }
            if (isImageAnalysisExist) {
                assertNotNull(mActivityRule.getActivity().getImageAnalysisUseCase());
            }
            if (isVideoCaptureExist) {
                assertNotNull(mActivityRule.getActivity().getVideoCaptureUseCase());
            }
            if (isViewFinderExist) {
                assertNotNull(mActivityRule.getActivity().getViewFinderUseCase());
            }
        }
    }

}

