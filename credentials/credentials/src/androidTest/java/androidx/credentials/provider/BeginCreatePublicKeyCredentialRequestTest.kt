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
package androidx.credentials.provider

import android.content.pm.SigningInfo
import android.os.Bundle
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
class BeginCreatePublicKeyCredentialRequestTest {
    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            BeginCreatePublicKeyCredentialRequest(
                "",
                CallingAppInfo(
                    "sample_package_name",
                    SigningInfo()
                ),
                Bundle()
            )
        }
    }

    @Test
    fun constructor_invalidJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected invalid Json to throw error",
            IllegalArgumentException::class.java
        ) {
            BeginCreatePublicKeyCredentialRequest(
                "invalid",
                CallingAppInfo(
                    "sample_package_name",
                    SigningInfo()
                ),
                Bundle()
            )
        }
    }

    @Test
    fun constructor_success() {
        BeginCreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            ),
            Bundle()
        )
    }

    @Test
    fun constructorWithClientDataHash_success() {
        BeginCreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            ),
            Bundle(),
            "client_data_hash".toByteArray()
        )
    }

    @Test
    fun constructor_success_createFrom() {
        val bundle = Bundle()
        bundle.putString(BUNDLE_KEY_REQUEST_JSON, "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}")
        bundle.putByteArray(BUNDLE_KEY_CLIENT_DATA_HASH, byteArrayOf())

        BeginCreatePublicKeyCredentialRequest.createForTest(
            bundle,
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            )
        )
    }

    @Test
    fun constructor_error_createFrom() {
        Assert.assertThrows(
            "Expected create from to throw error",
            FrameworkClassParsingException::class.java
        ) {
            BeginCreatePublicKeyCredentialRequest.createForTest(
                Bundle(),
                CallingAppInfo(
                    "sample_package_name", SigningInfo()
                )
            )
        }
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val createPublicKeyCredentialReq = BeginCreatePublicKeyCredentialRequest(
            testJsonExpected,
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            ),
            Bundle()
        )

        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
        assertThat(createPublicKeyCredentialReq.clientDataHash).isNull()
    }

    @Test
    fun getter_clientDataHash_success() {
        val testClientDataHashExpected = "client_data_hash".toByteArray()
        val createPublicKeyCredentialReq = BeginCreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            ),
            Bundle(),
            testClientDataHashExpected
        )

        val testClientDataHashActual = createPublicKeyCredentialReq.clientDataHash
        assertThat(testClientDataHashActual).isEqualTo(testClientDataHashExpected)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun conversion() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val req = BeginCreatePublicKeyCredentialRequest(
            testJsonExpected,
            CallingAppInfo(
                "sample_package_name", SigningInfo()
            ),
            Bundle()
        )

        val bundle = BeginCreateCredentialRequest.asBundle(req)
        assertThat(bundle).isNotNull()

        var converted = BeginCreateCredentialRequest.fromBundle(bundle)
        assertThat(req.type).isEqualTo(converted!!.type)
    }

    internal companion object {
        internal const val BUNDLE_KEY_CLIENT_DATA_HASH =
            "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH"
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
    }
}
