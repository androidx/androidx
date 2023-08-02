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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
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
        "pubKeyCredParams"
    )

  @Test
  fun verifyJsonKeyNamingHasNotChanged() {
    JSON_KEYS_TO_EXPECTED_VAULES.forEach { entry -> assertThat(entry.key).isEqualTo(entry.value) }
  }
}
