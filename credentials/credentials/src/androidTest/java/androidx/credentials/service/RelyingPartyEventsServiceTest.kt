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

package androidx.credentials.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.CancellationSignal
import android.os.IBinder
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RelyingPartyEventsServiceTest {

    @Test
    fun onBind_nullIntent_returnsNull() {
        val service =
            TestRelyingPartyEventsService(allowedProviders = listOf(DummyProvider::class.java.name))
        assertThat(service.onBind(null)).isNull()
    }

    @Test
    fun onBind_wrongAction_returnsNull() {
        val service =
            TestRelyingPartyEventsService(allowedProviders = listOf(DummyProvider::class.java.name))
        assertThat(service.onBind(Intent("wrong.action"))).isNull()
    }

    @Test
    fun onBind_noProviderInManifest_returnsNull() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = TestRelyingPartyEventsService().apply { attachContext(context) }
        val intent =
            Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE).apply {
                putExtra(
                    RelyingPartyEventsStubProvider.RELYING_PARTY_EVENTS_SERVICE_PROVIDER_KEY,
                    DummyProvider::class.java.name,
                )
            }
        assertThat(service.onBind(intent)).isNull()
    }

    @Test
    fun onBind_classNotFound_returnsNull() {
        val service =
            TestRelyingPartyEventsService(
                allowedProviders = listOf("androidx.credentials.service.NonExistentProviderClass")
            )
        val intent = Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE)
        assertThat(service.onBind(intent)).isNull()
    }

    @Test
    fun onBind_incompatibleClass_returnsNull() {
        IncompatibleProvider.constructed = false
        val service =
            TestRelyingPartyEventsService(
                allowedProviders = listOf(IncompatibleProvider::class.java.name)
            )
        val intent = Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE)

        assertThat(service.onBind(intent)).isNull()
        assertThat(IncompatibleProvider.constructed).isFalse()
    }

    @Test
    fun onBind_multipleProviders_returnsNull() {
        val service =
            TestRelyingPartyEventsService(
                allowedProviders =
                    listOf(DummyProvider::class.java.name, SecondDummyProvider::class.java.name)
            )
        val intent = Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE)
        assertThat(service.onBind(intent)).isNull()
    }

    @Test
    fun onBind_validProvider_returnsBinder() {
        val service =
            TestRelyingPartyEventsService(allowedProviders = listOf(DummyProvider::class.java.name))
        val intent = Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE)
        assertThat(service.onBind(intent)).isNotNull()
    }

    @Test
    fun onBind_stubProviderThrows_returnsNull() {
        val service =
            TestRelyingPartyEventsService(
                allowedProviders = listOf(ThrowingProvider::class.java.name)
            )
        val intent = Intent(RelyingPartyEventsService.ACTION_RELYING_PARTY_EVENTS_SERVICE)
        assertThat(service.onBind(intent)).isNull()
    }

    @Test
    fun onCheckPublicKeyCredentialCreationEligibility_completesWithResponse() {
        val service = TestRelyingPartyEventsService(isEligible = true)
        var result: CheckPublicKeyCredentialCreationEligibilityResponse? = null

        service.onCheckPublicKeyCredentialCreationEligibility(
            CheckPublicKeyCredentialCreationEligibilityRequest(),
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    CheckPublicKeyCredentialCreationEligibilityResponse,
                    CheckPublicKeyCredentialCreationEligibilityException,
                > {
                override fun onResult(
                    response: CheckPublicKeyCredentialCreationEligibilityResponse
                ) {
                    result = response
                }

                override fun onError(error: CheckPublicKeyCredentialCreationEligibilityException) {}
            },
        )

        assertThat(result).isNotNull()
        assertThat(result!!.isEligible).isTrue()
    }

    @Test
    fun onCheckPublicKeyCredentialCreationEligibility_propagatesFailure() {
        val service = TestRelyingPartyEventsService(shouldFail = true)
        var error: CheckPublicKeyCredentialCreationEligibilityException? = null

        service.onCheckPublicKeyCredentialCreationEligibility(
            CheckPublicKeyCredentialCreationEligibilityRequest(),
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    CheckPublicKeyCredentialCreationEligibilityResponse,
                    CheckPublicKeyCredentialCreationEligibilityException,
                > {
                override fun onResult(
                    response: CheckPublicKeyCredentialCreationEligibilityResponse
                ) {}

                override fun onError(e: CheckPublicKeyCredentialCreationEligibilityException) {
                    error = e
                }
            },
        )

        assertThat(error)
            .isInstanceOf(CheckPublicKeyCredentialCreationEligibilityException::class.java)
        assertThat(error?.errorMessage).isEqualTo("eligibility check failed")
    }

    @Test
    fun onGetPublicKeyCredentialCreationRequest_completesWithCreationOptions() {
        val service = TestRelyingPartyEventsService()
        var result: GetPublicKeyCredentialCreationResponse? = null

        service.onGetPublicKeyCredentialCreationRequest(
            GetPublicKeyCredentialCreationRequest(),
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    GetPublicKeyCredentialCreationResponse,
                    GetPublicKeyCredentialCreationException,
                > {
                override fun onResult(response: GetPublicKeyCredentialCreationResponse) {
                    result = response
                }

                override fun onError(error: GetPublicKeyCredentialCreationException) {}
            },
        )

        assertThat(result).isNotNull()
        assertThat(result!!.createPublicKeyCredentialRequest.requestJson).isEqualTo(REQUEST_JSON)
    }

    @Test
    fun onGetPublicKeyCredentialCreationRequest_propagatesFailure() {
        val service = TestRelyingPartyEventsService(shouldFail = true)
        var error: GetPublicKeyCredentialCreationException? = null

        service.onGetPublicKeyCredentialCreationRequest(
            GetPublicKeyCredentialCreationRequest(),
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    GetPublicKeyCredentialCreationResponse,
                    GetPublicKeyCredentialCreationException,
                > {
                override fun onResult(response: GetPublicKeyCredentialCreationResponse) {}

                override fun onError(e: GetPublicKeyCredentialCreationException) {
                    error = e
                }
            },
        )

        assertThat(error).isInstanceOf(GetPublicKeyCredentialCreationException::class.java)
        assertThat(error?.errorMessage).isEqualTo("creation options failed")
    }

    @Test
    fun onPublicKeyCredentialCreationResult_exposesRegistrationResponseJson() {
        val service = TestRelyingPartyEventsService()
        val result =
            PublicKeyCredentialCreationResult(CreatePublicKeyCredentialResponse(RESPONSE_JSON))
        var succeeded = false

        service.onPublicKeyCredentialCreationResult(
            result,
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    PublicKeyCredentialCreationResponse,
                    PublicKeyCredentialCreationException,
                > {
                override fun onResult(response: PublicKeyCredentialCreationResponse) {
                    succeeded = true
                }

                override fun onError(error: PublicKeyCredentialCreationException) {}
            },
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.credential?.registrationResponseJson).isEqualTo(RESPONSE_JSON)
        assertThat(result.exception).isNull()
        assertThat(succeeded).isTrue()
    }

    @Test
    fun onPublicKeyCredentialCreationResult_exposesCreationException() {
        val service = TestRelyingPartyEventsService()
        val creationException = CreateCredentialUnknownException("provider creation failed")
        val result = PublicKeyCredentialCreationResult(creationException)
        var succeeded = false

        service.onPublicKeyCredentialCreationResult(
            result,
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    PublicKeyCredentialCreationResponse,
                    PublicKeyCredentialCreationException,
                > {
                override fun onResult(response: PublicKeyCredentialCreationResponse) {
                    succeeded = true
                }

                override fun onError(error: PublicKeyCredentialCreationException) {}
            },
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.credential).isNull()
        assertThat(result.exception).isSameInstanceAs(creationException)
        assertThat(succeeded).isTrue()
    }

    @Test
    fun onPublicKeyCredentialCreationResult_propagatesFailure() {
        val service = TestRelyingPartyEventsService(shouldFail = true)
        var error: PublicKeyCredentialCreationException? = null

        service.onPublicKeyCredentialCreationResult(
            PublicKeyCredentialCreationResult(CreatePublicKeyCredentialResponse(RESPONSE_JSON)),
            CancellationSignal(),
            object :
                OutcomeReceiverCompat<
                    PublicKeyCredentialCreationResponse,
                    PublicKeyCredentialCreationException,
                > {
                override fun onResult(response: PublicKeyCredentialCreationResponse) {}

                override fun onError(e: PublicKeyCredentialCreationException) {
                    error = e
                }
            },
        )

        assertThat(error).isInstanceOf(PublicKeyCredentialCreationException::class.java)
        assertThat(error?.errorMessage).isEqualTo("registration failed")
    }

    @Test
    fun dtoClasses_equalsHashCodeToString() {
        assertThat(CheckPublicKeyCredentialCreationEligibilityRequest())
            .isEqualTo(CheckPublicKeyCredentialCreationEligibilityRequest())
        assertThat(CheckPublicKeyCredentialCreationEligibilityRequest().hashCode())
            .isEqualTo(CheckPublicKeyCredentialCreationEligibilityRequest().hashCode())
        assertThat(CheckPublicKeyCredentialCreationEligibilityRequest().toString())
            .isEqualTo("CheckPublicKeyCredentialCreationEligibilityRequest()")

        val eligibleResp = CheckPublicKeyCredentialCreationEligibilityResponse(isEligible = true)
        val ineligibleResp = CheckPublicKeyCredentialCreationEligibilityResponse(isEligible = false)
        assertThat(eligibleResp)
            .isEqualTo(CheckPublicKeyCredentialCreationEligibilityResponse(isEligible = true))
        assertThat(eligibleResp).isNotEqualTo(ineligibleResp)
        assertThat(eligibleResp.hashCode())
            .isEqualTo(
                CheckPublicKeyCredentialCreationEligibilityResponse(isEligible = true).hashCode()
            )
        assertThat(eligibleResp.toString()).contains("isEligible=true")

        assertThat(GetPublicKeyCredentialCreationRequest())
            .isEqualTo(GetPublicKeyCredentialCreationRequest())
        assertThat(GetPublicKeyCredentialCreationRequest().hashCode())
            .isEqualTo(GetPublicKeyCredentialCreationRequest().hashCode())
        assertThat(GetPublicKeyCredentialCreationRequest().toString())
            .isEqualTo("GetPublicKeyCredentialCreationRequest()")

        val pkRequest = CreatePublicKeyCredentialRequest(REQUEST_JSON)
        val creationResp = GetPublicKeyCredentialCreationResponse(pkRequest)
        assertThat(creationResp).isEqualTo(GetPublicKeyCredentialCreationResponse(pkRequest))
        assertThat(creationResp.hashCode())
            .isEqualTo(GetPublicKeyCredentialCreationResponse(pkRequest).hashCode())
        assertThat(creationResp.toString()).contains("createPublicKeyCredentialRequest=")

        val pkResponse = CreatePublicKeyCredentialResponse(RESPONSE_JSON)
        val createdSuccess = PublicKeyCredentialCreationResult(pkResponse)
        val ex = CreateCredentialUnknownException("failed")
        val createdFailure = PublicKeyCredentialCreationResult(ex)
        assertThat(createdSuccess).isEqualTo(PublicKeyCredentialCreationResult(pkResponse))
        assertThat(createdFailure).isEqualTo(PublicKeyCredentialCreationResult(ex))
        assertThat(createdSuccess).isNotEqualTo(createdFailure)
        assertThat(createdSuccess.hashCode())
            .isEqualTo(PublicKeyCredentialCreationResult(pkResponse).hashCode())
        assertThat(createdSuccess.toString()).contains("credential=")

        assertThat(PublicKeyCredentialCreationResponse())
            .isEqualTo(PublicKeyCredentialCreationResponse())
        assertThat(PublicKeyCredentialCreationResponse().hashCode())
            .isEqualTo(PublicKeyCredentialCreationResponse().hashCode())
        assertThat(PublicKeyCredentialCreationResponse().toString())
            .isEqualTo("PublicKeyCredentialCreationResponse()")
    }

    private class TestRelyingPartyEventsService(
        private val isEligible: Boolean = false,
        private val shouldFail: Boolean = false,
        private val allowedProviders: List<String>? = null,
    ) : RelyingPartyEventsService() {

        fun attachContext(context: Context) {
            attachBaseContext(context)
        }

        override fun getAllowedProvidersFromManifest(): List<String> =
            allowedProviders ?: super.getAllowedProvidersFromManifest()

        override fun onCheckPublicKeyCredentialCreationEligibility(
            request: CheckPublicKeyCredentialCreationEligibilityRequest,
            cancellationSignal: CancellationSignal,
            callback:
                OutcomeReceiverCompat<
                    CheckPublicKeyCredentialCreationEligibilityResponse,
                    CheckPublicKeyCredentialCreationEligibilityException,
                >,
        ) {
            if (shouldFail) {
                callback.onError(
                    CheckPublicKeyCredentialCreationEligibilityException("eligibility check failed")
                )
            } else {
                callback.onResult(CheckPublicKeyCredentialCreationEligibilityResponse(isEligible))
            }
        }

        override fun onGetPublicKeyCredentialCreationRequest(
            request: GetPublicKeyCredentialCreationRequest,
            cancellationSignal: CancellationSignal,
            callback:
                OutcomeReceiverCompat<
                    GetPublicKeyCredentialCreationResponse,
                    GetPublicKeyCredentialCreationException,
                >,
        ) {
            if (shouldFail) {
                callback.onError(GetPublicKeyCredentialCreationException("creation options failed"))
            } else {
                callback.onResult(
                    GetPublicKeyCredentialCreationResponse(
                        CreatePublicKeyCredentialRequest(REQUEST_JSON)
                    )
                )
            }
        }

        override fun onPublicKeyCredentialCreationResult(
            result: PublicKeyCredentialCreationResult,
            cancellationSignal: CancellationSignal,
            callback:
                OutcomeReceiverCompat<
                    PublicKeyCredentialCreationResponse,
                    PublicKeyCredentialCreationException,
                >,
        ) {
            if (shouldFail) {
                callback.onError(PublicKeyCredentialCreationException("registration failed"))
            } else {
                callback.onResult(PublicKeyCredentialCreationResponse())
            }
        }
    }

    class DummyProvider : RelyingPartyEventsStubProvider {
        override fun getStubImplementation(service: RelyingPartyEventsService): IBinder? = Binder()
    }

    class SecondDummyProvider : RelyingPartyEventsStubProvider {
        override fun getStubImplementation(service: RelyingPartyEventsService): IBinder? = Binder()
    }

    class ThrowingProvider : RelyingPartyEventsStubProvider {
        override fun getStubImplementation(service: RelyingPartyEventsService): IBinder? =
            throw IllegalStateException("Stub could not be created")
    }

    /**
     * Has a correctly shaped `getStubImplementation` but does not implement
     * [RelyingPartyEventsStubProvider], so it must be refused without being constructed.
     */
    class IncompatibleProvider {
        init {
            constructed = true
        }

        @Suppress("UNUSED_PARAMETER")
        fun getStubImplementation(service: RelyingPartyEventsService): IBinder? = Binder()

        companion object {
            @Volatile var constructed = false
        }
    }

    private companion object {
        const val REQUEST_JSON =
            "{\"rp\":{\"name\":\"Example\",\"id\":\"example.com\"}," +
                "\"user\":{\"id\":\"dXNlcg\",\"name\":\"user\",\"displayName\":\"user\"}," +
                "\"challenge\":\"Y2hhbGxlbmdl\"," +
                "\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}]}"
        const val RESPONSE_JSON =
            "{\"id\":\"Y3JlZElk\",\"rawId\":\"Y3JlZElk\",\"type\":\"public-key\"," +
                "\"response\":{\"clientDataJSON\":\"e30\",\"attestationObject\":\"e30\"}}"
    }
}
