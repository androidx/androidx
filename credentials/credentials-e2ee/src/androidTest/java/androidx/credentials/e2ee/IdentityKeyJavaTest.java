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

package androidx.credentials.e2ee;

import static com.google.common.truth.Truth.assertThat;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.common.io.BaseEncoding;

import org.junit.Test;

import java.util.Random;

/* Same tests as in IdentityKeyTest.kt. Used to assert Java interop. */
public class IdentityKeyJavaTest {
    Random mRandom = new Random();

    @NonNull
    private byte[] randBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        mRandom.nextBytes(bytes);
        return bytes;
    }

    private String hexEncode(byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }

    @Test
    public void identityKeyWithFixedInputs_mustProduceExpectedOutput() {
        byte[] prf = new byte[32];
        byte[] salt = new byte[32];
        // with an all-zero PRF and salt, this is the expected key
        String expectedPrivKeyHex =
                "df7204546f1bee78b85324a7898ca119b387e01386d1aef037781d4a8a036aee";
        String expectedPubKeyHex =
                "ba33d523fd7bf0d06ce9298c3440be1bea3748c6270ae3e07ae8ea19abb8ed23";

        IdentityKey identityKey = IdentityKey.createFromPrf(prf, salt,
                IdentityKey.IDENTITY_KEY_TYPE_ED25519);

        assertThat(identityKey.getPrivate()).isNotNull();
        assertThat(identityKey.getPublic()).isNotNull();
        assertThat(hexEncode(identityKey.getPrivate())).isEqualTo(expectedPrivKeyHex);
        assertThat(hexEncode(identityKey.getPublic())).isEqualTo(expectedPubKeyHex);
    }


    @Test
    public void identityKeyWithoutSalt_mustBeIdenticalToEmptySalt() {
        for (int i = 0; i < 10; i++) {
            byte[] prf = randBytes(32);
            IdentityKey identityKey = IdentityKey.createFromPrf(prf, /* salt= */null,
                    IdentityKey.IDENTITY_KEY_TYPE_ED25519);
            IdentityKey identityKey2 = IdentityKey.createFromPrf(prf, new byte[32],
                    IdentityKey.IDENTITY_KEY_TYPE_ED25519);

            assertThat(identityKey).isEqualTo(identityKey2);
        }
    }

    @Test
    public void identityKey_canBeGeneratedUsingWebAuthnPrfOutput() {
        /*
        Ideally, we would test the full webauthn interaction (set the PRF extension to true, call
        navigator.credentials.create, read the PRF output). The problem is that this would tie
        androidX to the implementation of a password manager.
        Instead, we manually copy the prfOutput value from
        //com/google/android/gms/fido/authenticator/embedded/AuthenticationRequestHandlerTest.java,
        like a test vector. Even if the two values get out of sync, what we care about is the Base64
        format, as the PRF output is fully random-looking by definition.
         */
        byte[] prfOutput = Base64.decode("f2HM0TolWHyYJ/+LQDW8N2vRdE0+risMV/tIKXQdj7tVKdGChdJuMyz1"
                + "/iX7x4y3GvHLlmja1A8qCsKsekW22Q==", Base64.DEFAULT);
        byte[] salt = new byte[32];
        String expectedPrivKeyHex =
                "bccdec572ae1be6b3c3f3473781965a1935d2614c928f5430b79188950658ad6";
        String expectedPubKeyHex =
                "23fa91da0af9edefae9c53c584f933f3d02f934aebddb70511adac91f255afda";

        IdentityKey identityKey = IdentityKey.createFromPrf(prfOutput, salt,
                IdentityKey.IDENTITY_KEY_TYPE_ED25519);

        assertThat(prfOutput).isNotNull();
        assertThat(identityKey.getPrivate()).isNotNull();
        assertThat(identityKey.getPublic()).isNotNull();
        assertThat(hexEncode(identityKey.getPrivate())).isEqualTo(expectedPrivKeyHex);
        assertThat(hexEncode(identityKey.getPublic())).isEqualTo(expectedPubKeyHex);
    }
}
