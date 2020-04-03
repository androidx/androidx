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

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.CoreAppTestUtil;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public final class PreviewViewFragmentTest {

    private static final int WAIT_TIMEOUT = 10000;
    private static final int PREVIEW_UPDATE_COUNT = 30;

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
        CoreAppTestUtil.clearDeviceUI(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void checkPreviewUpdatedAfterDestroyRecreate() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        // Recreate fragment
        scenario.recreate();
        assertPreviewUpdating(scenario);
    }

    @Test
    public void checkPreviewUpdatedAfterStopResume_3Times() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(scenario);

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(scenario);

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(scenario);
    }

    @Test
    public void checkPreviewUpdatedAfterToggleCameraAndStopResume() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT));
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK));

        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        // Toggle camera
        onView(withId(R.id.toggle_camera)).perform(click());

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertPreviewUpdating(scenario);
    }

    @Test
    public void checkPreviewNotUpdatedAfterPreviewUnbound() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        // Toggle visibility to unbind preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        assertPreviewNotUpdating(scenario);
    }

    @Test
    public void checkPreviewUpdatedWhenSamePreviewViewAttachedToMultiplePreviewUseCases() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        // Toggle visibility to unbind preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        // Toggle visibility to bind new preview
        onView(withId(R.id.toggle_visibility)).perform(click());
        assertPreviewUpdating(scenario);
    }

    @Test
    public void checkSameScaleTypeAfterStopResume() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        getPreviewView(scenario).setScaleType(PreviewView.ScaleType.FIT_END);

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertThat(getPreviewView(scenario).getScaleType()).isEqualTo(
                PreviewView.ScaleType.FIT_END);
    }

    @Test
    public void checkSameImplementationModeAfterStopResume() {
        final FragmentScenario<PreviewViewFragment> scenario = createScenario();
        assertPreviewUpdating(scenario);

        getPreviewView(scenario).setPreferredImplementationMode(
                PreviewView.ImplementationMode.TEXTURE_VIEW);

        // Stop the fragment
        scenario.moveToState(Lifecycle.State.CREATED);
        // Resume the fragment
        scenario.moveToState(Lifecycle.State.RESUMED);
        assertThat(getPreviewView(scenario).getPreferredImplementationMode()).isEqualTo(
                PreviewView.ImplementationMode.TEXTURE_VIEW);
    }

    @NonNull
    private FragmentScenario<PreviewViewFragment> createScenario() {
        return FragmentScenario.launchInContainer(PreviewViewFragment.class, null, R.style.AppTheme,
                null);
    }

    private void assertPreviewUpdating(@NonNull FragmentScenario<PreviewViewFragment> scenario) {
        assertPreviewUpdateState(scenario, true);
    }

    private void assertPreviewNotUpdating(@NonNull FragmentScenario<PreviewViewFragment> scenario) {
        assertPreviewUpdateState(scenario, false);
    }

    /**
     * Waits at most for the duration {@link #WAIT_TIMEOUT} for the preview to update at least
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
            isPreviewUpdating = latch.await(WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
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
        scenario.onFragment(fragment -> {
            previewView.set(
                    (PreviewView) fragment.requireActivity().findViewById(R.id.preview_view));
        });
        return previewView.get();
    }
}
