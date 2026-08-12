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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.json.JSONException
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
    fun parse_missingRequestIndex_throwsJSONException() {
        val json = """{"eid":"entry_id_1"}"""
        assertThrows(JSONException::class.java) { OpenId4VciSelectionMetadata.parse(json) }
    }
}
