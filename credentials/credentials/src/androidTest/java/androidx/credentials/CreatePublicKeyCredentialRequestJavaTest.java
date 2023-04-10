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

import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_ALLOW_HYBRID;
import static androidx.credentials.CreatePublicKeyCredentialRequest.BUNDLE_KEY_REQUEST_JSON;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreatePublicKeyCredentialRequestJavaTest {

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new CreatePublicKeyCredentialRequest("")
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NPE",
                NullPointerException.class,
                () -> new CreatePublicKeyCredentialRequest(null)
        );
    }

    @Test
    public void constructor_success()  {
        new CreatePublicKeyCredentialRequest(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}");
    }

    @Test
    public void constructor_setsAllowHybridToTrueByDefault()  {
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                new CreatePublicKeyCredentialRequest(
                        "JSON");
        boolean allowHybridActual = createPublicKeyCredentialRequest.allowHybrid();
        assertThat(allowHybridActual).isTrue();
    }

    @Test
    public void constructor_setsAllowHybridToFalse()  {
        boolean allowHybridExpected = false;
        CreatePublicKeyCredentialRequest createPublicKeyCredentialRequest =
                new CreatePublicKeyCredentialRequest("testJson",
                        allowHybridExpected);
        boolean allowHybridActual = createPublicKeyCredentialRequest.allowHybrid();
        assertThat(allowHybridActual).isEqualTo(allowHybridExpected);
    }

    @Test
    public void getter_requestJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        CreatePublicKeyCredentialRequest createPublicKeyCredentialReq =
                new CreatePublicKeyCredentialRequest(testJsonExpected);

        String testJsonActual = createPublicKeyCredentialReq.getRequestJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_frameworkProperties_success() {
        String requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        boolean allowHybridExpected = false;
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
                CreatePublicKeyCredentialRequest
                        .BUNDLE_VALUE_SUBTYPE_CREATE_PUBLIC_KEY_CREDENTIAL_REQUEST);
        expectedData.putString(
                BUNDLE_KEY_REQUEST_JSON, requestJsonExpected);
        expectedData.putBoolean(
                BUNDLE_KEY_ALLOW_HYBRID, allowHybridExpected);

        CreatePublicKeyCredentialRequest request =
                new CreatePublicKeyCredentialRequest(requestJsonExpected, allowHybridExpected);

        assertThat(request.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(request.getCredentialData(), expectedData)).isTrue();
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(), expectedData)).isTrue();
        assertThat(request.getRequireSystemProvider()).isFalse();
    }

    @Test
    public void frameworkConversion_success() {
        CreatePublicKeyCredentialRequest request =
                new CreatePublicKeyCredentialRequest("json", true);

        CreateCredentialRequest convertedRequest = CreateCredentialRequest.createFrom(
                request.getType(), request.getCredentialData(),
                request.getCandidateQueryData(), request.getRequireSystemProvider()
        );

        assertThat(convertedRequest).isInstanceOf(CreatePublicKeyCredentialRequest.class);
        CreatePublicKeyCredentialRequest convertedSubclassRequest =
                (CreatePublicKeyCredentialRequest) convertedRequest;
        assertThat(convertedSubclassRequest.getRequestJson()).isEqualTo(request.getRequestJson());
        assertThat(convertedSubclassRequest.allowHybrid()).isEqualTo(request.allowHybrid());
    }
}
