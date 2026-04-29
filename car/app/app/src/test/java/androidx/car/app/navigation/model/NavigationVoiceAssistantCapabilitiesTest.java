/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.navigation.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class NavigationVoiceAssistantCapabilitiesTest {

    @Test
    public void createInstance() {
        NavigationVoiceAssistantCapabilities capabilities =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_EXIT_NAVIGATION)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_MUTE_AND_UNMUTE)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_TRAFFIC)
                        .build();

        assertThat(capabilities.isVoiceAssistantConsentGranted()).isTrue();
        assertThat(capabilities.getSupportedActions())
                .containsExactly(
                        NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS,
                        NavigationVoiceAssistantCapabilities.ACTION_EXIT_NAVIGATION,
                        NavigationVoiceAssistantCapabilities.ACTION_MUTE_AND_UNMUTE);
        assertThat(capabilities.getSupportedDisruptions())
                .containsExactly(
                        NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH,
                        NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_TRAFFIC);
    }

    @Test
    public void createInstance_empty() {
        NavigationVoiceAssistantCapabilities capabilities =
                new NavigationVoiceAssistantCapabilities.Builder().build();

        assertThat(capabilities.isVoiceAssistantConsentGranted()).isFalse();
        assertThat(capabilities.getSupportedActions()).isEmpty();
        assertThat(capabilities.getSupportedDisruptions()).isEmpty();
    }

    @Test
    public void getSupportedActions_returnsUnmodifiableSet() {
        NavigationVoiceAssistantCapabilities capabilities =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> capabilities.getSupportedActions().add(
                        NavigationVoiceAssistantCapabilities.ACTION_EXIT_NAVIGATION)
        );
    }

    @Test
    public void getSupportedDisruptions_returnsUnmodifiableSet() {
        NavigationVoiceAssistantCapabilities capabilities =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        assertThrows(
                UnsupportedOperationException.class,
                () -> capabilities.getSupportedDisruptions().add(
                        NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_TRAFFIC)
        );
    }

    @Test
    public void createInstance_modifyBuilderAfterBuild_doesNotAffectInstance() {
        NavigationVoiceAssistantCapabilities.Builder builder =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH);

        NavigationVoiceAssistantCapabilities capabilities = builder.build();

        builder.addSupportedAction(NavigationVoiceAssistantCapabilities.ACTION_EXIT_NAVIGATION);
        builder.addSupportedDisruption(
                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_TRAFFIC);

        assertThat(capabilities.getSupportedActions())
                .containsExactly(NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS);
        assertThat(capabilities.getSupportedDisruptions())
                .containsExactly(NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH);
    }

    @Test
    public void equals() {
        NavigationVoiceAssistantCapabilities capabilities1 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        NavigationVoiceAssistantCapabilities capabilities2 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        assertThat(capabilities1).isEqualTo(capabilities2);
    }

    @Test
    public void notEquals_differentConsent() {
        NavigationVoiceAssistantCapabilities capabilities1 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .build();

        NavigationVoiceAssistantCapabilities capabilities2 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(false)
                        .build();

        assertThat(capabilities1).isNotEqualTo(capabilities2);
    }

    @Test
    public void notEquals_differentActions() {
        NavigationVoiceAssistantCapabilities capabilities1 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .build();

        NavigationVoiceAssistantCapabilities capabilities2 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities
                                        .ACTION_ALLOW_AND_AVOID_HIGHWAYS)
                        .build();

        assertThat(capabilities1).isNotEqualTo(capabilities2);
    }

    @Test
    public void notEquals_differentDisruptions() {
        NavigationVoiceAssistantCapabilities capabilities1 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        NavigationVoiceAssistantCapabilities capabilities2 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_TRAFFIC)
                        .build();

        assertThat(capabilities1).isNotEqualTo(capabilities2);
    }

    @Test
    public void hashCode_equals() {
        NavigationVoiceAssistantCapabilities capabilities1 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        NavigationVoiceAssistantCapabilities capabilities2 =
                new NavigationVoiceAssistantCapabilities.Builder()
                        .setVoiceAssistantConsentGranted(true)
                        .addSupportedAction(
                                NavigationVoiceAssistantCapabilities.ACTION_ALLOW_AND_AVOID_TOLLS)
                        .addSupportedDisruption(
                                NavigationVoiceAssistantCapabilities.DISRUPTION_REPORT_CRASH)
                        .build();

        assertThat(capabilities1.hashCode()).isEqualTo(capabilities2.hashCode());
    }

}
