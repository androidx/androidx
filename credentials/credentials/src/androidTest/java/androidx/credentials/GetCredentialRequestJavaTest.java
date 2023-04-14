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

package androidx.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ComponentName;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetCredentialRequestJavaTest {
    @Test
    public void constructor_emptyCredentialOptions_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetCredentialRequest(new ArrayList<>()));

    }

    @Test
    public void constructor() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest(expectedCredentialOptions);

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
        assertThat(request.getPreferIdentityDocUi()).isFalse();
        assertThat(request.preferImmediatelyAvailableCredentials()).isFalse();
        assertThat(request.getPreferUiBrandingComponentName()).isNull();
    }

    @Test
    public void constructor_nonDefaultPreferUiBrandingComponentName() {
        ArrayList<CredentialOption> options = new ArrayList<>();
        options.add(new GetPasswordOption());
        ComponentName expectedComponentName = new ComponentName("test pkg", "test cls");

        GetCredentialRequest request = new GetCredentialRequest(
                options, /*origin=*/ null, /*preferIdentityDocUi=*/false, expectedComponentName);

        assertThat(request.getCredentialOptions().get(0).isAutoSelectAllowed()).isFalse();
        assertThat(request.getPreferUiBrandingComponentName()).isEqualTo(expectedComponentName);
    }

    @Test
    public void constructor_nonDefaultPreferImmediatelyAvailableCredentials() {
        ArrayList<CredentialOption> options = new ArrayList<>();
        options.add(new GetPasswordOption());
        boolean expectedPreferImmediatelyAvailableCredentials = true;

        GetCredentialRequest request = new GetCredentialRequest(
                options, /*origin=*/ null, /*preferIdentityDocUi=*/false,
                /*preferUiBrandingComponentName=*/ null,
                expectedPreferImmediatelyAvailableCredentials);

        assertThat(request.getCredentialOptions().get(0).isAutoSelectAllowed()).isFalse();
        assertThat(request.preferImmediatelyAvailableCredentials())
                .isEqualTo(expectedPreferImmediatelyAvailableCredentials);
    }

    @Test
    public void constructor_defaultAutoSelect() {
        ArrayList<CredentialOption> options = new ArrayList<>();
        options.add(new GetPasswordOption());

        GetCredentialRequest request = new GetCredentialRequest(options);

        assertThat(request.getCredentialOptions().get(0).isAutoSelectAllowed()).isFalse();
        assertThat(request.getPreferIdentityDocUi()).isFalse();
    }

    @Test
    public void builder_addCredentialOption() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(expectedCredentialOptions.get(0))
                .addCredentialOption(expectedCredentialOptions.get(1))
                .build();

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
    }

    @Test
    public void builder_setCredentialOptions() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .build();

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
        assertThat(request.getPreferIdentityDocUi()).isFalse();
        assertThat(request.preferImmediatelyAvailableCredentials()).isFalse();
        assertThat(request.getPreferUiBrandingComponentName()).isNull();
    }

    @Test
    public void builder_setPreferIdentityDocUi() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferIdentityDocUi(true)
                .build();

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
        assertThat(request.getPreferIdentityDocUi()).isTrue();
    }

    @Test
    public void builder_setPreferImmediatelyAvailableCredentials() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));
        boolean expectedPreferImmediatelyAvailableCredentials = true;

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferImmediatelyAvailableCredentials(
                        expectedPreferImmediatelyAvailableCredentials)
                .build();

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
        assertThat(request.preferImmediatelyAvailableCredentials())
                .isEqualTo(expectedPreferImmediatelyAvailableCredentials);
    }

    @Test
    public void builder_setPreferUiBrandingComponentName() {
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));
        ComponentName expectedComponentName = new ComponentName("test pkg", "test cls");

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferUiBrandingComponentName(expectedComponentName)
                .build();

        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
        assertThat(request.getPreferUiBrandingComponentName()).isEqualTo(expectedComponentName);
    }

    @Test
    public void builder_defaultAutoSelect() {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetPasswordOption())
                .build();

        assertThat(request.getCredentialOptions().get(0).isAutoSelectAllowed()).isFalse();
    }

    @Test
    public void frameworkConversion() {
        ArrayList<CredentialOption> options = new ArrayList<>();
        options.add(new GetPasswordOption());
        boolean expectedPreferImmediatelyAvailableCredentials = true;
        ComponentName expectedComponentName = new ComponentName("test pkg", "test cls");
        boolean expectedPreferIdentityDocUi = true;
        String expectedOrigin = "origin";
        GetCredentialRequest request = new GetCredentialRequest(options, expectedOrigin,
                expectedPreferIdentityDocUi, expectedComponentName,
                expectedPreferImmediatelyAvailableCredentials);


        GetCredentialRequest convertedRequest = GetCredentialRequest.createFrom(
                options, request.getOrigin(), GetCredentialRequest.toRequestDataBundle(request)
        );

        assertThat(convertedRequest.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(convertedRequest.getPreferIdentityDocUi()).isEqualTo(
                expectedPreferIdentityDocUi);
        assertThat(convertedRequest.getPreferUiBrandingComponentName()).isEqualTo(
                expectedComponentName);
        assertThat(convertedRequest.preferImmediatelyAvailableCredentials()).isEqualTo(
                expectedPreferImmediatelyAvailableCredentials);
    }
}
