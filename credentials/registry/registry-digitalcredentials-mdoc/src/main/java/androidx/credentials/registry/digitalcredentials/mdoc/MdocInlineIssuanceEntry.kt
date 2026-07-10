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

package androidx.credentials.registry.digitalcredentials.mdoc

import androidx.credentials.registry.provider.digitalcredentials.InlineIssuanceEntry

/**
 * An entry offering the user an opportunity to add a digital credential to your holder / wallet
 * application during the presentation moment.
 *
 * This serves as a fallback or alternative to a real digital credential that can be presented. When
 * the verifier application requests for a digital credential presentation, it is possible that the
 * user has not officially add their corresponding digital credential(s) to their holder application
 * yet. The [MdocInlineIssuanceEntry] allows the holder application to add the credentials on the
 * fly and then immediately return them, allowing the request, as well as the future requests, to
 * still succeed.
 *
 * @property id the provider unique identifier of this credential entry, which can be used to
 *   identify the exact credential that the user has chosen; it is recommended that you generate
 *   this `id` with enough entropy that it cannot be guessed by a third party, e.g. through
 *   encrypting this `id` or randomizing it.
 * @property display the display properties associated with the given entry
 * @property supportedMdocs supported mdocs to offer the inline issuance flow; cannot be empty
 * @constructor
 * @throws IllegalArgumentException if [id] length is greater than 64 characters
 * @throws IllegalArgumentException if [supportedMdocs] is empty
 */
public class MdocInlineIssuanceEntry(
    id: String,
    display: InlineIssuanceDisplayProperties,
    public val supportedMdocs: Set<SupportedMdoc>,
) : InlineIssuanceEntry(id = id, display = display) {
    init {
        require(supportedMdocs.isNotEmpty()) { "`supportedMdocs` must not be empty" }
    }

    /**
     * Configuration determining whether the [MdocInlineIssuanceEntry] should be offered for a
     * presentation request
     *
     * @property docType the supported mdoc document type as defined in
     *   [the ISO/IEC 18013-5:2021 specification](https://www.iso.org/standard/69084.html)
     * @constructor
     * @throws IllegalArgumentException if `docType` is empty
     */
    public class SupportedMdoc(public val docType: String) {
        init {
            require(docType.isNotEmpty()) { "`docType` cannot" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SupportedMdoc) return false
            return this.docType == other.docType
        }

        override fun hashCode(): Int {
            return docType.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MdocInlineIssuanceEntry) return false
        return this.id == other.id &&
            this.display == other.display &&
            this.supportedMdocs == other.supportedMdocs
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + display.hashCode()
        result = 31 * result + supportedMdocs.hashCode()
        return result
    }
}
