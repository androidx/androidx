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

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
public class CredentialProviderServiceJavaTest {

    @Test
    public void test_createRequest() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

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
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        CredentialProviderServiceTestImpl service = new CredentialProviderServiceTestImpl();
        service.setTestMode(true);

        BeginGetCredentialRequest request =
                new BeginGetCredentialRequest(new ArrayList<BeginGetCredentialOption>());
        OutcomeReceiver<
                        androidx.credentials.provider.BeginGetCredentialResponse,
                        androidx.credentials.exceptions.GetCredentialException>
                outcome =
                        new OutcomeReceiver<
                                androidx.credentials.provider.BeginGetCredentialResponse,
                                androidx.credentials.exceptions.GetCredentialException>() {
                    public void onResult(
                                    androidx.credentials.provider.BeginGetCredentialResponse
                                            response) {}

                    public void onError(
                                    androidx.credentials.exceptions.GetCredentialException error) {}
                };

        // Call the service.
        assertThat(service.getLastGetRequest()).isNull();
        service.onBeginGetCredentialRequest(request, new CancellationSignal(), outcome);
        assertThat(service.getLastGetRequest()).isNotNull();
    }

    @Test
    public void test_clearRequest() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        CredentialProviderServiceTestImpl service = new CredentialProviderServiceTestImpl();
        service.setTestMode(true);

        ProviderClearCredentialStateRequest request =
                new ProviderClearCredentialStateRequest(
                        new CallingAppInfo("name", new SigningInfo()));
        OutcomeReceiver<Void, androidx.credentials.exceptions.ClearCredentialException> outcome =
                new OutcomeReceiver<
                        Void, androidx.credentials.exceptions.ClearCredentialException>() {
                    public void onResult(Void response) {}

                    public void onError(
                            androidx.credentials.exceptions.ClearCredentialException error) {}
                };

        // Call the service.
        assertThat(service.getLastClearRequest()).isNull();
        service.onClearCredentialStateRequest(request, new CancellationSignal(), outcome);
        assertThat(service.getLastClearRequest()).isNotNull();
    }
}
