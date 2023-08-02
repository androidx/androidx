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

package androidx.credentials.playservices.createpublickeycredential;


import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.ALL_REQUIRED_AND_OPTIONAL_SIGNATURE;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.ALL_REQUIRED_FIELDS_SIGNATURE;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerTestUtils.createJsonObjectFromPublicKeyCredentialCreationOptions;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;

import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.playservices.TestCredentialsActivity;
import androidx.credentials.playservices.TestUtils;
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CredentialProviderCreatePublicKeyCredentialControllerJavaTest {
    @Test
    public void convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            try {
                JSONObject expectedJson = new JSONObject(
                        MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT);

                PublicKeyCredentialCreationOptions actualResponse =
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                                .convertRequestToPlayServices(
                                        new CreatePublicKeyCredentialRequest(
                                                MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT));
                JSONObject actualJson = createJsonObjectFromPublicKeyCredentialCreationOptions(
                        actualResponse);
                JSONObject requiredKeys = new JSONObject(ALL_REQUIRED_FIELDS_SIGNATURE);

                assertThat(TestUtils.Companion.isSubsetJson(expectedJson, actualJson,
                        requiredKeys)).isTrue();
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void convertRequestToPlayServices_correctRequiredAndOptionalRequest_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            try {
                JSONObject expectedJson = new JSONObject(
                        MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT);

                PublicKeyCredentialCreationOptions actualResponse =
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                                .convertRequestToPlayServices(new CreatePublicKeyCredentialRequest(
                                        MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT));
                JSONObject actualJson =
                        createJsonObjectFromPublicKeyCredentialCreationOptions(
                                actualResponse);
                JSONObject requiredKeys = new JSONObject(ALL_REQUIRED_AND_OPTIONAL_SIGNATURE);

                assertThat(TestUtils.Companion.isSubsetJson(expectedJson, actualJson,
                        requiredKeys)).isTrue();
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }
    @Test
    public void convertRequestToPlayServices_missingRequired_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            try {
                CredentialProviderCreatePublicKeyCredentialController
                        .getInstance(activity)
                        .convertRequestToPlayServices(
                                new CreatePublicKeyCredentialRequest(
                                        MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD));

                // Should not reach here.
                assertWithMessage("Exception should be thrown").that(true).isFalse();
            } catch (Exception e) {
                assertThat(e.getMessage().contains("No value for id")).isTrue();
                assertThat(e.getClass().getName().contains("JSONException")).isTrue();
            }
        });
    }

    @Test
    public void convertRequestToPlayServices_emptyRequired_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            assertThrows("Expected bad required json to throw",
                    JSONException.class,
                    () -> CredentialProviderCreatePublicKeyCredentialController
                            .getInstance(activity).convertRequestToPlayServices(
                                    new CreatePublicKeyCredentialRequest(
                                            MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY)));
        });
    }

    @Test
    public void convertRequestToPlayServices_missingOptionalRequired_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            assertThrows("Expected bad required json to throw",
                    JSONException.class,
                    () -> CredentialProviderCreatePublicKeyCredentialController
                            .getInstance(activity)
                            .convertRequestToPlayServices(
                                    new CreatePublicKeyCredentialRequest(
                                            OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD)));
        });
    }

    @Test
    public void convertRequestToPlayServices_emptyOptionalRequired_throws() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {

            assertThrows("Expected bad required json to throw",
                    JSONException.class,
                    () -> CredentialProviderCreatePublicKeyCredentialController
                            .getInstance(activity)
                            .convertRequestToPlayServices(
                                    new CreatePublicKeyCredentialRequest(
                                            OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD)));
        });
    }

    @Test
    public void convertRequestToPlayServices_missingOptionalNotRequired_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            try {
                JSONObject expectedJson = new JSONObject(
                        OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD);

                PublicKeyCredentialCreationOptions actualResponse =
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                                .convertRequestToPlayServices(
                                        new CreatePublicKeyCredentialRequest(
                                                OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD));
                JSONObject actualJson = createJsonObjectFromPublicKeyCredentialCreationOptions(
                        actualResponse);
                JSONObject requiredKeys = new
                        JSONObject(OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE);

                assertThat(TestUtils.Companion.isSubsetJson(expectedJson, actualJson,
                        requiredKeys)).isTrue();
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
