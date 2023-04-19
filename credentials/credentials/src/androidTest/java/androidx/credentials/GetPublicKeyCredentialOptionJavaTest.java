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

import static androidx.credentials.CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED;
import static androidx.credentials.GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetPublicKeyCredentialOptionJavaTest {

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new GetPublicKeyCredentialOption("")
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NPE",
                NullPointerException.class,
                () -> new GetPublicKeyCredentialOption(null)
        );
    }

    @Test
    public void constructor_success() {
        new GetPublicKeyCredentialOption(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}");
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        GetPublicKeyCredentialOption getPublicKeyCredentialOpt =
                new GetPublicKeyCredentialOption(testJsonExpected);
        String testJsonActual = getPublicKeyCredentialOpt.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_frameworkProperties_success() {
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        boolean expectedIsAutoSelect = true;
        byte[] clientDataHash = "hash".getBytes();
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION);
        expectedData.putString(BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putByteArray(GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
                clientDataHash);
        expectedData.putBoolean(BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, expectedIsAutoSelect);

        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(
                requestJsonExpected, clientDataHash, expectedAllowedProviders);

        assertThat(option.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(option.getRequestData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(option.isSystemProviderRequired()).isFalse();
        assertThat(option.getAllowedProviders())
                .containsAtLeastElementsIn(expectedAllowedProviders);
    }

    @Test
    public void frameworkConversion_success() {
        byte[] clientDataHash = "hash".getBytes();
        Set<ComponentName> expectedAllowedProviders = ImmutableSet.of(
                new ComponentName("pkg", "cls"),
                new ComponentName("pkg2", "cls2")
        );
        GetPublicKeyCredentialOption option = new GetPublicKeyCredentialOption(
                "json", clientDataHash, expectedAllowedProviders);

        CredentialOption convertedOption = CredentialOption.createFrom(
                option.getType(), option.getRequestData(),
                option.getCandidateQueryData(), option.isSystemProviderRequired(),
                option.getAllowedProviders());

        assertThat(convertedOption).isInstanceOf(GetPublicKeyCredentialOption.class);
        GetPublicKeyCredentialOption convertedSubclassOption =
                (GetPublicKeyCredentialOption) convertedOption;
        assertThat(convertedSubclassOption.getRequestJson()).isEqualTo(option.getRequestJson());
        assertThat(convertedSubclassOption.getAllowedProviders())
                .containsAtLeastElementsIn(expectedAllowedProviders);
    }
}
