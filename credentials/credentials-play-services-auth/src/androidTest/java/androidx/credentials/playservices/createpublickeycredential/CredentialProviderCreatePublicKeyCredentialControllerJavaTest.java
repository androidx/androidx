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

import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.CREATE_REQUEST_INPUT_REQUIRED_AND_OPTIONAL;
import static androidx.credentials.playservices.createkeycredential.CreatePublicKeyCredentialControllerUtils.CREATE_REQUEST_INPUT_REQUIRED_ONLY;

import static com.google.common.truth.Truth.assertThat;

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
    public void
            convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            try {
                JSONObject expectedJson = new JSONObject(CREATE_REQUEST_INPUT_REQUIRED_ONLY);

                PublicKeyCredentialCreationOptions actualResponse =
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                                .convertRequestToPlayServices(
                                        new CreatePublicKeyCredentialRequest(
                                                CREATE_REQUEST_INPUT_REQUIRED_ONLY));
                JSONObject actualJson =
                        TestUtils.Companion
                                .createJsonObjectFromPublicKeyCredentialCreationOptions(
                                        actualResponse);

                assertThat(TestUtils.Companion.isSubsetJson(expectedJson, actualJson)).isTrue();
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
                        CREATE_REQUEST_INPUT_REQUIRED_AND_OPTIONAL);

                PublicKeyCredentialCreationOptions actualResponse =
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                                .convertRequestToPlayServices(
                                        new CreatePublicKeyCredentialRequest(
                                                CREATE_REQUEST_INPUT_REQUIRED_ONLY));
                JSONObject actualJson =
                        TestUtils.Companion
                                .createJsonObjectFromPublicKeyCredentialCreationOptions(
                                        actualResponse);

                assertThat(TestUtils.Companion.isSubsetJson(expectedJson, actualJson)).isTrue();
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
