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

import static org.junit.Assert.assertThrows;

import android.content.ComponentName;
import android.content.pm.SigningInfo;
import android.os.Bundle;

import androidx.credentials.CredentialOption;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ProviderGetCredentialRequestJavaTest {

    @Test
    public void constructor_success() {
        new ProviderGetCredentialRequest(
                Collections.singletonList(CredentialOption.createFrom("type", new Bundle(),
                        new Bundle(), true, ImmutableSet.of())),
                new CallingAppInfo("name",
                new SigningInfo()));
    }

    @Test
    public void constructor_nullInputs_throws() {
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new ProviderGetCredentialRequest(null, null)
        );
    }

    @Test
    public void getter_credentialOptions() {
        String expectedType = "BoeingCred";
        String expectedQueryKey = "PilotName";
        String expectedQueryValue = "PilotPassword";
        Bundle expectedCandidateQueryData = new Bundle();
        expectedCandidateQueryData.putString(expectedQueryKey, expectedQueryValue);
        String expectedRequestKey = "PlaneKey";
        String expectedRequestValue = "PlaneInfo";
        Bundle expectedRequestData = new Bundle();
        expectedRequestData.putString(expectedRequestKey, expectedRequestValue);
        boolean expectedRequireSystemProvider = true;
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );

        ProviderGetCredentialRequest providerGetCredentialRequest =
                new ProviderGetCredentialRequest(
                        Collections.singletonList(CredentialOption.createFrom(expectedType,
                                expectedRequestData,
                                expectedCandidateQueryData,
                                expectedRequireSystemProvider,
                                expectedAllowedProviders)),
                        new CallingAppInfo("name",
                                new SigningInfo()));
        List<CredentialOption> actualCredentialOptionsList =
                providerGetCredentialRequest.getCredentialOptions();
        assertThat(actualCredentialOptionsList.size()).isEqualTo(1);
        String actualType = actualCredentialOptionsList.get(0).getType();
        String actualRequestValue =
                actualCredentialOptionsList.get(0).getRequestData().getString(expectedRequestKey);
        String actualQueryValue =
                actualCredentialOptionsList.get(0).getCandidateQueryData()
                        .getString(expectedQueryKey);
        boolean actualRequireSystemProvider =
                actualCredentialOptionsList.get(0).isSystemProviderRequired();

        assertThat(actualType).isEqualTo(expectedType);
        assertThat(actualRequestValue).isEqualTo(expectedRequestValue);
        assertThat(actualQueryValue).isEqualTo(expectedQueryValue);
        assertThat(actualRequireSystemProvider).isEqualTo(expectedRequireSystemProvider);
        assertThat(actualCredentialOptionsList.get(0).getAllowedProviders())
                .containsAtLeastElementsIn(expectedAllowedProviders);
    }

    @Test
    public void getter_signingInfo() {
        String expectedPackageName = "cool.security.package";

        ProviderGetCredentialRequest providerGetCredentialRequest =
                new ProviderGetCredentialRequest(
                        Collections.singletonList(CredentialOption.createFrom("type", new Bundle(),
                                new Bundle(), true, ImmutableSet.of())),
                        new CallingAppInfo(expectedPackageName,
                        new SigningInfo()));
        String actualPackageName =
                providerGetCredentialRequest.getCallingAppInfo().getPackageName();

        assertThat(actualPackageName).isEqualTo(expectedPackageName);
    }
}
