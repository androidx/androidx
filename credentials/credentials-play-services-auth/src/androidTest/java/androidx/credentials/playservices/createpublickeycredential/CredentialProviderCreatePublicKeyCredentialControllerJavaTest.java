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

import android.app.Activity;
import android.os.Build;

import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.playservices.TestCredentialsActivity;
import androidx.credentials.playservices.TestUtils;
import androidx.credentials.playservices.controllers.CreatePublicKeyCredential.CredentialProviderCreatePublicKeyCredentialController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.filters.SmallTest;

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SmallTest
public class CredentialProviderCreatePublicKeyCredentialControllerJavaTest {

    private final boolean mUseFragmentActivity;

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[] {true, false};
    }

    public CredentialProviderCreatePublicKeyCredentialControllerJavaTest(
            final boolean useFragmentActivity) throws Throwable {
        mUseFragmentActivity = useFragmentActivity;
    }

    interface TestActivityListener {
        void onActivity(Activity a);
    }

    private void launchTestActivity(TestActivityListener listener) {
        if (mUseFragmentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ActivityScenario<androidx.credentials.playservices.TestCredentialsFragmentActivity>
                    activityScenario =
                            ActivityScenario.launch(
                                    androidx.credentials.playservices
                                            .TestCredentialsFragmentActivity.class);
            activityScenario.onActivity(
                    activity -> {
                        listener.onActivity((Activity) activity);
                    });
        } else {
            ActivityScenario<TestCredentialsActivity> activityScenario =
                    ActivityScenario.launch(TestCredentialsActivity.class);
            activityScenario.onActivity(
                    activity -> {
                        listener.onActivity((Activity) activity);
                    });
        }
    }

    private PublicKeyCredentialCreationOptions convertRequestToPlayServices(
            Activity activity, String type) {
        CreatePublicKeyCredentialRequest pubKeyRequest = new CreatePublicKeyCredentialRequest(type);
        return CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
                .convertRequestToPlayServices(pubKeyRequest);
    }

    @Test
    public void convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        launchTestActivity(
                activity -> {
                    try {
                        JSONObject expectedJson =
                                new JSONObject(MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT);

                        PublicKeyCredentialCreationOptions actualResponse =
                                convertRequestToPlayServices(
                                        activity,
                                        MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT);
                        JSONObject actualJson =
                                createJsonObjectFromPublicKeyCredentialCreationOptions(
                                        actualResponse);
                        JSONObject requiredKeys = new JSONObject(ALL_REQUIRED_FIELDS_SIGNATURE);

                        assertThat(
                                        TestUtils.Companion.isSubsetJson(
                                                expectedJson, actualJson, requiredKeys))
                                .isTrue();
                        // TODO("Add remaining tests in detail after discussing ideal form")
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void convertRequestToPlayServices_correctRequiredAndOptionalRequest_success() {
        launchTestActivity(
                activity -> {
                    try {
                        JSONObject expectedJson =
                                new JSONObject(
                                        MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT);

                        PublicKeyCredentialCreationOptions actualResponse =
                                convertRequestToPlayServices(
                                        activity,
                                        MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT);
                        JSONObject actualJson =
                                createJsonObjectFromPublicKeyCredentialCreationOptions(
                                        actualResponse);
                        JSONObject requiredKeys =
                                new JSONObject(ALL_REQUIRED_AND_OPTIONAL_SIGNATURE);

                        assertThat(
                                        TestUtils.Companion.isSubsetJson(
                                                expectedJson, actualJson, requiredKeys))
                                .isTrue();
                        // TODO("Add remaining tests in detail after discussing ideal form")
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void convertRequestToPlayServices_missingRequired_throws() {
        launchTestActivity(
                activity -> {
                    try {
                        PublicKeyCredentialCreationOptions actualResponse =
                                convertRequestToPlayServices(
                                        activity,
                                        MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT);

                        CreatePublicKeyCredentialRequest pubKeyRequest =
                                new CreatePublicKeyCredentialRequest(
                                        MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT);
                        CredentialProviderCreatePublicKeyCredentialController.getInstance(activity)
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
        launchTestActivity(
                activity -> {
                    assertThrows(
                            "Expected bad required json to throw",
                            JSONException.class,
                            () ->
                                    convertRequestToPlayServices(
                                            activity,
                                            MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY));
                });
    }

    @Test
    public void convertRequestToPlayServices_missingOptionalRequired_throws() {
        launchTestActivity(
                activity -> {
                    assertThrows(
                            "Expected bad required json to throw",
                            JSONException.class,
                            () ->
                                    convertRequestToPlayServices(
                                            activity,
                                            OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD));
                });
    }

    @Test
    public void convertRequestToPlayServices_emptyOptionalRequired_throws() {
        launchTestActivity(
                activity -> {
                    assertThrows(
                            "Expected bad required json to throw",
                            JSONException.class,
                            () ->
                                    convertRequestToPlayServices(
                                            activity,
                                            OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD));
                });
    }

    @Test
    public void convertRequestToPlayServices_missingOptionalNotRequired_success() {
        launchTestActivity(
                activity -> {
                    try {
                        JSONObject expectedJson =
                                new JSONObject(OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD);

                        PublicKeyCredentialCreationOptions actualResponse =
                                convertRequestToPlayServices(
                                        activity,
                                        OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD);
                        JSONObject actualJson =
                                createJsonObjectFromPublicKeyCredentialCreationOptions(
                                        actualResponse);
                        JSONObject requiredKeys =
                                new JSONObject(OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE);

                        assertThat(
                                        TestUtils.Companion.isSubsetJson(
                                                expectedJson, actualJson, requiredKeys))
                                .isTrue();
                        // TODO("Add remaining tests in detail after discussing ideal form")
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
