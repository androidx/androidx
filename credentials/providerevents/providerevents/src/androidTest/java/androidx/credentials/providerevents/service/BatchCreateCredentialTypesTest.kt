/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.providerevents.service

import android.content.pm.SigningInfo
import android.os.Bundle
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.provider.BeginCreateCustomCredentialRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 28)
@RunWith(AndroidJUnit4::class)
@SmallTest
class BatchCreateCredentialTypesTest {

    @Test
    fun providerBatchCreateCredentialRequest_copiesRequests() {
        val requests = mutableListOf(providerRequest(RP_PACKAGE_1), providerRequest(RP_PACKAGE_2))

        val batch = ProviderBatchCreateCredentialRequest(requests)
        requests.clear()

        assertThat(batch.requests.map { it.callingAppInfo.packageName })
            .containsExactly(RP_PACKAGE_1, RP_PACKAGE_2)
            .inOrder()
    }

    @Test
    fun providerBatchCreateCredentialRequest_duplicatePackageNames_throws() {
        assertThrows<IllegalArgumentException> {
                ProviderBatchCreateCredentialRequest(
                    listOf(providerRequest(RP_PACKAGE_1), providerRequest(RP_PACKAGE_1))
                )
            }
            .hasMessageThat()
            .contains("Duplicate package name")
    }

    @Test
    fun beginBatchCreateCredentialRequest_copiesRequests() {
        val requests = mutableListOf(beginRequest(RP_PACKAGE_1), beginRequest(RP_PACKAGE_2))

        val batch = BeginBatchCreateCredentialRequest(requests)
        requests.clear()

        assertThat(batch.requests.map { it.callingAppInfo!!.packageName })
            .containsExactly(RP_PACKAGE_1, RP_PACKAGE_2)
            .inOrder()
    }

    @Test
    fun beginBatchCreateCredentialRequest_missingCallingAppInfo_throws() {
        assertThrows<IllegalArgumentException> {
            BeginBatchCreateCredentialRequest(
                listOf(
                    BeginCreateCustomCredentialRequest(
                        PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
                        Bundle(),
                        null,
                    )
                )
            )
        }
    }

    @Test
    fun beginBatchCreateCredentialRequest_duplicatePackageNames_throws() {
        assertThrows<IllegalArgumentException> {
                BeginBatchCreateCredentialRequest(
                    listOf(beginRequest(RP_PACKAGE_1), beginRequest(RP_PACKAGE_1))
                )
            }
            .hasMessageThat()
            .contains("Duplicate package name")
    }

    @Test
    fun batchCreateCredentialResponse_results() {
        val success =
            CreateCredentialResult(
                RP_PACKAGE_1,
                CreatePublicKeyCredentialResponse(REGISTRATION_RESPONSE_JSON),
            )
        val failure = CreateCredentialResult(RP_PACKAGE_2, CreateCredentialUnknownException())
        val results = mutableListOf(success, failure)

        val response = BatchCreateCredentialResponse(results)
        results.clear()

        assertThat(response.results).containsExactly(success, failure).inOrder()
        assertThat(success.response).isNotNull()
        assertThat(success.exception).isNull()
        assertThat(failure.exception).isNotNull()
        assertThat(failure.response).isNull()
    }

    @Test
    fun batchCreateCredentialResponse_duplicatePackageNames_throws() {
        assertThrows<IllegalArgumentException> {
                BatchCreateCredentialResponse(
                    listOf(
                        CreateCredentialResult(
                            RP_PACKAGE_1,
                            CreatePublicKeyCredentialResponse(REGISTRATION_RESPONSE_JSON),
                        ),
                        CreateCredentialResult(RP_PACKAGE_1, CreateCredentialUnknownException()),
                    )
                )
            }
            .hasMessageThat()
            .contains("Duplicate package name")
    }

    @Test
    fun createCredentialResult_emptyPackageName_throws() {
        assertThrows<IllegalArgumentException> {
            CreateCredentialResult("", CreateCredentialUnknownException())
        }
    }

    private fun providerRequest(packageName: String): ProviderCreateCredentialRequest =
        ProviderCreateCredentialRequest(
            callingRequest = CreatePasswordRequest("id", "password"),
            callingAppInfo = CallingAppInfo(packageName, SigningInfo()),
        )

    private fun beginRequest(packageName: String): BeginCreateCustomCredentialRequest =
        BeginCreateCustomCredentialRequest(
            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
            Bundle(),
            CallingAppInfo(packageName, SigningInfo()),
        )

    private companion object {
        const val RP_PACKAGE_1 = "com.example.rp1"
        const val RP_PACKAGE_2 = "com.example.rp2"
        const val REGISTRATION_RESPONSE_JSON = "{\"id\":\"credential-id\"}"
    }
}
