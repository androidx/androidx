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

import android.content.Context
import android.os.Bundle
import androidx.credentials.assertEquals
import androidx.credentials.getTestCallingAppInfo
import androidx.credentials.internal.FrameworkClassParsingException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginCreatePublicKeyCredentialRequestTest {
    private val mContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java
        ) {
            BeginCreatePublicKeyCredentialRequest(
                "",
                getTestCallingAppInfo(mContext, "origin"),
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
                getTestCallingAppInfo(mContext, "origin"),
                Bundle()
            )
        }
    }

    @Test
    fun constructor_success() {
        BeginCreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            getTestCallingAppInfo(mContext, "origin"),
            Bundle()
        )
    }

    @Test
    fun constructorWithClientDataHash_success() {
        BeginCreatePublicKeyCredentialRequest(
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            getTestCallingAppInfo(mContext),
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
            getTestCallingAppInfo(mContext),
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
                getTestCallingAppInfo(mContext),
            )
        }
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val createPublicKeyCredentialReq =
            BeginCreatePublicKeyCredentialRequest(
                testJsonExpected,
                getTestCallingAppInfo(mContext),
                Bundle()
            )

        val testJsonActual = createPublicKeyCredentialReq.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
        assertThat(createPublicKeyCredentialReq.clientDataHash).isNull()
    }

    @Test
    fun getter_clientDataHash_success() {
        val testClientDataHashExpected = "client_data_hash".toByteArray()
        val createPublicKeyCredentialReq =
            BeginCreatePublicKeyCredentialRequest(
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                getTestCallingAppInfo(mContext),
                Bundle(),
                testClientDataHashExpected
            )

        val testClientDataHashActual = createPublicKeyCredentialReq.clientDataHash
        assertThat(testClientDataHashActual).isEqualTo(testClientDataHashExpected)
    }

    @Test
    fun conversion() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val req =
            BeginCreatePublicKeyCredentialRequest(
                testJsonExpected,
                getTestCallingAppInfo(ApplicationProvider.getApplicationContext(), "test"),
                Bundle()
            )

        val bundle = BeginCreateCredentialRequest.asBundle(req)
        assertThat(bundle).isNotNull()

        val converted = BeginCreateCredentialRequest.fromBundle(bundle)
        assertThat(converted).isInstanceOf(BeginCreatePublicKeyCredentialRequest::class.java)
        assertEquals(converted!!, req)
    }

    internal companion object {
        internal const val BUNDLE_KEY_CLIENT_DATA_HASH =
            "androidx.credentials.BUNDLE_KEY_CLIENT_DATA_HASH"
        internal const val BUNDLE_KEY_REQUEST_JSON = "androidx.credentials.BUNDLE_KEY_REQUEST_JSON"
    }
}
