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

package androidx.credentials.provider;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.SigningInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 34)
public class CredentialProviderServiceJavaTest {

    @Test
    public void test_createRequest() {
        CredentialProviderServiceTestImpl service = new CredentialProviderServiceTestImpl();
        service.setTestMode(true);

        android.service.credentials.BeginCreateCredentialRequest request =
                new android.service.credentials.BeginCreateCredentialRequest("test", new Bundle());
        OutcomeReceiver<
                        android.service.credentials.BeginCreateCredentialResponse,
                        android.credentials.CreateCredentialException>
                outcome =
                        new OutcomeReceiver<
                                android.service.credentials.BeginCreateCredentialResponse,
                                android.credentials.CreateCredentialException>() {
                    public void onResult(
                                    android.service.credentials.BeginCreateCredentialResponse
                                            response) {}

                    public void onError(
                                    android.credentials.CreateCredentialException error) {}
                };

        // Call the service.
        assertThat(service.getLastCreateRequest()).isNull();
        service.onBeginCreateCredential(request, new CancellationSignal(), outcome);
        assertThat(service.getLastCreateRequest()).isNotNull();
    }

    @Test
    public void test_getRequest() {
        CredentialProviderServiceTestImpl service = new CredentialProviderServiceTestImpl();
        service.setTestMode(true);

        android.service.credentials.BeginGetCredentialOption option =
                new android.service.credentials.BeginGetCredentialOption(
                        "id", "type", new Bundle());
        List<android.service.credentials.BeginGetCredentialOption> options = new ArrayList<>();
        options.add(option);

        android.service.credentials.BeginGetCredentialRequest request =
                        new android.service.credentials.BeginGetCredentialRequest.Builder()
                .setBeginGetCredentialOptions(options).build();
        OutcomeReceiver<
                        android.service.credentials.BeginGetCredentialResponse,
                        android.credentials.GetCredentialException>
                outcome = new OutcomeReceiver<
                        android.service.credentials.BeginGetCredentialResponse,
                                android.credentials.GetCredentialException>() {
                        public void onResult(
                                android.service.credentials.BeginGetCredentialResponse response) {}

                        public void onError(android.credentials.GetCredentialException error) {}
                };

        // Call the service.
        assertThat(service.getLastGetRequest()).isNull();
        service.onBeginGetCredential(request, new CancellationSignal(), outcome);
        assertThat(service.getLastGetRequest()).isNotNull();
    }

    @Test
    public void test_clearRequest() {
        CredentialProviderServiceTestImpl service = new CredentialProviderServiceTestImpl();
        service.setTestMode(true);

        android.service.credentials.ClearCredentialStateRequest request =
                new android.service.credentials.ClearCredentialStateRequest(
                        new android.service.credentials.CallingAppInfo(
                                "name", new SigningInfo()), new Bundle());
        OutcomeReceiver<Void, android.credentials.ClearCredentialStateException> outcome =
                new OutcomeReceiver<
                        Void, android.credentials.ClearCredentialStateException>() {
                    public void onResult(Void response) {}

                    public void onError(
                            android.credentials.ClearCredentialStateException error) {}
                };

        // Call the service.
        assertThat(service.getLastClearRequest()).isNull();
        service.onClearCredentialState(request, new CancellationSignal(), outcome);
        assertThat(service.getLastClearRequest()).isNotNull();
    }
}
