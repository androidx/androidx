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

package androidx.credentials.playservices.controllers.CreatePublicKeyCredential

import androidx.credentials.GetPublicKeyCredentialOption
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PublicKeyCredentialControllerUtilityTest {

  private val JSON_KEYS_TO_EXPECTED_VAULES =
    mapOf(
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_CLIENT_DATA to "clientDataJSON",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_ATTESTATION_OBJ to
        "attestationObject",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_AUTH_DATA to "authenticatorData",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_SIGNATURE to "signature",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_USER_HANDLE to "userHandle",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RESPONSE to "response",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_ID to "id",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RAW_ID to "rawId",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_TYPE to "type",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RPID to "rpId",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_CHALLENGE to "challenge",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_APPID to "appid",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_THIRD_PARTY_PAYMENT to
        "thirdPartyPayment",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_AUTH_SELECTION to
        "authenticatorSelection",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_REQUIRE_RES_KEY to
        "requireResidentKey",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RES_KEY to "residentKey",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_AUTH_ATTACHMENT to
        "authenticatorAttachment",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_TIMEOUT to "timeout",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_EXCLUDE_CREDENTIALS to
        "excludeCredentials",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_TRANSPORTS to "transports",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RP to "rp",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_NAME to "name",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_ICON to "icon",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_ALG to "alg",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_USER to "user",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_DISPLAY_NAME to "displayName",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_USER_VERIFICATION_METHOD to
        "userVerificationMethod",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_KEY_PROTECTION_TYPE to
        "keyProtectionType",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_MATCHER_PROTECTION_TYPE to
        "matcherProtectionType",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_EXTENSTIONS to "extensions",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_ATTESTATION to "attestation",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_PUB_KEY_CRED_PARAMS to
        "pubKeyCredParams",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_CLIENT_EXTENSION_RESULTS to
        "clientExtensionResults",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_CRED_PROPS to
          "credProps",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RK to
          "rk"
    )

  private val TEST_REQUEST_JSON = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

  private val TEST_DOM_EXCEPTION_PREFIX =
    "androidx.credentials.TYPE_GET_PUBLIC_KEY_CREDENTIAL_DOM_EXCEPTION/"

  private val ERROR_CODE_TO_TYPES =
    mapOf(
      ErrorCode.UNKNOWN_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_UNKNOWN_ERROR",
      ErrorCode.ABORT_ERR to TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_ABORT_ERROR",
      ErrorCode.ATTESTATION_NOT_PRIVATE_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_NOT_READABLE_ERROR",
      ErrorCode.CONSTRAINT_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_CONSTRAINT_ERROR",
      ErrorCode.DATA_ERR to TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_DATA_ERROR",
      ErrorCode.INVALID_STATE_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_INVALID_STATE_ERROR",
      ErrorCode.ENCODING_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_ENCODING_ERROR",
      ErrorCode.NETWORK_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_NETWORK_ERROR",
      ErrorCode.NOT_ALLOWED_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_NOT_ALLOWED_ERROR",
      ErrorCode.NOT_SUPPORTED_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_NOT_SUPPORTED_ERROR",
      ErrorCode.SECURITY_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_SECURITY_ERROR",
      ErrorCode.TIMEOUT_ERR to
        TEST_DOM_EXCEPTION_PREFIX + "androidx.credentials.TYPE_TIMEOUT_ERROR",
    )

  @Test
  fun verifyJsonKeyNamingHasNotChanged() {
    JSON_KEYS_TO_EXPECTED_VAULES.forEach { entry -> assertThat(entry.key).isEqualTo(entry.value) }
  }

  @Test
  fun verifyCheckAlgSupported() {
    assertThat(PublicKeyCredentialControllerUtility.Companion.checkAlgSupported(-7))
      .isTrue() // es-256
    assertThat(PublicKeyCredentialControllerUtility.Companion.checkAlgSupported(999999)).isFalse()
  }

  @Test
  fun convertToPlayAuthPasskeyJsonRequest_success() {
    val option = GetPublicKeyCredentialOption(TEST_REQUEST_JSON)
    val output =
      PublicKeyCredentialControllerUtility.Companion.convertToPlayAuthPasskeyJsonRequest(option)

    assertThat(output.isSupported()).isTrue()
    assertThat(output.getRequestJson()).isEqualTo(TEST_REQUEST_JSON)
  }

  @Test
  fun toAssertPasskeyResponse_authenticatorErrorResponse_success() {
    ERROR_CODE_TO_TYPES.forEach { entry ->
      var exception =
        PublicKeyCredentialControllerUtility.beginSignInPublicKeyCredentialResponseContainsError(
          entry.key,
          "test message"
        )
      assertThat(exception.type).isEqualTo(entry.value)
      assertThat(exception.errorMessage).isEqualTo("test message")
    }
  }

  @Test
  fun toAssertPasskeyResponse_authenticatorAssertionResponse_success() {
    val byteArrayClientDataJson = byteArrayOf(0x48, 101, 108, 108, 111)
    val byteArrayAuthenticatorData = byteArrayOf(0x48, 101, 108, 108, 112)
    val byteArraySignature = byteArrayOf(0x48, 101, 108, 108, 113)
    val byteArrayUserHandle = byteArrayOf(0x48, 101, 108, 108, 114)
    val json = JSONObject()
    val publicKeyCredId = "id"
    val publicKeyCredRawId = byteArrayOf(0x48, 101, 108, 108, 115)
    val publicKeyCredType = "type"
    val authenticatorAttachment = "platform"
    val hasClientExtensionOutputs = true
    val isDiscoverableCredential = true
    val expectedClientExtensions = "{\"credProps\":{\"rk\":true}}"

    PublicKeyCredentialControllerUtility.beginSignInAssertionResponse(
      byteArrayClientDataJson,
      byteArrayAuthenticatorData,
      byteArraySignature,
      byteArrayUserHandle,
      json,
      publicKeyCredId,
      publicKeyCredRawId,
      publicKeyCredType,
      authenticatorAttachment,
      hasClientExtensionOutputs,
      isDiscoverableCredential
    )

    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_ID))
      .isEqualTo(publicKeyCredId)
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_RAW_ID))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(publicKeyCredRawId))
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_TYPE))
      .isEqualTo(publicKeyCredType)
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_ATTACHMENT))
      .isEqualTo(authenticatorAttachment)
    assertThat(json.getJSONObject(PublicKeyCredentialControllerUtility
      .JSON_KEY_CLIENT_EXTENSION_RESULTS).toString()).isEqualTo(expectedClientExtensions)

    // There is some embedded JSON so we should make sure we test that.
    var embeddedResponse =
      json.getJSONObject(PublicKeyCredentialControllerUtility.JSON_KEY_RESPONSE)
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayClientDataJson))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayAuthenticatorData))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_SIGNATURE))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArraySignature))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_USER_HANDLE))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayUserHandle))

    // ClientExtensions are another group of embedded JSON
    var clientExtensions = json.getJSONObject(PublicKeyCredentialControllerUtility
      .JSON_KEY_CLIENT_EXTENSION_RESULTS)
    assertThat(clientExtensions.get(PublicKeyCredentialControllerUtility.JSON_KEY_CRED_PROPS))
      .isNotNull()
    assertThat(clientExtensions.getJSONObject(PublicKeyCredentialControllerUtility
      .JSON_KEY_CRED_PROPS).getBoolean(PublicKeyCredentialControllerUtility.JSON_KEY_RK)).isTrue()
  }

  fun toAssertPasskeyResponse_authenticatorAssertionResponse_noUserHandle_success() {
    val byteArrayClientDataJson = byteArrayOf(0x48, 101, 108, 108, 111)
    val byteArrayAuthenticatorData = byteArrayOf(0x48, 101, 108, 108, 112)
    val byteArraySignature = byteArrayOf(0x48, 101, 108, 108, 113)
    val json = JSONObject()
    val publicKeyCredId = "id"
    val publicKeyCredRawId = byteArrayOf(0x48, 101, 108, 108, 115)
    val publicKeyCredType = "type"
    val authenticatorAttachment = "platform"
    val hasClientExtensionOutputs = false

    PublicKeyCredentialControllerUtility.beginSignInAssertionResponse(
      byteArrayClientDataJson,
      byteArrayAuthenticatorData,
      byteArraySignature,
      null,
      json,
      publicKeyCredId,
      publicKeyCredRawId,
      publicKeyCredType,
      authenticatorAttachment,
      hasClientExtensionOutputs,
      null
    )

    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_ID))
      .isEqualTo(publicKeyCredId)
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_RAW_ID))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(publicKeyCredRawId))
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_TYPE))
      .isEqualTo(publicKeyCredType)
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_ATTACHMENT))
      .isEqualTo(authenticatorAttachment)
    assertThat(json.getJSONObject(PublicKeyCredentialControllerUtility
      .JSON_KEY_CLIENT_EXTENSION_RESULTS).toString()).isEqualTo(JSONObject().toString())

    // There is some embedded JSON so we should make sure we test that.
    var embeddedResponse =
      json.getJSONObject(PublicKeyCredentialControllerUtility.JSON_KEY_RESPONSE)
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayClientDataJson))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayAuthenticatorData))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_SIGNATURE))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArraySignature))
    assertThat(embeddedResponse.has(PublicKeyCredentialControllerUtility.JSON_KEY_USER_HANDLE))
      .isFalse()
  }

  fun toAssertPasskeyResponse_authenticatorAssertionResponse_noAuthenticatorAttachment_success() {
    val byteArrayClientDataJson = byteArrayOf(0x48, 101, 108, 108, 111)
    val byteArrayAuthenticatorData = byteArrayOf(0x48, 101, 108, 108, 112)
    val byteArraySignature = byteArrayOf(0x48, 101, 108, 108, 113)
    val json = JSONObject()
    val publicKeyCredId = "id"
    val publicKeyCredRawId = byteArrayOf(0x48, 101, 108, 108, 115)
    val publicKeyCredType = "type"
    val hasClientExtensionOutputs = false

    PublicKeyCredentialControllerUtility.beginSignInAssertionResponse(
      byteArrayClientDataJson,
      byteArrayAuthenticatorData,
      byteArraySignature,
      null,
      json,
      publicKeyCredId,
      publicKeyCredRawId,
      publicKeyCredType,
      null,
      hasClientExtensionOutputs,
      null
    )

    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_ID))
      .isEqualTo(publicKeyCredId)
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_RAW_ID))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(publicKeyCredRawId))
    assertThat(json.get(PublicKeyCredentialControllerUtility.JSON_KEY_TYPE))
      .isEqualTo(publicKeyCredType)
    assertThat(json.optJSONObject(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_ATTACHMENT))
      .isNull()
    assertThat(json.getJSONObject(PublicKeyCredentialControllerUtility
      .JSON_KEY_CLIENT_EXTENSION_RESULTS).toString()).isEqualTo(JSONObject().toString())

    // There is some embedded JSON so we should make sure we test that.
    var embeddedResponse =
      json.getJSONObject(PublicKeyCredentialControllerUtility.JSON_KEY_RESPONSE)
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayClientDataJson))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_AUTH_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayAuthenticatorData))
    assertThat(embeddedResponse.get(PublicKeyCredentialControllerUtility.JSON_KEY_SIGNATURE))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArraySignature))
    assertThat(embeddedResponse.has(PublicKeyCredentialControllerUtility.JSON_KEY_USER_HANDLE))
      .isFalse()
  }
}
