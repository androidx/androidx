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

package androidx.credentials.playservices.beginsignin;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.playservices.TestCredentialsActivity;
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SuppressWarnings("deprecation")
public class CredentialProviderBeginSignInControllerJavaTest {
    @Test
    public void convertRequestToPlayServices_setPasswordOptionRequestAndFalseAutoSelect_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            BeginSignInRequest actualResponse =
                    CredentialProviderBeginSignInController
                            .getInstance(activity)
                            .convertRequestToPlayServices(new GetCredentialRequest(List.of(
                                    new GetPasswordOption()
                            )));

            assertThat(actualResponse.getPasswordRequestOptions().isSupported()).isTrue();
            assertThat(actualResponse.isAutoSelectEnabled()).isFalse();
        });
    }

    @Test
    public void convertRequestToPlayServices_setPasswordOptionRequestAndTrueAutoSelect_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            BeginSignInRequest actualResponse =
                    CredentialProviderBeginSignInController
                            .getInstance(activity)
                            .convertRequestToPlayServices(new GetCredentialRequest(List.of(
                                    new GetPasswordOption(new HashSet<>(), true)
                            )));

            assertThat(actualResponse.getPasswordRequestOptions().isSupported()).isTrue();
            assertThat(actualResponse.isAutoSelectEnabled()).isTrue();
        });
    }

    @Test
    public void convertRequestToPlayServices_nullRequest_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            assertThrows(
                    "null get credential request must throw exception",
                    NullPointerException.class,
                    () -> CredentialProviderBeginSignInController
                            .getInstance(activity)
                            .convertRequestToPlayServices(null)
            );
        });
    }

    @Test
    public void convertResponseToCredentialManager_nullRequest_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            assertThrows(
                    "null sign in credential response must throw exception",
                    NullPointerException.class,
                    () -> CredentialProviderBeginSignInController
                            .getInstance(activity)
                            .convertResponseToCredentialManager(null)
            );
        });
    }

    @Test
    public void convertRequestToPlayServices_setGoogleIdOptionRequestAndTrueAutoSelect_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);

        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setServerClientId("server_client_id")
                .setNonce("nonce")
                .setFilterByAuthorizedAccounts(true)
                .setRequestVerifiedPhoneNumber(false)
                .associateLinkedAccounts("link_service_id", List.of("a", "b", "c"))
                .setAutoSelectEnabled(true)
                .build();

        activityScenario.onActivity(activity -> {

            BeginSignInRequest actualRequest =
                    CredentialProviderBeginSignInController
                            .getInstance(activity)
                            .convertRequestToPlayServices(new GetCredentialRequest(List.of(
                                    option
                            )));

            assertThat(actualRequest.getGoogleIdTokenRequestOptions().isSupported()).isTrue();
            assertThat(actualRequest.isAutoSelectEnabled()).isTrue();

            BeginSignInRequest.GoogleIdTokenRequestOptions actualOption =
                    actualRequest.getGoogleIdTokenRequestOptions();
            assertThat(actualOption.getServerClientId()).isEqualTo(option.getServerClientId());
            assertThat(actualOption.getNonce()).isEqualTo(option.getNonce());
            assertThat(actualOption.filterByAuthorizedAccounts()).isEqualTo(
                    option.getFilterByAuthorizedAccounts());
            assertThat(actualOption.requestVerifiedPhoneNumber()).isEqualTo(
                    option.getRequestVerifiedPhoneNumber());
            assertThat(actualOption.getLinkedServiceId()).isEqualTo(option.getLinkedServiceId());
            assertThat(actualOption.getIdTokenDepositionScopes()).isEqualTo(
                    option.getIdTokenDepositionScopes());

        });
    }
}
