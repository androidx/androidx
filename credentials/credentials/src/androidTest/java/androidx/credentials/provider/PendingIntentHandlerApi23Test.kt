/*
 * Copyright 2024 The Android Open Source Project
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

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.Bundle
import androidx.credentials.CreateCredentialRequest.Companion.createFrom
import androidx.credentials.CreateCustomCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.assertEquals
import androidx.credentials.equals
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.domerrors.ConstraintError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.credentials.getTestCallingAppInfo
import androidx.credentials.internal.getFinalCreateCredentialData
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.extractBeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.extractGetCredentialException
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.extractGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.setBeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.setProviderCreateCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Api23Impl.Companion.setProviderGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Companion.retrieveBeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Companion.retrieveProviderCreateCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Companion.retrieveProviderGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler.Companion.setBeginGetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler.Companion.setCreateCredentialException
import androidx.credentials.provider.PendingIntentHandler.Companion.setCreateCredentialResponse
import androidx.credentials.provider.PendingIntentHandler.Companion.setGetCredentialException
import androidx.credentials.provider.PendingIntentHandler.Companion.setGetCredentialResponse
import androidx.credentials.provider.ui.UiUtils.Companion.constructActionEntry
import androidx.credentials.provider.ui.UiUtils.Companion.constructAuthenticationActionEntry
import androidx.credentials.provider.ui.UiUtils.Companion.constructPasswordCredentialEntryDefault
import androidx.credentials.provider.ui.UiUtils.Companion.constructRemoteEntry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(maxSdkVersion = 33)
@Suppress("deprecation")
class PendingIntentHandlerApi23Test {
    private val mContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Throws(Exception::class)
    fun retrieveProviderCreateCredentialRequest_success() {
        val callingRequest =
            CreatePasswordRequest(
                "id",
                "password",
                "origin",
                /* preferImmediatelyAvailableCredentials= */ true,
                /* isAutoSelectAllowed= */ true,
            )
        val expectedRequest =
            ProviderCreateCredentialRequest(
                createFrom(
                    callingRequest.type,
                    getFinalCreateCredentialData(callingRequest, mContext),
                    callingRequest.candidateQueryData,
                    callingRequest.isSystemProviderRequired,
                    callingRequest.origin,
                ),
                getTestCallingAppInfo(callingRequest.origin),
            )
        val intent = Intent()
        setProviderCreateCredentialRequest(intent, expectedRequest)

        val actualRequest = retrieveProviderCreateCredentialRequest(intent)!!

        assertEquals(mContext, actualRequest, expectedRequest)
    }

    @Test
    @Throws(Exception::class)
    fun retrieveProviderCreateCredentialRequest_emptyIntent_returnsNull() {
        val intent = Intent()

        val actualRequest = retrieveProviderCreateCredentialRequest(intent)

        assertThat(actualRequest).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun retrieveProviderCreateCredentialRequest_invalidDataInIntent_returnsNull() {
        val expected = GetPublicKeyCredentialDomException(NotAllowedError(), "Error msg")
        val intent = Intent()
        setGetCredentialException(intent, expected)

        val actualRequest = retrieveProviderCreateCredentialRequest(intent)

        assertThat(actualRequest).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun setBeginGetCredentialResponse_success() {
        val option = GetPublicKeyCredentialOption(TEST_JSON, "client_data_hash".toByteArray())
        val responseBuilder =
            BeginGetCredentialResponse.Builder()
                .addCredentialEntry(constructPasswordCredentialEntryDefault("pwd-username"))
                .addCredentialEntry(
                    PublicKeyCredentialEntry(
                        mContext,
                        "username",
                        PendingIntent.getActivity(
                            mContext,
                            0,
                            Intent(),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                        BeginGetPublicKeyCredentialOption(
                            option.candidateQueryData,
                            "id",
                            option.requestJson,
                            option.clientDataHash,
                        ),
                        "displayname",
                        null,
                        ICON,
                        true,
                        true,
                    )
                )
                .addAction(constructActionEntry("action-title-1", "subtitle"))
                .addAction(constructActionEntry("action-title-2", null))
                .addAuthenticationAction(constructAuthenticationActionEntry("auth-title"))
                .setRemoteEntry(constructRemoteEntry())
        if (Build.VERSION.SDK_INT >= 26) {
            responseBuilder.addCredentialEntry(
                CustomCredentialEntry(
                    mContext,
                    "title",
                    PendingIntent.getActivity(mContext, 0, Intent(), PendingIntent.FLAG_IMMUTABLE),
                    BeginGetCustomCredentialOption("id", "custom-type", Bundle()),
                    null,
                    null,
                    Instant.now(),
                    ICON,
                    false,
                    "entry-group-id",
                    false,
                )
            )
        }
        val response = responseBuilder.build()
        val intent = Intent()

        setBeginGetCredentialResponse(intent, response)

        val actual: BeginGetCredentialResponse = extractBeginGetCredentialResponse(intent)!!
        assertEquals(mContext, actual, response)
    }

    @Test
    @Throws(Exception::class)
    fun retrieveBeginGetCredentialRequest_withoutCallingAppInfo_success() {
        val intent = Intent()
        val pwdOption1 = GetPasswordOption()
        val pwdOption2 =
            GetPasswordOption(setOf("uid1", "uid2"), true, setOf(ComponentName("pkg1", "cls1")))
        val passkeyOption = GetPublicKeyCredentialOption(TEST_JSON, "hash".toByteArray())
        val customQueryData = Bundle()
        customQueryData.putInt("key1", 1)
        customQueryData.putBinder("key2", Binder())
        val customOption =
            GetCustomCredentialOption("custom_type", customQueryData, customQueryData, false)
        val expectedRequest =
            BeginGetCredentialRequest(
                listOf(
                    BeginGetPasswordOption(
                        pwdOption1.allowedUserIds,
                        pwdOption1.candidateQueryData,
                        "id-1",
                    ),
                    BeginGetPasswordOption(
                        pwdOption2.allowedUserIds,
                        pwdOption2.candidateQueryData,
                        "id-2",
                    ),
                    BeginGetPublicKeyCredentialOption(
                        passkeyOption.candidateQueryData,
                        "id-3",
                        passkeyOption.requestJson,
                        passkeyOption.clientDataHash,
                    ),
                    BeginGetCustomCredentialOption(
                        "id-4",
                        customOption.type,
                        customOption.candidateQueryData,
                    ),
                )
            )
        setBeginGetCredentialRequest(intent, expectedRequest)

        val actual = retrieveBeginGetCredentialRequest(intent)!!

        assertEquals(actual, expectedRequest)
    }

    @Test
    @Throws(Exception::class)
    fun retrieveBeginGetCredentialRequest_withCallingAppInfo_success() {
        val intent = Intent()
        val pwdOption = GetPasswordOption()
        val passkeyOption = GetPublicKeyCredentialOption(TEST_JSON)
        val customQueryData = Bundle()
        customQueryData.putInt("key1", 1)
        customQueryData.putBinder("key2", Binder())
        val customOption =
            GetCustomCredentialOption("custom_type", customQueryData, customQueryData, false)
        val expectedRequest =
            BeginGetCredentialRequest(
                listOf(
                    BeginGetPasswordOption(
                        pwdOption.allowedUserIds,
                        pwdOption.candidateQueryData,
                        "id-1",
                    ),
                    BeginGetPublicKeyCredentialOption(
                        passkeyOption.candidateQueryData,
                        "id-3",
                        passkeyOption.requestJson,
                        passkeyOption.clientDataHash,
                    ),
                    BeginGetCustomCredentialOption(
                        "id-4",
                        customOption.type,
                        customOption.candidateQueryData,
                    ),
                ),
                getTestCallingAppInfo(null),
            )
        setBeginGetCredentialRequest(intent, expectedRequest)

        val actual = retrieveBeginGetCredentialRequest(intent)!!

        assertEquals(actual, expectedRequest)
    }

    @Test
    @Throws(Exception::class)
    fun retrieveBeginGetCredentialRequest_emptyIntent_returnsNull() {
        val intent = Intent()

        val actualRequest = retrieveBeginGetCredentialRequest(intent)

        assertThat(actualRequest).isNull()
    }

    @Test
    fun setCreateCredentialResponse_passkeyResponse_success() {
        val intent = Intent()
        val expected = CreatePublicKeyCredentialResponse(TEST_JSON)

        setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun setCreateCredentialResponse_passwordResponse_success() {
        val intent = Intent()
        val expected = CreatePasswordResponse()

        setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun setCreateCredentialResponse_customResponse_success() {
        val intent = Intent()
        val customData = Bundle()
        customData.putString("k1", "text")
        customData.putBinder("k2", Binder())
        val expected = CreateCustomCredentialResponse("type", customData)

        setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun retrieveCreateCredentialResponse_emptyResponse_returnsNull() {
        val actual = PendingIntentHandler.retrieveCreateCredentialResponse("type", Intent())

        assertThat(actual).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun retrieveProviderGetCredentialRequest_success() {
        val intent = Intent()
        val pwdOption1 = GetPasswordOption()
        val pwdOption2 =
            GetPasswordOption(setOf("uid1", "uid2"), true, setOf(ComponentName("pkg1", "cls1")))
        val passkeyOption = GetPublicKeyCredentialOption(TEST_JSON, "hash".toByteArray())
        val customQueryData = Bundle()
        customQueryData.putInt("key1", 1)
        customQueryData.putBinder("key2", Binder())
        val customOption =
            GetCustomCredentialOption("custom_type", customQueryData, customQueryData, false)
        val expectedRequest =
            ProviderGetCredentialRequest(
                listOf(pwdOption1, pwdOption2, passkeyOption, customOption),
                getTestCallingAppInfo("origin"),
            )
        setProviderGetCredentialRequest(intent, expectedRequest)

        val actual = retrieveProviderGetCredentialRequest(intent)!!

        assertEquals(actual, expectedRequest)
    }

    @Test
    @Throws(Exception::class)
    fun retrieveProviderGetCredentialRequest_emptyIntent_returnsNull() {
        val intent = Intent()

        val actualRequest = retrieveProviderGetCredentialRequest(intent)

        assertThat(actualRequest).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun setGetCredentialResponse_passwordCredential_success() {
        val cred: Credential = PasswordCredential("username", "pwd")
        val expected = GetCredentialResponse(cred)
        val intent = Intent()

        setGetCredentialResponse(intent, expected)

        val actual: GetCredentialResponse = extractGetCredentialResponse(intent)!!
        equals(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun setGetCredentialResponse_passkeyCredential_success() {
        val cred: Credential = PublicKeyCredential(TEST_JSON)
        val expected = GetCredentialResponse(cred)
        val intent = Intent()

        setGetCredentialResponse(intent, expected)

        val actual: GetCredentialResponse = extractGetCredentialResponse(intent)!!
        equals(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun setGetCredentialResponse_customCredential_success() {
        val customData = Bundle()
        customData.putString("k1", "text")
        customData.putBinder("k2", Binder())
        val cred: Credential = CustomCredential("test type", customData)
        val expected = GetCredentialResponse(cred)
        val intent = Intent()

        setGetCredentialResponse(intent, expected)

        val actual: GetCredentialResponse = extractGetCredentialResponse(intent)!!
        equals(actual, expected)
    }

    @Test
    @Throws(Exception::class)
    fun setGetCredentialException_success() {
        val expected = GetPublicKeyCredentialDomException(NotAllowedError(), "Error msg")
        val intent = Intent()

        setGetCredentialException(intent, expected)

        val actual: GetCredentialException = extractGetCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected.javaClass)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
    }

    @Test
    fun setCreateCredentialException_success() {
        val expected = CreateCredentialInterruptedException("Error msg")
        val intent = Intent()

        setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
    }

    @Test
    fun retrieveCreateCredentialException_success() {
        val expected = CreatePublicKeyCredentialDomException(ConstraintError(), "Error msg")
        val intent = Intent()

        setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
        val actualConverted = actual as CreatePublicKeyCredentialDomException
        assertThat(actualConverted.domError).isInstanceOf((expected.domError)::class.java)
    }

    @Test
    fun retrieveCreateCredentialException_emptyIntent_returnsNull() {
        val actual = PendingIntentHandler.retrieveCreateCredentialException(Intent())

        assertThat(actual).isNull()
    }

    companion object {
        private val ICON =
            Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        private const val TEST_JSON = "{\"test_key\":\"test_value\"}"
    }
}
