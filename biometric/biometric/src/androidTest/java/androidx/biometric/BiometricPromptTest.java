/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.biometric;

import static androidx.biometric.BiometricManager.Authenticators;

import static com.google.common.truth.Truth.assertThat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BiometricPromptTest {

    @After
    public void cleanup() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome();
    }

    @Test // prevents test runner from failing since all tests are ignored
    @SdkSuppress(minSdkVersion = 18)
    public void dummy() {}

    @Test
    @SdkSuppress(minSdkVersion = 18)
    @Ignore("TODO(b/225187683): fails in postsubmit")
    public void testViewModel_inActivity() {
        try (ActivityScenario<TestActivity> scenario =
                     ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                final BiometricPrompt prompt = new BiometricPrompt(activity,
                        MoreExecutors.directExecutor(),
                        new BiometricPrompt.AuthenticationCallback() {});
                try {
                    start(prompt);
                    final Fragment promptFragment = activity.getSupportFragmentManager()
                            .findFragmentByTag(BiometricPrompt.BIOMETRIC_FRAGMENT_TAG);
                    assertThat(getViewModelOrNull(promptFragment, true /* hostedInActivity */))
                            .isSameInstanceAs(findViewModel(activity));
                    assertThat(getViewModelOrNull(promptFragment, false /* hostedInActivity */))
                            .isNull();
                } finally {
                    prompt.cancelAuthentication();
                }
            });
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 18)
    @Ignore("TODO(b/225187683): fails in postsubmit")
    public void testViewModel_inFragment() {
        try (FragmentScenario<TestFragment> scenario =
                     FragmentScenario.launchInContainer(TestFragment.class)) {
            scenario.onFragment(fragment -> {
                final BiometricPrompt prompt = new BiometricPrompt(fragment,
                        MoreExecutors.directExecutor(),
                        new BiometricPrompt.AuthenticationCallback() {});
                try {
                    start(prompt);
                    final Fragment promptFragment = fragment.getChildFragmentManager()
                            .findFragmentByTag(BiometricPrompt.BIOMETRIC_FRAGMENT_TAG);
                    assertThat(getViewModelOrNull(promptFragment, false /* hostedInActivity */))
                            .isSameInstanceAs(findViewModel(fragment));
                    assertThat(getViewModelOrNull(promptFragment, true /* hostedInActivity */))
                            .isNotSameInstanceAs(findViewModel(fragment));
                } finally {
                    prompt.cancelAuthentication();
                }
            });
        }
    }

    private void start(BiometricPrompt prompt) {
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle("title")
                .setSubtitle("subtitle")
                .setDescription("description")
                .setConfirmationRequired(false)
                .setAllowedAuthenticators(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL)
                .build());
    }

    private BiometricViewModel getViewModelOrNull(Fragment fragment, boolean hostedInActivity) {
        try {
            return BiometricPrompt.getViewModel(fragment, hostedInActivity);
        } catch (Throwable t) {
            return null;
        }
    }

    private BiometricViewModel findViewModel(ViewModelStoreOwner owner) {
        return new ViewModelProvider(owner).get(BiometricViewModel.class);
    }

    public static class TestActivity extends FragmentActivity {}

    public static class TestFragment extends Fragment {}
}
