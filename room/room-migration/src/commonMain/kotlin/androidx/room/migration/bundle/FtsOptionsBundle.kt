/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.migration.bundle

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Data class that holds [androidx.room.FtsOptions] information. */
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FtsOptionsBundle(
    @SerialName("tokenizer") private val tokenizer: String,
    @SerialName("tokenizerArgs") val tokenizerArgs: List<String>,
    @SerialName("contentTable") val contentTable: String,
    @SerialName("languageIdColumnName") val languageIdColumnName: String,
    @SerialName("matchInfo") val matchInfo: String,
    @SerialName("notIndexedColumns") val notIndexedColumns: List<String>,
    @SerialName("prefixSizes") val prefixSizes: List<Int>,
    @SerialName("preferredOrder") val preferredOrder: String
) : SchemaEquality<FtsOptionsBundle> {

    override fun isSchemaEqual(other: FtsOptionsBundle): Boolean {
        return tokenizer == other.tokenizer &&
            tokenizerArgs == other.tokenizerArgs &&
            contentTable == other.contentTable &&
            languageIdColumnName == other.languageIdColumnName &&
            matchInfo == other.matchInfo &&
            notIndexedColumns == other.notIndexedColumns &&
            prefixSizes == other.prefixSizes &&
            preferredOrder == other.preferredOrder
    }
}
