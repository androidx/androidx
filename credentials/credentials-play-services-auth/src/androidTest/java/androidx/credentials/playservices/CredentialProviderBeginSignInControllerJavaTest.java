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

package androidx.credentials.playservices;


import static androidx.credentials.playservices.TestUtils.EXPECTED_LIFECYCLE_TAG;
import static androidx.credentials.playservices.TestUtils.clearFragmentManager;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController;
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.SignInCredential;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@RequiresApi(api = Build.VERSION_CODES.O)
@SuppressWarnings("deprecation")
public class CredentialProviderBeginSignInControllerJavaTest {

    @Test
    public void getInstance_createBrandNewFragment_constructSuccess() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            android.app.FragmentManager reusedFragmentManager = activity.getFragmentManager();

            clearFragmentManager(reusedFragmentManager);

            assertThat(reusedFragmentManager.getFragments().get(0).getTag().equals(
                    EXPECTED_LIFECYCLE_TAG));
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);

            CredentialProviderBeginSignInController actualBeginSignInController =
                    CredentialProviderBeginSignInController.getInstance(reusedFragmentManager);

            assertThat(actualBeginSignInController).isNotNull();
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);
        });

    }

    @Test
    public void getInstance_createDifferentFragment_replaceWithNewFragmentSuccess() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            android.app.FragmentManager reusedFragmentManager = activity.getFragmentManager();

            clearFragmentManager(reusedFragmentManager);

            assertThat(reusedFragmentManager.getFragments().get(0).getTag().equals(
                    EXPECTED_LIFECYCLE_TAG));
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);

            CredentialProviderCreatePasswordController oldFragment =
                    CredentialProviderCreatePasswordController.getInstance(reusedFragmentManager);

            assertThat(oldFragment).isNotNull();
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);

            CredentialProviderBeginSignInController newFragment =
                    CredentialProviderBeginSignInController.getInstance(reusedFragmentManager);

            assertThat(newFragment).isNotNull();
            assertThat(newFragment).isNotSameInstanceAs(oldFragment);
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);
        });
    }

    @Test
    public void getInstance_createFragment_replaceAttemptGivesBackSameFragmentSuccess() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            android.app.FragmentManager reusedFragmentManager = activity.getFragmentManager();

            clearFragmentManager(reusedFragmentManager);

            assertThat(reusedFragmentManager.getFragments().get(0).getTag().equals(
                    EXPECTED_LIFECYCLE_TAG));
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);

            CredentialProviderBeginSignInController expectedBeginSignInController =
                    CredentialProviderBeginSignInController.getInstance(reusedFragmentManager);

            assertThat(expectedBeginSignInController).isNotNull();
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);

            CredentialProviderBeginSignInController actualBeginSignInController =
                    CredentialProviderBeginSignInController.getInstance(reusedFragmentManager);

            assertThat(actualBeginSignInController).isNotNull();
            assertThat(actualBeginSignInController)
                    .isSameInstanceAs(expectedBeginSignInController);
            assertThat(reusedFragmentManager.getFragments().size()).isEqualTo(1);
        });
    }

    @Test
    public void invokePlayServices_success() {
        // TODO(" Requires mocking inner Identity call. ")
    }

    @Test
    public void convertResponseToCredentialManager_signInCredentialPasswordInput_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        String expectedId = "id";
        String expectedPassword = "password";
        String expectedType = PasswordCredential.TYPE_PASSWORD_CREDENTIAL;
        activityScenario.onActivity(activity -> {
            CredentialProviderBeginSignInController beginSignInController =
                    CredentialProviderBeginSignInController
                            .getInstance(activity.getFragmentManager());
            beginSignInController.callback = new CredentialManagerCallback<GetCredentialResponse,
                    GetCredentialException>() {
                @Override
                public void onResult(@NonNull GetCredentialResponse result) {

                }
                @Override
                public void onError(@NonNull GetCredentialException e) {

                }
            };
            beginSignInController.executor = Runnable::run;

            Credential actualResponse =
                    beginSignInController
                                    .convertResponseToCredentialManager(
                                            new SignInCredential(expectedId, null, null,
                                                    null, null, expectedPassword,
                                                    null, null)
                                    ).getCredential();

            assertThat(actualResponse.getType()).isEqualTo(expectedType);
            assertThat(((PasswordCredential) actualResponse).getPassword())
                    .isEqualTo(expectedPassword);
            assertThat(((PasswordCredential) actualResponse).getId()).isEqualTo(expectedId);
        });
    }

    @Test
    public void
            convertRequestToPlayServices_setPasswordOptionRequestAndFalseAutoSelect_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            BeginSignInRequest actualResponse =
                    CredentialProviderBeginSignInController
                            .getInstance(activity.getFragmentManager())
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
                        .getInstance(activity.getFragmentManager())
                        .convertRequestToPlayServices(new GetCredentialRequest(List.of(
                                new GetPasswordOption()
                        ), true));

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
                            .getInstance(activity.getFragmentManager())
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
                            .getInstance(activity.getFragmentManager())
                            .convertResponseToCredentialManager(null)
            );
        });
    }
}
