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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public final class PreviewViewFragmentTest {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int PREVIEW_UPDATE_COUNT = 30;

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

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private FragmentScenario<PreviewViewFragment> mScenario;

    @Before
    public void setup() throws CoreAppTestUtil.ForegroundOccupiedError {
        assumeTrue(CameraUtil.deviceHasCamera());
        CoreAppTestUtil.assumeCompatibleDevice();
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(mInstrumentation);
        mScenario = createScenario();
    }

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.moveToState(Lifecycle.State.DESTROYED);
        }
    }

    @Test
    public void checkPreviewUpdatedAfterDestroyRecreate() {
        assertPreviewUpdating(mScenario);

        // Recreate fragment
        mScenario.recreate();
        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkPreviewUpdatedAfterStopResume_3Times() {
        assertPreviewUpdating(mScenario);

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(mScenario);

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(mScenario);

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        assertPreviewUpdating(mScenario);

        // Toggle camera
        onView(withId(R.id.toggle_camera)).perform(click());

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkPreviewUpdatedAfterToggleCameraTwiceAndStopResume() {
        // This is to test that after toggling between the front and back camera the front camera
        // will still be working after stopping and resuming the lifecycle state.

        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        // Toggle camera. This is to toggle the camera from back to front.
        onView(withId(R.id.toggle_camera)).perform(click());

        // Toggle camera twice. This is to toggle the camera from front to back.
        onView(withId(R.id.toggle_camera)).perform(click());

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkPreviewUpdatesAfterToggleCameraMultipleTimes() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        assertPreviewUpdating(mScenario);

        for (int i = 0; i <= 10; i++) {
            onView(withId(R.id.toggle_camera)).perform(click());
        }

        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkPreviewNotUpdatedAfterPreviewUnbound() {
        assertPreviewUpdating(mScenario);

        // Toggle visibility to unbind preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        assertPreviewNotUpdating(mScenario);
    }

    @Test
    public void checkPreviewUpdatedWhenSamePreviewViewAttachedToMultiplePreviewUseCases() {
        assertPreviewUpdating(mScenario);

        // Toggle visibility to unbind preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        // Toggle visibility to bind new preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        assertPreviewUpdating(mScenario);
    }

    @Test
    public void checkSameScaleTypeAfterStopResume() {
        assertPreviewUpdating(mScenario);

        mInstrumentation.runOnMainSync(
                () -> getPreviewView(mScenario).setScaleType(PreviewView.ScaleType.FIT_END));

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        mInstrumentation.runOnMainSync(
                () -> assertThat(getPreviewView(mScenario).getScaleType()).isEqualTo(
                        PreviewView.ScaleType.FIT_END));
    }

    @Test
    public void checkSameImplementationModeAfterStopResume() {
        assertPreviewUpdating(mScenario);

        mInstrumentation.runOnMainSync(
                () -> getPreviewView(mScenario).setImplementationMode(
                        PreviewView.ImplementationMode.COMPATIBLE));

        // Stop the fragment
        mScenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        mScenario.moveToState(Lifecycle.State.RESUMED);
        mInstrumentation.runOnMainSync(
                () -> assertThat(getPreviewView(mScenario).getImplementationMode()).isEqualTo(
                        PreviewView.ImplementationMode.COMPATIBLE));
    }

    @NonNull
    private FragmentScenario<PreviewViewFragment> createScenario() {
        return FragmentScenario.launchInContainer(PreviewViewFragment.class, null, R.style.AppTheme,
                new FragmentFactory());
    }

    private void assertPreviewUpdating(@NonNull FragmentScenario<PreviewViewFragment> scenario) {
        assertPreviewUpdateState(scenario, true);
    }

    private void assertPreviewNotUpdating(@NonNull FragmentScenario<PreviewViewFragment> scenario) {
        assertPreviewUpdateState(scenario, false);
    }

    /**
     * Waits at most for the duration {@link #TIMEOUT_SECONDS} for the preview to update at least
     * {@link #PREVIEW_UPDATE_COUNT} times.
     */
    private void assertPreviewUpdateState(@NonNull FragmentScenario<PreviewViewFragment> scenario,
            boolean shouldPreviewUpdate) {
        AtomicReference<PreviewViewFragment> fragment = new AtomicReference<>();
        scenario.onFragment(fragment::set);

        final CountDownLatch latch = new CountDownLatch(PREVIEW_UPDATE_COUNT);
        fragment.get().setPreviewUpdatingLatch(latch);

        boolean isPreviewUpdating;
        try {
            isPreviewUpdating = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            isPreviewUpdating = false;
        }

        if (shouldPreviewUpdate) {
            assertThat(isPreviewUpdating).isTrue();
        } else {
            assertThat(isPreviewUpdating).isFalse();
        }
    }

    @NonNull
    private PreviewView getPreviewView(@NonNull FragmentScenario<PreviewViewFragment> scenario) {
        final AtomicReference<PreviewView> previewView = new AtomicReference<>();
        scenario.onFragment(fragment -> previewView.set(
                (PreviewView) fragment.requireActivity().findViewById(R.id.preview_view)));
        return previewView.get();
    }
}
