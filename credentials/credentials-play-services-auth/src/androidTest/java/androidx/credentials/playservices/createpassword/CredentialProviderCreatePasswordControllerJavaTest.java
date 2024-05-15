/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.playservices.createpassword;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePasswordRequest;
import androidx.credentials.CreatePasswordResponse;
import androidx.credentials.playservices.TestCredentialsActivity;
import androidx.credentials.playservices.TestUtils;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import kotlin.Unit;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SuppressWarnings("deprecation")
public class CredentialProviderCreatePasswordControllerJavaTest {

    @Test
    public void convertResponseToCredentialManager_unitInput_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        String expectedResponseType = new CreatePasswordResponse().getType();
        activityScenario.onActivity(
                activity -> {
                    CreateCredentialResponse actualResponse =
                            androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity)
                                    .convertResponseToCredentialManager(Unit.INSTANCE);

                    assertThat(actualResponse.getType()).isEqualTo(expectedResponseType);
                    assertThat(TestUtils.Companion.equals(actualResponse.getData(), Bundle.EMPTY))
                            .isTrue();
                });
    }

    @Test
    public void convertRequestToPlayServices_createPasswordRequest_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        String expectedId = "LM";
        String expectedPassword = "SodaButton";
        activityScenario.onActivity(
                activity -> {
                    com.google.android.gms.auth.api.identity.SignInPassword actualRequest =
                            androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity)
                                    .convertRequestToPlayServices(
                                            new CreatePasswordRequest(expectedId, expectedPassword))
                                    .getSignInPassword();

                    assertThat(actualRequest.getPassword()).isEqualTo(expectedPassword);
                    assertThat(actualRequest.getId()).isEqualTo(expectedId);
                });
    }

    @Test
    public void convertRequestToPlayServices_nullRequest_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(
                activity -> {
                    assertThrows(
                            "null create password request must throw exception",
                            NullPointerException.class,
                            () ->
                                    androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity)
                                            .convertRequestToPlayServices(null)
                                            .getSignInPassword());
                });
    }

    @Test
    public void convertResponseToCredentialManager_nullRequest_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(
                activity -> {
                    assertThrows(
                            "null unit response must throw exception",
                            NullPointerException.class,
                            () ->
                                    androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity)
                                            .convertResponseToCredentialManager(null));
                });
    }

    @Test
    public void duplicateGetInstance_shouldBeUnequal() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(
                activity -> {
                    androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController firstInstance =
                            androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity);
                    androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController secondInstance =
                            androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.getInstance(activity);
                    assertThat(firstInstance).isNotEqualTo(secondInstance);
                });
    }
}
