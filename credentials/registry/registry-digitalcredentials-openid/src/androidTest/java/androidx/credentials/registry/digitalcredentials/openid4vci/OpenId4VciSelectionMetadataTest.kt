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

package androidx.credentials.registry.digitalcredentials.openid4vci

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
public class OpenId4VciSelectionMetadataTest {

    @Test
    fun parse_validJson_success() {
        val json = """{"eid":"entry_id_1","ridx":2}"""
        val metadata = OpenId4VciSelectionMetadata.parse(json)
        assertThat(metadata.entryId).isEqualTo("entry_id_1")
        assertThat(metadata.requestIndex).isEqualTo(2)

        val zeroIndex = OpenId4VciSelectionMetadata.parse("""{"eid":"entry_0","ridx":0}""")
        assertThat(zeroIndex.entryId).isEqualTo("entry_0")
        assertThat(zeroIndex.requestIndex).isEqualTo(0)
    }

    @Test
    fun parse_malformedJson_throwsJSONException() {
        val json = """{"eid":"entry_id_1","ridx"}"""
        assertThrows(JSONException::class.java) { OpenId4VciSelectionMetadata.parse(json) }
    }

    @Test
    fun parse_missingEntryId_throwsJSONException() {
        val json = """{"ridx":2}"""
        assertThrows(JSONException::class.java) { OpenId4VciSelectionMetadata.parse(json) }
    }

    @Test
    fun parse_nullOrNonStringEntryId_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":null,"ridx":0}""")
        }
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":123,"ridx":0}""")
        }
    }

    @Test
    fun parse_blankEntryId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"","ridx":0}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"   ","ridx":0}""")
        }
    }

    @Test
    fun parse_missingRequestIndex_throwsJSONException() {
        val json = """{"eid":"entry_id_1"}"""
        assertThrows(JSONException::class.java) { OpenId4VciSelectionMetadata.parse(json) }
    }

    @Test
    fun parse_nullOrNonIntegerRequestIndex_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":null}""")
        }
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":"2"}""")
        }
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":1.5}""")
        }
        assertThrows(JSONException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":2147483648}""")
        }
    }

    @Test
    fun parse_negativeRequestIndex_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":-1}""")
        }
    }

    @Test
    fun equalsHashCodeAndToString() {
        val meta1 = OpenId4VciSelectionMetadata.parse("""{"eid":"entry_1","ridx":2}""")
        val meta2 = OpenId4VciSelectionMetadata.parse("""{"eid":"entry_1","ridx":2}""")
        val diffId = OpenId4VciSelectionMetadata.parse("""{"eid":"entry_2","ridx":2}""")
        val diffIndex = OpenId4VciSelectionMetadata.parse("""{"eid":"entry_1","ridx":3}""")

        assertThat(meta1).isEqualTo(meta2)
        assertThat(meta1.hashCode()).isEqualTo(meta2.hashCode())
        assertThat(meta1).isNotEqualTo(diffId)
        assertThat(meta1).isNotEqualTo(diffIndex)
        assertThat(meta1.toString())
            .isEqualTo("OpenId4VciSelectionMetadata(entryId=entry_1, requestIndex=2)")
    }

    @Test
    fun selectedOpenId4VciMetadata_validMetadata_returnsParsedMetadata() {
        val request = createProviderRequestWithMetadata("""{"eid":"entry_id_1","ridx":2}""")
        val metadata = request.selectedOpenId4VciMetadata
        assertThat(metadata)
            .isEqualTo(OpenId4VciSelectionMetadata.parse("""{"eid":"entry_id_1","ridx":2}"""))
    }

    @Test
    fun selectedOpenId4VciMetadata_absentMetadata_returnsNull() {
        val request = createProviderRequestWithMetadata(null)
        assertThat(request.selectedOpenId4VciMetadata).isNull()
    }

    @Test
    fun selectedOpenId4VciMetadata_invalidOrMalformedMetadata_returnsNull() {
        val malformedRequest = createProviderRequestWithMetadata("not_json")
        assertThat(malformedRequest.selectedOpenId4VciMetadata).isNull()

        val negativeIndexRequest =
            createProviderRequestWithMetadata("""{"eid":"entry_id_1","ridx":-1}""")
        assertThat(negativeIndexRequest.selectedOpenId4VciMetadata).isNull()
    }

    private fun createProviderRequestWithMetadata(
        metadataJson: String?
    ): ProviderCreateCredentialRequest {
        val baseRequest =
            ProviderCreateCredentialRequest(
                CreatePasswordRequest("id", "password"),
                getTestCallingAppInfo(),
            )
        val bundle = ProviderCreateCredentialRequest.asBundle(baseRequest)
        if (metadataJson != null) {
            bundle.putString(EXTRA_CREDENTIAL_METADATA_KEY, metadataJson)
        }
        return ProviderCreateCredentialRequest.fromBundle(bundle)
    }

    @Suppress("DEPRECATION")
    private fun getTestCallingAppInfo(): CallingAppInfo {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageName = context.packageName
        if (Build.VERSION.SDK_INT >= 28) {
            val packageInfo =
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            assertNotNull(packageInfo.signingInfo)
            return CallingAppInfo(packageName, packageInfo.signingInfo!!, null)
        } else {
            val packageInfo =
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            return CallingAppInfo(packageName, packageInfo.signatures!!.filterNotNull(), null)
        }
    }

    private companion object {
        private const val EXTRA_CREDENTIAL_METADATA_KEY =
            "androidx.credentials.registry.provider.extra.CREDENTIAL_METADATA"
    }
}
