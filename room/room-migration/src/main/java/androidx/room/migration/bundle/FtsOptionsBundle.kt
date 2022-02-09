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

import com.google.gson.annotations.SerializedName

/**
 * Data class that holds FTS Options of an {@link Fts3 Fts3} or
 * {@link Fts4 Fts4}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FtsOptionsBundle(
    @SerializedName("tokenizer")
    private val tokenizer: String,
    @SerializedName("tokenizerArgs")
    public open val tokenizerArgs: List<String>,
    @SerializedName("contentTable")
    public open val contentTable: String,
    @SerializedName("languageIdColumnName")
    public open val languageIdColumnName: String,
    @SerializedName("matchInfo")
    public open val matchInfo: String,
    @SerializedName("notIndexedColumns")
    public open val notIndexedColumns: List<String>,
    @SerializedName("prefixSizes")
    public open val prefixSizes: List<Int>,
    @SerializedName("preferredOrder")
    public open val preferredOrder: String
) : SchemaEquality<FtsOptionsBundle> {

    // Used by GSON
    @Deprecated("Marked deprecated to avoid usage in the codebase")
    @SuppressWarnings("unused")
    private constructor() : this("", emptyList(), "", "", "", emptyList(), emptyList(), "")

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
