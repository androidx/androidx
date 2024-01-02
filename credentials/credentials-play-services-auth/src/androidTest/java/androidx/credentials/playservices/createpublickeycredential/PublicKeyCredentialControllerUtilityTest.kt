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
import androidx.testutils.assertThrows
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONException
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
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_CRED_PROPS to "credProps",
      PublicKeyCredentialControllerUtility.Companion.JSON_KEY_RK to "rk"
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
  fun toCreatePasskeyResponseJson_addAuthenticatorAttestationResponse_success() {
    val json = JSONObject()
    val byteArrayClientDataJson = byteArrayOf(0x48, 101, 108, 108, 111)
    val byteArrayAttestationObject = byteArrayOf(0x48, 101, 108, 108, 112)
    var transportArray = arrayOf("transport")

    PublicKeyCredentialControllerUtility.addAuthenticatorAttestationResponse(
      byteArrayClientDataJson,
      byteArrayAttestationObject,
      transportArray,
      json
    )

    var response = json.getJSONObject(PublicKeyCredentialControllerUtility.JSON_KEY_RESPONSE)

    assertThat(response.get(PublicKeyCredentialControllerUtility.JSON_KEY_CLIENT_DATA))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayClientDataJson))
    assertThat(response.get(PublicKeyCredentialControllerUtility.JSON_KEY_ATTESTATION_OBJ))
      .isEqualTo(PublicKeyCredentialControllerUtility.b64Encode(byteArrayAttestationObject))
    assertThat(response.get(PublicKeyCredentialControllerUtility.JSON_KEY_TRANSPORTS))
      .isEqualTo(JSONArray(transportArray))
  }

  @Test
  fun convertJSON_requiredFields_success() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )
    var output = PublicKeyCredentialControllerUtility.convertJSON(json)

    assertThat(output.user.id).isNotEmpty()
    assertThat(output.user.name).isEqualTo("Name of User")
    assertThat(output.user.displayName).isEqualTo("Display Name of User")
    assertThat(output.user.icon).isEqualTo("icon.png")
    assertThat(output.challenge).isNotEmpty()
    assertThat(output.rp.id).isNotEmpty()
    assertThat(output.rp.name).isEqualTo("Name of RP")
    assertThat(output.rp.icon).isEqualTo("rpicon.png")
    assertThat(output.parameters[0].algorithmIdAsInteger).isEqualTo(-7)
    assertThat(output.parameters[0].typeAsString).isEqualTo("public-key")
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingRpId() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingRpName() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingRp() {
    var json =
      JSONObject(
        "{" +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingPubKeyCredParams() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingChallenge() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingUser() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingUserId() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingUserName() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_requiredFields_failOnMissingUserDisplayName() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )

    assertThrows<JSONException> { PublicKeyCredentialControllerUtility.convertJSON(json) }
  }

  @Test
  fun convertJSON_optionalFields_extensions_success() {
    var json =
      JSONObject(
        "{" +
          "\"rp\": {" +
          "\"id\": \"rpidvalue\"," +
          "\"name\": \"Name of RP\"," +
          "\"icon\": \"rpicon.png\"" +
          "}," +
          "\"extensions\": {" +
          "\"appid\": \"https://www.android.com/appid1\"," +
          "\"uvm\": true" +
          "}," +
          "\"pubKeyCredParams\": [{" +
          "\"alg\": -7," +
          "\"type\": \"public-key\"" +
          "}]," +
          "\"challenge\": \"dGVzdA==\"," +
          "\"user\": {" +
          "\"id\": \"idvalue\"," +
          "\"name\": \"Name of User\"," +
          "\"displayName\": \"Display Name of User\"," +
          "\"icon\": \"icon.png\"" +
          "}" +
          "}"
      )
    var output = PublicKeyCredentialControllerUtility.convertJSON(json)

    assertThat(output.authenticationExtensions!!.fidoAppIdExtension!!.appId)
      .isEqualTo("https://www.android.com/appid1")
    assertThat(output.authenticationExtensions!!.userVerificationMethodExtension!!.uvm).isTrue()
  }
}
