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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.integration.core.idlingresource.ElapsedTimeIdlingResource;
import androidx.camera.integration.core.idlingresource.WaitForViewToShow;
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test toggle buttons in CoreTestApp. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class ToggleButtonUITest {

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

    public static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void testFlashToggleButton() {
        waitFor(new WaitForViewToShow(R.id.flash_toggle));

        ImageCapture useCase = mActivityRule.getActivity().getImageCapture();
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

        waitForIdlingRegistryAndPressBackAndHomeButton();
    }

    @Test
    public void testTorchToggleButton() {
        waitFor(new WaitForViewToShow(R.id.torch_toggle));

        Preview useCase = mActivityRule.getActivity().getPreview();
        assertNotNull(useCase);
        boolean isTorchOn = useCase.isTorchOn();

        onView(withId(R.id.torch_toggle)).perform(click());
        assertNotEquals(useCase.isTorchOn(), isTorchOn);

        // By pressing the torch toggle button two times, it should switch back to original state.
        onView(withId(R.id.torch_toggle)).perform(click());
        assertEquals(useCase.isTorchOn(), isTorchOn);

        waitForIdlingRegistryAndPressBackAndHomeButton();
    }

    @Test
    public void testSwitchCameraToggleButton() {
        waitFor(new WaitForViewToShow(R.id.direction_toggle));

        boolean isPreviewExist = mActivityRule.getActivity().getPreview() != null;
        boolean isImageCaptureExist = mActivityRule.getActivity().getImageCapture() != null;
        boolean isVideoCaptureExist = mActivityRule.getActivity().getVideoCapture() != null;
        boolean isImageAnalysisExist = mActivityRule.getActivity().getImageAnalysis() != null;

        for (int i = 0; i < 2; i++) {
            onView(withId(R.id.direction_toggle)).perform(click());
            waitFor(new ElapsedTimeIdlingResource(2000));
            if (isImageCaptureExist) {
                assertNotNull(mActivityRule.getActivity().getImageCapture());
            }
            if (isImageAnalysisExist) {
                assertNotNull(mActivityRule.getActivity().getImageAnalysis());
            }
            if (isVideoCaptureExist) {
                assertNotNull(mActivityRule.getActivity().getVideoCapture());
            }
            if (isPreviewExist) {
                assertNotNull(mActivityRule.getActivity().getPreview());
            }
        }

        waitForIdlingRegistryAndPressBackAndHomeButton();
    }

    private void waitForIdlingRegistryAndPressBackAndHomeButton() {
        // Idles Espresso thread and make activity complete each action.
        waitFor(new ElapsedTimeIdlingResource(IDLE_TIMEOUT_MS));

        mDevice.pressBack();

        // Returns to Home to restart next test.
        mDevice.pressHome();
        mDevice.wait(Until.hasObject(By.pkg(mLauncherPackageName).depth(0)), LAUNCH_TIMEOUT_MS);
    }

}

