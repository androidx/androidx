/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.openid4vp

import android.graphics.Bitmap
import androidx.credentials.registry.digitalcredentials.mdoc.MdocEntry
import androidx.credentials.registry.digitalcredentials.mdoc.MdocField
import androidx.credentials.registry.digitalcredentials.mdoc.MdocInlineIssuanceEntry
import androidx.credentials.registry.digitalcredentials.openid4vp.OpenId4VpDefaults.DEFAULT_MATCHER
import androidx.credentials.registry.digitalcredentials.sdjwt.SdJwtClaim
import androidx.credentials.registry.digitalcredentials.sdjwt.SdJwtEntry
import androidx.credentials.registry.digitalcredentials.sdjwt.SdJwtInlineIssuanceEntry
import androidx.credentials.registry.provider.RegistryManager
import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry
import androidx.credentials.registry.provider.digitalcredentials.VerificationEntryDisplayProperties
import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class OpenId4VpRegistryTest {

    private companion object {
        private val TEST_ICON_1 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        private val TEST_ICON_2 = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_4444)
        private val TEST_ICON_3 = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_4444)
    }

    @Test
    fun construction_longId_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VpRegistry(
                credentialEntries = emptyList(),
                id = "a".repeat(65),
                intentAction = "androidx.credentials.IntentAction",
            )
        }
    }

    @Test
    fun construction_longIntentAction_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VpRegistry(
                credentialEntries = emptyList(),
                id = "id",
                intentAction = "a".repeat(65),
            )
        }
    }

    @Test
    fun construction_emptyEntries() {
        val registry =
            OpenId4VpRegistry(
                credentialEntries = emptyList(),
                id = "id",
                intentAction = "androidx.credentials.IntentAction",
            )

        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(registry.id).isEqualTo("id")
        assertThat(registry.intentAction).isEqualTo("androidx.credentials.IntentAction")
        assertThat(json.getJSONObject("credentials").getJSONObject("dc+sd-jwt").length())
            .isEqualTo(0)
        assertThat(json.getJSONObject("credentials").getJSONObject("mso_mdoc").length())
            .isEqualTo(0)
        assertThat(json.getJSONObject("credentials").getJSONObject("issuance").length())
            .isEqualTo(0)
        assertThat(icons).isEmpty()
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
    }

    @Test
    fun construction_singleSdJwt() {
        val claim1 =
            SdJwtClaim(
                path = listOf("claim1"),
                value = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val claim2 =
            SdJwtClaim(
                path = listOf("nested", "claim2"),
                value = null,
                fieldDisplayPropertySet = setOf(VerificationFieldDisplayProperties("Claim Two")),
            )
        val sdJwtEntryVerificationDisplay =
            VerificationEntryDisplayProperties("SD-JWT Credential", "For testing", TEST_ICON_1)
        val sdJwtEntry =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:1",
                claims = listOf(claim1, claim2),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay),
                id = "sd-jwt-id",
            )

        val registry = OpenId4VpRegistry(credentialEntries = listOf(sdJwtEntry), id = "registry_id")

        assertThat(registry.id).isEqualTo("registry_id")
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
        assertThat(registry.intentAction).isEqualTo(RegistryManager.ACTION_GET_CREDENTIAL)
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("dc+sd-jwt")).isTrue()
        assertThat(credentials.has("mso_mdoc")).isTrue()
        assertThat(icons).hasSize(1)

        // Assert SD-JWT part
        val sdJwtCreds = credentials.getJSONObject("dc+sd-jwt")
        assertThat(sdJwtCreds.has(sdJwtEntry.verifiableCredentialType)).isTrue()
        val sdJwtArray = sdJwtCreds.getJSONArray(sdJwtEntry.verifiableCredentialType)
        assertThat(sdJwtArray.length()).isEqualTo(1)
        val sdJwtJson = sdJwtArray.getJSONObject(0)
        assertThat(sdJwtJson.getString("id")).isEqualTo(sdJwtEntry.id)

        val sdJwtDisplay = sdJwtJson.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay.title)
        assertThat(sdJwtDisplay.getString("subtitle"))
            .isEqualTo(sdJwtEntryVerificationDisplay.subtitle)
        assertThat(sdJwtDisplay.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry.id]!!.size)

        val sdJwtPaths = sdJwtJson.getJSONObject("paths")
        val claim1Json = sdJwtPaths.getJSONObject("claim1")
        assertThat(claim1Json.getString("value")).isEqualTo(claim1.value)
        val claim1Display = claim1Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim1Display.getString("display"))
            .isEqualTo(
                (claim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )
        assertThat(claim1Display.getString("display_value"))
            .isEqualTo(
                (claim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayValue
            )

        val claim2Json = sdJwtPaths.getJSONObject("nested").getJSONObject("claim2")
        assertThat(claim2Json.has("value")).isFalse() // value is null
        val claim2Display = claim2Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim2Display.getString("display"))
            .isEqualTo(
                (claim2.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )

        // Assert mdoc part
        val mdocCreds = credentials.getJSONObject("mso_mdoc")
        assertThat(mdocCreds.length()).isEqualTo(0)
        // Assert inline issuance
        assertThat(json.getJSONObject("credentials").getJSONObject("issuance").length())
            .isEqualTo(0)
    }

    @Test
    fun construction_multipleSdJwts() {
        val claim1 =
            SdJwtClaim(
                path = listOf("claim1"),
                value = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val claim2 =
            SdJwtClaim(
                path = listOf("nested", "claim2"),
                value = null,
                fieldDisplayPropertySet = setOf(VerificationFieldDisplayProperties("Claim Two")),
            )
        val sdJwtEntryVerificationDisplay1 =
            VerificationEntryDisplayProperties("SD-JWT Credential 1", null, TEST_ICON_1)
        val sdJwtEntry1 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:1",
                claims = listOf(claim1, claim2),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay1),
                id = "sd-jwt-id-1",
            )
        val claim3 =
            SdJwtClaim(
                path = listOf("claim3"),
                value = "value3",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim Three", null)),
            )
        val sdJwtEntryVerificationDisplay2 =
            VerificationEntryDisplayProperties("SD-JWT Credential 2", "subtitle 2", TEST_ICON_2)
        val sdJwtEntry2 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:1",
                claims = listOf(claim3),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay2),
                id = "sd-jwt-id-2",
            )
        val sdJwtEntryVerificationDisplay3 =
            VerificationEntryDisplayProperties("SD-JWT Credential 3", "subtitle 3", TEST_ICON_3)
        val sdJwtEntry3 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:2",
                claims = emptyList(),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay3),
                id = "sd-jwt-id-3",
            )

        val registry =
            OpenId4VpRegistry(
                credentialEntries = listOf(sdJwtEntry1, sdJwtEntry2, sdJwtEntry3),
                id = "registry_id",
                intentAction = "custom",
            )

        assertThat(registry.id).isEqualTo("registry_id")
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
        assertThat(registry.intentAction).isEqualTo("custom")
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("dc+sd-jwt")).isTrue()
        assertThat(credentials.has("mso_mdoc")).isTrue()
        assertThat(icons).hasSize(3)

        // Assert SD-JWT part
        val sdJwtCreds = credentials.getJSONObject("dc+sd-jwt")
        val pidV1Array = sdJwtCreds.getJSONArray("urn:eudi:pid:1")
        assertThat(pidV1Array.length()).isEqualTo(2)
        val pidV2Array = sdJwtCreds.getJSONArray("urn:eudi:pid:2")
        assertThat(pidV2Array.length()).isEqualTo(1)

        val sdJwtJson1 = pidV1Array.getJSONObject(0)
        assertThat(sdJwtJson1.getString("id")).isEqualTo(sdJwtEntry1.id)

        val sdJwtDisplay1 = sdJwtJson1.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay1.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay1.title)
        assertThat(sdJwtDisplay1.has("subtitle")).isFalse()
        assertThat(sdJwtDisplay1.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry1.id]!!.size)

        val claim1Json = sdJwtJson1.getJSONObject("paths").getJSONObject("claim1")
        assertThat(claim1Json.getString("value")).isEqualTo(claim1.value)
        val claim1Display = claim1Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim1Display.getString("display"))
            .isEqualTo(
                (claim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )
        assertThat(claim1Display.getString("display_value"))
            .isEqualTo(
                (claim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayValue
            )

        val claim2Json =
            sdJwtJson1.getJSONObject("paths").getJSONObject("nested").getJSONObject("claim2")
        assertThat(claim2Json.has("value")).isFalse() // value is null
        val claim2Display = claim2Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim2Display.getString("display"))
            .isEqualTo(
                (claim2.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )

        val sdJwtJson2 = pidV1Array.getJSONObject(1)
        assertThat(sdJwtJson2.getString("id")).isEqualTo(sdJwtEntry2.id)

        val sdJwtDisplay2 = sdJwtJson2.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay2.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay2.title)
        assertThat(sdJwtDisplay2.getString("subtitle"))
            .isEqualTo(sdJwtEntryVerificationDisplay2.subtitle)
        assertThat(sdJwtDisplay2.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry2.id]!!.size)

        val claim3Json = sdJwtJson2.getJSONObject("paths").getJSONObject("claim3")
        assertThat(claim3Json.getString("value")).isEqualTo(claim3.value)
        val claim3Display = claim3Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim3Display.getString("display"))
            .isEqualTo(
                (claim3.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )
        assertThat(claim3Display.has("display_value")).isFalse()

        val sdJwtJson3 = pidV2Array.getJSONObject(0)
        assertThat(sdJwtJson3.getString("id")).isEqualTo(sdJwtEntry3.id)

        val sdJwtDisplay3 = sdJwtJson3.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay3.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay3.title)
        assertThat(sdJwtDisplay3.getString("subtitle"))
            .isEqualTo(sdJwtEntryVerificationDisplay3.subtitle)
        assertThat(sdJwtDisplay3.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry3.id]!!.size)

        // Assert mdoc part
        val mdocCreds = credentials.getJSONObject("mso_mdoc")
        assertThat(mdocCreds.length()).isEqualTo(0)
        // Assert inline issuance
        assertThat(json.getJSONObject("credentials").getJSONObject("issuance").length())
            .isEqualTo(0)
    }

    @Test
    fun construction_singleMdoc() {
        val claim1 =
            MdocField(
                namespace = "namespace1",
                identifier = "identifier1",
                fieldValue = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val claim2 =
            MdocField(
                namespace = "namespace2",
                identifier = "identifier2",
                fieldValue = "value2",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim Two", "Value Two")),
            )
        val mdocEntryVerificationDisplay =
            VerificationEntryDisplayProperties("MDOC Credential", "For testing", TEST_ICON_1)
        val mdocEntry =
            MdocEntry(
                docType = "example.docType.1",
                fields = listOf(claim1, claim2),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay),
                id = "mdocid",
            )

        val registry = OpenId4VpRegistry(credentialEntries = listOf(mdocEntry), id = "registry_id")

        assertThat(registry.id).isEqualTo("registry_id")
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
        assertThat(registry.intentAction).isEqualTo(RegistryManager.ACTION_GET_CREDENTIAL)
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("dc+sd-jwt")).isTrue()
        assertThat(credentials.has("mso_mdoc")).isTrue()
        assertThat(icons).hasSize(1)

        // Assert SD-JWT part
        val sdJwtCreds = credentials.getJSONObject("dc+sd-jwt")
        assertThat(sdJwtCreds.length()).isEqualTo(0)

        // Assert mdoc part
        val mdocCreds = credentials.getJSONObject("mso_mdoc")
        assertThat(mdocCreds.has("example.docType.1")).isTrue()
        val mdocArray = mdocCreds.getJSONArray("example.docType.1")
        assertThat(mdocArray.length()).isEqualTo(1)
        val mdocJson = mdocArray.getJSONObject(0)
        assertThat(mdocJson.getString("id")).isEqualTo(mdocEntry.id)

        val mdocDisplay = mdocJson.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay.getString("title")).isEqualTo(mdocEntryVerificationDisplay.title)
        assertThat(mdocDisplay.getString("subtitle"))
            .isEqualTo(mdocEntryVerificationDisplay.subtitle)
        assertThat(mdocDisplay.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry.id]!!.size)

        // Assert inline issuance
        assertThat(json.getJSONObject("credentials").getJSONObject("issuance").length())
            .isEqualTo(0)
    }

    @Test
    fun construction_multipleMdocs() {
        val claim1 =
            MdocField(
                namespace = "namespace1",
                identifier = "identifier1",
                fieldValue = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val claim2 =
            MdocField(
                namespace = "namespace2",
                identifier = "identifier2",
                fieldValue = "value2",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim Two", "Value Two")),
            )
        val mdocEntryVerificationDisplay1 =
            VerificationEntryDisplayProperties("MDOC Credential", "For testing", TEST_ICON_1)
        val mdocEntry1 =
            MdocEntry(
                docType = "example.docType.1",
                fields = listOf(claim1, claim2),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay1),
                id = "mdocid1",
            )
        val claim3 =
            MdocField(
                namespace = "namespace3",
                identifier = "identifier3",
                fieldValue = "value3",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim 3", "Value 3")),
            )
        val mdocEntryVerificationDisplay2 =
            VerificationEntryDisplayProperties("MDOC Credential 2", null, TEST_ICON_2)
        val mdocEntry2 =
            MdocEntry(
                docType = "example.docType.1",
                fields = listOf(claim3),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay2),
                id = "mdocid2",
            )
        val mdocEntryVerificationDisplay3 =
            VerificationEntryDisplayProperties("MDOC Credential 3", "subtitle", TEST_ICON_3)
        val mdocEntry3 =
            MdocEntry(
                docType = "example.docType.2",
                fields = listOf(),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay3),
                id = "mdocid3",
            )

        val registry =
            OpenId4VpRegistry(
                credentialEntries = listOf(mdocEntry1, mdocEntry2, mdocEntry3),
                id = "registry_id",
            )

        assertThat(registry.id).isEqualTo("registry_id")
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
        assertThat(registry.intentAction).isEqualTo(RegistryManager.ACTION_GET_CREDENTIAL)
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("dc+sd-jwt")).isTrue()
        assertThat(credentials.has("mso_mdoc")).isTrue()
        assertThat(icons).hasSize(3)

        // Assert SD-JWT part
        val sdJwtCreds = credentials.getJSONObject("dc+sd-jwt")
        assertThat(sdJwtCreds.length()).isEqualTo(0)

        // Assert mdoc part
        val mdocCreds = credentials.getJSONObject("mso_mdoc")
        assertThat(mdocCreds.has("example.docType.1")).isTrue()
        val mdocArray = mdocCreds.getJSONArray("example.docType.1")
        assertThat(mdocArray.length()).isEqualTo(2)

        val mdocJson1 = mdocArray.getJSONObject(0)
        assertThat(mdocJson1.getString("id")).isEqualTo(mdocEntry1.id)
        val mdocDisplay1 = mdocJson1.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay1.getString("title")).isEqualTo(mdocEntryVerificationDisplay1.title)
        assertThat(mdocDisplay1.getString("subtitle"))
            .isEqualTo(mdocEntryVerificationDisplay1.subtitle)
        assertThat(mdocDisplay1.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry1.id]!!.size)

        val mdocJson2 = mdocArray.getJSONObject(1)
        assertThat(mdocJson2.getString("id")).isEqualTo(mdocEntry2.id)
        val mdocDisplay2 = mdocJson2.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay2.getString("title")).isEqualTo(mdocEntryVerificationDisplay2.title)
        assertThat(mdocDisplay2.has("subtitle")).isFalse()
        assertThat(mdocDisplay2.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry2.id]!!.size)

        assertThat(mdocCreds.has("example.docType.2")).isTrue()
        val mdocArray2 = mdocCreds.getJSONArray("example.docType.2")
        assertThat(mdocArray2.length()).isEqualTo(1)

        val mdocJson3 = mdocArray2.getJSONObject(0)
        assertThat(mdocJson3.getString("id")).isEqualTo(mdocEntry3.id)
        val mdocDisplay3 = mdocJson3.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay3.getString("title")).isEqualTo(mdocEntryVerificationDisplay3.title)
        assertThat(mdocDisplay3.getString("subtitle"))
            .isEqualTo(mdocEntryVerificationDisplay3.subtitle)
        assertThat(mdocDisplay3.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry3.id]!!.size)

        // Assert inline issuance
        assertThat(json.getJSONObject("credentials").getJSONObject("issuance").length())
            .isEqualTo(0)
    }

    @Test
    fun construction_mixedMdocAndSdJwtAndInlineIssuances() {
        val sdJwtClaim1 =
            SdJwtClaim(
                path = listOf("claim1"),
                value = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val sdJwtClaim2 =
            SdJwtClaim(
                path = listOf("nested", "claim2"),
                value = null,
                fieldDisplayPropertySet = setOf(VerificationFieldDisplayProperties("Claim Two")),
            )
        val sdJwtEntryVerificationDisplay1 =
            VerificationEntryDisplayProperties("SD-JWT Credential 1", null, TEST_ICON_1)
        val sdJwtEntry1 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:1",
                claims = listOf(sdJwtClaim1, sdJwtClaim2),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay1),
                id = "sd-jwt-id-1",
            )
        val sdJwtClaim3 =
            SdJwtClaim(
                path = listOf("claim3"),
                value = "value3",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim Three", null)),
            )
        val sdJwtEntryVerificationDisplay2 =
            VerificationEntryDisplayProperties("SD-JWT Credential 2", "subtitle 2", TEST_ICON_2)
        val sdJwtEntry2 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:1",
                claims = listOf(sdJwtClaim3),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay2),
                id = "sd-jwt-id-2",
            )
        val sdJwtEntryVerificationDisplay3 =
            VerificationEntryDisplayProperties("SD-JWT Credential 3", "subtitle 3", TEST_ICON_3)
        val sdJwtEntry3 =
            SdJwtEntry(
                verifiableCredentialType = "urn:eudi:pid:2",
                claims = emptyList(),
                entryDisplayPropertySet = setOf(sdJwtEntryVerificationDisplay3),
                id = "sd-jwt-id-3",
            )

        val mdocClaim1 =
            MdocField(
                namespace = "namespace1",
                identifier = "identifier1",
                fieldValue = "value1",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim One", "Value One")),
            )
        val mdocClaim2 =
            MdocField(
                namespace = "namespace2",
                identifier = "identifier2",
                fieldValue = "value2",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim Two", "Value Two")),
            )
        val mdocEntryVerificationDisplay1 =
            VerificationEntryDisplayProperties("MDOC Credential", "For testing", TEST_ICON_1)
        val mdocEntry1 =
            MdocEntry(
                docType = "example.docType.1",
                fields = listOf(mdocClaim1, mdocClaim2),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay1),
                id = "mdocid1",
            )
        val mdocClaim3 =
            MdocField(
                namespace = "namespace3",
                identifier = "identifier3",
                fieldValue = "value3",
                fieldDisplayPropertySet =
                    setOf(VerificationFieldDisplayProperties("Claim 3", "Value 3")),
            )
        val mdocEntryVerificationDisplay2 =
            VerificationEntryDisplayProperties("MDOC Credential 2", null, TEST_ICON_2)
        val mdocEntry2 =
            MdocEntry(
                docType = "example.docType.1",
                fields = listOf(mdocClaim3),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay2),
                id = "mdocid2",
            )
        val mdocEntryVerificationDisplay3 =
            VerificationEntryDisplayProperties("MDOC Credential 3", "subtitle", TEST_ICON_3)
        val mdocEntry3 =
            MdocEntry(
                docType = "example.docType.2",
                fields = listOf(),
                entryDisplayPropertySet = setOf(mdocEntryVerificationDisplay3),
                id = "mdocid3",
            )
        val issuanceDisplay1 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(subtitle = "New mdoc")
        val issuanceEntry1 =
            MdocInlineIssuanceEntry(
                id = "issuance_id_1",
                display = issuanceDisplay1,
                supportedMdocs =
                    setOf(MdocInlineIssuanceEntry.SupportedMdoc("org.iso.18013.5.1.mDL")),
            )

        val issuanceDisplay2 =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(
                titleHint = "Test Wallet",
                subtitle = "New sd-jwt",
                iconHint = TEST_ICON_2,
            )
        val issuanceEntry2 =
            SdJwtInlineIssuanceEntry(
                id = "issuance_id_2",
                display = issuanceDisplay2,
                supportedSdJwts = setOf(SdJwtInlineIssuanceEntry.SupportedSdJwt("urn:eudi:pid:1")),
            )

        val registry =
            OpenId4VpRegistry(
                credentialEntries =
                    listOf(
                        sdJwtEntry1,
                        sdJwtEntry2,
                        sdJwtEntry3,
                        mdocEntry1,
                        mdocEntry2,
                        mdocEntry3,
                    ),
                inlineIssuanceEntries = listOf(issuanceEntry1, issuanceEntry2),
                id = "registry_id",
            )

        assertThat(registry.id).isEqualTo("registry_id")
        assertThat(registry.matcher).isEqualTo(DEFAULT_MATCHER)
        assertThat(registry.intentAction).isEqualTo(RegistryManager.ACTION_GET_CREDENTIAL)
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("dc+sd-jwt")).isTrue()
        assertThat(credentials.has("mso_mdoc")).isTrue()
        assertThat(credentials.has("issuance")).isTrue()
        assertThat(icons).hasSize(7)

        // Assert SD-JWT part
        val sdJwtCreds = credentials.getJSONObject("dc+sd-jwt")
        val pidV1Array = sdJwtCreds.getJSONArray("urn:eudi:pid:1")
        assertThat(pidV1Array.length()).isEqualTo(2)
        val pidV2Array = sdJwtCreds.getJSONArray("urn:eudi:pid:2")
        assertThat(pidV2Array.length()).isEqualTo(1)

        val sdJwtJson1 = pidV1Array.getJSONObject(0)
        assertThat(sdJwtJson1.getString("id")).isEqualTo(sdJwtEntry1.id)

        val sdJwtDisplay1 = sdJwtJson1.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay1.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay1.title)
        assertThat(sdJwtDisplay1.has("subtitle")).isFalse()
        assertThat(sdJwtDisplay1.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry1.id]!!.size)

        val claim1Json = sdJwtJson1.getJSONObject("paths").getJSONObject("claim1")
        assertThat(claim1Json.getString("value")).isEqualTo(sdJwtClaim1.value)
        val claim1Display = claim1Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim1Display.getString("display"))
            .isEqualTo(
                (sdJwtClaim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )
        assertThat(claim1Display.getString("display_value"))
            .isEqualTo(
                (sdJwtClaim1.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayValue
            )

        val claim2Json =
            sdJwtJson1.getJSONObject("paths").getJSONObject("nested").getJSONObject("claim2")
        assertThat(claim2Json.has("value")).isFalse() // value is null
        val claim2Display = claim2Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim2Display.getString("display"))
            .isEqualTo(
                (sdJwtClaim2.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )

        val sdJwtJson2 = pidV1Array.getJSONObject(1)
        assertThat(sdJwtJson2.getString("id")).isEqualTo(sdJwtEntry2.id)

        val sdJwtDisplay2 = sdJwtJson2.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay2.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay2.title)
        assertThat(sdJwtDisplay2.getString("subtitle"))
            .isEqualTo(sdJwtEntryVerificationDisplay2.subtitle)
        assertThat(sdJwtDisplay2.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry2.id]!!.size)

        val claim3Json = sdJwtJson2.getJSONObject("paths").getJSONObject("claim3")
        assertThat(claim3Json.getString("value")).isEqualTo(sdJwtClaim3.value)
        val claim3Display = claim3Json.getJSONObject("display").getJSONObject("verification")
        assertThat(claim3Display.getString("display"))
            .isEqualTo(
                (sdJwtClaim3.fieldDisplayPropertySet.first() as VerificationFieldDisplayProperties)
                    .displayName
            )
        assertThat(claim3Display.has("display_value")).isFalse()

        val sdJwtJson3 = pidV2Array.getJSONObject(0)
        assertThat(sdJwtJson3.getString("id")).isEqualTo(sdJwtEntry3.id)

        val sdJwtDisplay3 = sdJwtJson3.getJSONObject("display").getJSONObject("verification")
        assertThat(sdJwtDisplay3.getString("title")).isEqualTo(sdJwtEntryVerificationDisplay3.title)
        assertThat(sdJwtDisplay3.getString("subtitle"))
            .isEqualTo(sdJwtEntryVerificationDisplay3.subtitle)
        assertThat(sdJwtDisplay3.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[sdJwtEntry3.id]!!.size)

        // Assert mdoc part
        val mdocCreds = credentials.getJSONObject("mso_mdoc")
        assertThat(mdocCreds.has("example.docType.1")).isTrue()
        val mdocArray = mdocCreds.getJSONArray("example.docType.1")
        assertThat(mdocArray.length()).isEqualTo(2)

        val mdocJson1 = mdocArray.getJSONObject(0)
        assertThat(mdocJson1.getString("id")).isEqualTo(mdocEntry1.id)
        val mdocDisplay1 = mdocJson1.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay1.getString("title")).isEqualTo(mdocEntryVerificationDisplay1.title)
        assertThat(mdocDisplay1.getString("subtitle"))
            .isEqualTo(mdocEntryVerificationDisplay1.subtitle)
        assertThat(mdocDisplay1.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry1.id]!!.size)

        val mdocJson2 = mdocArray.getJSONObject(1)
        assertThat(mdocJson2.getString("id")).isEqualTo(mdocEntry2.id)
        val mdocDisplay2 = mdocJson2.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay2.getString("title")).isEqualTo(mdocEntryVerificationDisplay2.title)
        assertThat(mdocDisplay2.has("subtitle")).isFalse()
        assertThat(mdocDisplay2.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry2.id]!!.size)

        assertThat(mdocCreds.has("example.docType.2")).isTrue()
        val mdocArray2 = mdocCreds.getJSONArray("example.docType.2")
        assertThat(mdocArray2.length()).isEqualTo(1)

        val mdocJson3 = mdocArray2.getJSONObject(0)
        assertThat(mdocJson3.getString("id")).isEqualTo(mdocEntry3.id)
        val mdocDisplay3 = mdocJson3.getJSONObject("display").getJSONObject("verification")
        assertThat(mdocDisplay3.getString("title")).isEqualTo(mdocEntryVerificationDisplay3.title)
        assertThat(mdocDisplay3.getString("subtitle"))
            .isEqualTo(mdocEntryVerificationDisplay3.subtitle)
        assertThat(mdocDisplay3.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[mdocEntry3.id]!!.size)

        // Assert issuance part
        val issuanceCreds = credentials.getJSONObject("issuance")
        assertThat(issuanceCreds.has("mso_mdoc")).isTrue()
        val mdocIssuanceArray = issuanceCreds.getJSONArray("mso_mdoc")
        assertThat(mdocIssuanceArray.length()).isEqualTo(1)
        val mdocIssuanceJson = mdocIssuanceArray.getJSONObject(0)
        assertThat(mdocIssuanceJson.getString("id")).isEqualTo(issuanceEntry1.id)
        assertThat(mdocIssuanceJson.getString("subtitle")).isEqualTo(issuanceDisplay1.subtitle)
        assertThat(mdocIssuanceJson.has("title")).isFalse()
        assertThat(mdocIssuanceJson.has("icon")).isFalse()

        assertThat(issuanceCreds.has("dc+sd-jwt")).isTrue()
        val sdJwtIssuanceArray = issuanceCreds.getJSONArray("dc+sd-jwt")
        assertThat(sdJwtIssuanceArray.length()).isEqualTo(1)
        val sdJwtIssuanceJson = sdJwtIssuanceArray.getJSONObject(0)
        assertThat(sdJwtIssuanceJson.getString("id")).isEqualTo(issuanceEntry2.id)
        assertThat(sdJwtIssuanceJson.getString("subtitle")).isEqualTo(issuanceDisplay2.subtitle)
        assertThat(sdJwtIssuanceJson.getString("title")).isEqualTo(issuanceDisplay2.titleHint)
        assertThat(sdJwtIssuanceJson.getJSONObject("icon").getInt("length"))
            .isEqualTo(icons[issuanceEntry2.id]!!.size)
    }

    @Test
    fun construction_singleIssuance() {
        val issuanceDisplay =
            InlineIssuanceEntry.InlineIssuanceDisplayProperties(
                subtitle = "New credential",
                titleHint = "Issuance",
            )
        val issuanceEntry =
            MdocInlineIssuanceEntry(
                id = "issuance_id",
                display = issuanceDisplay,
                supportedMdocs =
                    setOf(MdocInlineIssuanceEntry.SupportedMdoc("org.iso.18013.5.1.mDL")),
            )

        val registry =
            OpenId4VpRegistry(
                credentialEntries = emptyList(),
                inlineIssuanceEntries = listOf(issuanceEntry),
                id = "registry_id",
            )

        assertThat(registry.id).isEqualTo("registry_id")
        val (json, icons) = parseCredentialBytes(registry.credentials)
        assertThat(json.has("credentials")).isTrue()
        val credentials = json.getJSONObject("credentials")
        assertThat(credentials.has("issuance")).isTrue()
        assertThat(icons).isEmpty()

        val issuanceCreds = credentials.getJSONObject("issuance")
        assertThat(issuanceCreds.has("mso_mdoc")).isTrue()
        val issuanceArray = issuanceCreds.getJSONArray("mso_mdoc")
        assertThat(issuanceArray.length()).isEqualTo(1)
        val issuanceJson = issuanceArray.getJSONObject(0)
        assertThat(issuanceJson.getString("id")).isEqualTo(issuanceEntry.id)
        assertThat(issuanceJson.getString("subtitle")).isEqualTo(issuanceDisplay.subtitle)
        assertThat(issuanceJson.getString("title")).isEqualTo(issuanceDisplay.titleHint)
        assertThat(issuanceJson.has("icon")).isFalse()
    }

    /** Helper to parse the custom byte format into a JSON object and a map of icon bytes. */
    private fun parseCredentialBytes(bytes: ByteArray): Pair<JSONObject, Map<String, ByteArray>> {
        val inputStream = ByteArrayInputStream(bytes)

        // First 4 bytes are the offset to the JSON part
        val offsetBytes = ByteArray(4)
        inputStream.read(offsetBytes)
        val jsonOffset = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN).int

        // Read the JSON part
        val jsonBytes = bytes.sliceArray(jsonOffset until bytes.size)
        val json = JSONObject(String(jsonBytes, Charsets.UTF_8))

        // Read the icons
        val icons = mutableMapOf<String, ByteArray>()
        val creds = json.getJSONObject("credentials")
        val sdJwtCreds = creds.optJSONObject("dc+sd-jwt")
        val mdocCreds = creds.optJSONObject("mso_mdoc")
        val inlineIssuanceCreds = creds.optJSONObject("issuance")

        val allCreds = mutableListOf<JSONObject>()
        sdJwtCreds?.keys()?.forEach { key ->
            sdJwtCreds.getJSONArray(key).let {
                for (i in 0 until it.length()) allCreds.add(it.getJSONObject(i))
            }
        }
        mdocCreds?.keys()?.forEach { key ->
            mdocCreds.getJSONArray(key).let {
                for (i in 0 until it.length()) allCreds.add(it.getJSONObject(i))
            }
        }
        inlineIssuanceCreds?.keys()?.forEach { key ->
            inlineIssuanceCreds.getJSONArray(key).let { array ->
                for (i in 0 until array.length()) allCreds.add(array.getJSONObject(i))
            }
        }

        for (cred in allCreds) {
            val id = cred.getString("id")
            val iconInfo =
                cred.optJSONObject("display")?.optJSONObject("verification")?.optJSONObject("icon")
                    ?: cred.optJSONObject("icon")
            if (iconInfo != null) {
                val start = iconInfo.getInt("start")
                val length = iconInfo.getInt("length")
                val iconBytes = bytes.sliceArray(start until start + length)
                icons[id] = iconBytes
            }
        }

        return Pair(json, icons)
    }
}
