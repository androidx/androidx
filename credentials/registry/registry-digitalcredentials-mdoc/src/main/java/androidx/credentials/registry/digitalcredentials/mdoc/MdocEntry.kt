/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.mdoc

import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialEntry
import androidx.credentials.registry.provider.digitalcredentials.EntryDisplayProperties

/**
 * Mdoc entry, a mobile document entry whose format follows
 * [the ISO/IEC 18013-5:2021 specification](https://www.iso.org/standard/69084.html).
 *
 * @property docType the DocType, e.g. "org.iso.18013.5.1.mDL" for a mobile driving license
 * @property entryDisplayPropertySet a set of entry display metadata, each serving a different UI
 *   style variant
 * @property id the unique identifier of this credential entry, which can be used to identify the
 *   exact credential that the user has chosen
 * @property fields the given mdoc's individual properties used both for filtering and display
 *   purposes
 * @constructor
 * @throws IllegalArgumentException if [id] length is greater than 64 characters
 */
public class MdocEntry(
    public val docType: String,
    public val fields: List<MdocField>,
    entryDisplayPropertySet: Set<EntryDisplayProperties>,
    id: String,
) : DigitalCredentialEntry(id = id, entryDisplayPropertySet = entryDisplayPropertySet) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MdocEntry) return false
        return this.id == other.id &&
            this.entryDisplayPropertySet == other.entryDisplayPropertySet &&
            this.docType == other.docType &&
            this.fields == other.fields &&
            this.entryDisplayPropertySet == other.entryDisplayPropertySet
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + entryDisplayPropertySet.hashCode()
        result = 31 * result + docType.hashCode()
        result = 31 * result + fields.hashCode()
        return result
    }
}
