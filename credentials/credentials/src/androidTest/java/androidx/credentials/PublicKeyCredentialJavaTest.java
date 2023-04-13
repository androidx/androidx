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

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Note that "PublicKeyCredential" and "Passkey" are used interchangeably.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PublicKeyCredentialJavaTest {

    @Test
    public void typeConstant() {
        assertThat(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
                .isEqualTo("androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL");
    }

    @Test
    public void constructor_emptyJson_throwsIllegalArgumentException() {
        assertThrows("Expected empty Json to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new PublicKeyCredential("")
        );
    }

    @Test
    public void constructor_nullJson_throwsNullPointerException() {
        assertThrows("Expected null Json to throw NullPointerException",
                NullPointerException.class,
                () -> new PublicKeyCredential(null)
        );
    }

    @Test
    public void constructor_success() {
        new PublicKeyCredential(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}");
    }

    @Test
    public void getter_authJson_success() {
        String testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        PublicKeyCredential publicKeyCredential = new PublicKeyCredential(testJsonExpected);
        String testJsonActual = publicKeyCredential.getAuthenticationResponseJson();
        assertThat(testJsonActual).isEqualTo(testJsonExpected);
    }

    @Test
    public void getter_frameworkProperties() {
        String jsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}";
        Bundle expectedData = new Bundle();
        expectedData.putString(
                PublicKeyCredential.BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON, jsonExpected);

        PublicKeyCredential publicKeyCredential = new PublicKeyCredential(jsonExpected);

        assertThat(publicKeyCredential.getType()).isEqualTo(
                PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertThat(TestUtilsKt.equals(publicKeyCredential.getData(), expectedData)).isTrue();
    }

    @Test
    public void frameworkConversion_success() {
        PublicKeyCredential credential = new PublicKeyCredential("json");

        Credential convertedCredential = Credential.createFrom(
                credential.getType(), credential.getData());

        assertThat(convertedCredential).isInstanceOf(PublicKeyCredential.class);
        PublicKeyCredential convertedSubclassCredential = (PublicKeyCredential) convertedCredential;
        assertThat(convertedSubclassCredential.getAuthenticationResponseJson())
                .isEqualTo(credential.getAuthenticationResponseJson());
    }

    @Test
    public void staticProperty_hasCorrectTypeConstantValue() {
        String typeExpected = "androidx.credentials.TYPE_PUBLIC_KEY_CREDENTIAL";
        String typeActual = PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL;
        assertThat(typeActual).isEqualTo(typeExpected);
    }
}
