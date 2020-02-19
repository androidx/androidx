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

package androidx.camera.integration.view;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Intent;

import androidx.camera.core.CameraSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
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

@RunWith(AndroidJUnit4.class)
@LargeTest
public final class PreviewViewAppPreviewUpdateTest {
    private static final int WAIT_TIMEOUT = 10000;
    private static final int MAX_TIMEOUT_MS = 3000;
    private final UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
    private Intent mIntent =
            new Intent("androidx.camera.integration.view.action.PREVIEWVIEWAPP");
    private Intent mResumeActivityIntent;
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
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setup() {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();

        CoreAppTestUtil.clearDeviceUI(InstrumentationRegistry.getInstrumentation());

        mResumeActivityIntent = new Intent(mIntent);
        mResumeActivityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        mActivityRule.launchActivity(mIntent);
    }

    @After
    public void tearDown() {
        mActivityRule.finishActivity();
    }

    @Test
    public void checkPreviewUpdatedAfterDestroyRecreate() {
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();

        // Destroy the activity
        pressBackButton();

        // Launch the activity again.
        mActivityRule.launchActivity(mIntent);
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();
    }

    @Test
    public void checkPreviewUpdatedAfterStopResume_3Times() {
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();

        pressHomeButton();

        // Resume the activity
        mActivityRule.getActivity().startActivity(mResumeActivityIntent);
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();

        pressHomeButton();

        // Resume the activity
        mActivityRule.getActivity().startActivity(mResumeActivityIntent);
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();

        pressHomeButton();

        // Resume the activity
        mActivityRule.getActivity().startActivity(mResumeActivityIntent);
        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();
    }

    @Test
    public void checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();
        // Switch camera
        onView(withId(R.id.toggle_camera)).perform(click());

        pressHomeButton();

        // Resume the activity
        mActivityRule.getActivity().startActivity(mResumeActivityIntent);

        assertThat(mActivityRule.getActivity().waitForPreviewUpdating(WAIT_TIMEOUT)).isTrue();
    }

    private void pressHomeButton() {
        mDevice.pressHome();
        mDevice.waitForIdle(MAX_TIMEOUT_MS);
    }

    private void pressBackButton() {
        mDevice.pressBack();
        mDevice.waitForIdle(MAX_TIMEOUT_MS);
    }
}
