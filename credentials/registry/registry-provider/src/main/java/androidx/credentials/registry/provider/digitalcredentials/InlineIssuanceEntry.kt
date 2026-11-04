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

package androidx.credentials.registry.provider.digitalcredentials

import android.graphics.Bitmap

/**
 * An entry offering the user an opportunity to add a digital credential to your holder / wallet
 * application during the presentation moment.
 *
 * This serves as a fallback or alternative to a real digital credential that can be presented. When
 * the verifier application requests for a digital credential presentation, it is possible that the
 * user has not officially add their corresponding digital credential(s) to their holder application
 * yet. The [InlineIssuanceEntry] allows the holder application to add the credentials on the fly
 * and then immediately return them, allowing the request, as well as the future requests, to still
 * succeed.
 *
 * @property id the provider unique identifier of this credential entry, which can be used to
 *   identify the exact credential that the user has chosen; it is recommended that you generate
 *   this `id` with enough entropy that it cannot be guessed by a third party, e.g. through
 *   encrypting this `id` or randomizing it.
 * @property display the display properties associated with the given entry
 * @throws IllegalArgumentException if [id] length is greater than 64 characters
 */
public abstract class InlineIssuanceEntry(
    public val id: String,
    public val display: InlineIssuanceDisplayProperties,
) {
    init {
        require(id.length <= 64) { "`id` length must be less than 64" }
    }

    /**
     * The display metadata associated with a [InlineIssuanceEntry] to be rendered in a selector UI.
     *
     * @property subtitle the subtitle to display for this entry
     * @property titleHint the title that _may_ be displayed for this entry in the selector UI only
     *   if the registering application is system privileged; otherwise, the application label will
     *   be used. Therefore, most applications should leave this unset.
     * @property iconHint the icon that _may_ be displayed for this entry in the selector UI only if
     *   the registering application is system privileged; otherwise, the application icon will be
     *   used. Therefore, most applications should leave this unset.
     */
    public class InlineIssuanceDisplayProperties(
        public val subtitle: CharSequence?,
        public val titleHint: CharSequence? = null,
        public val iconHint: Bitmap? = null,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InlineIssuanceDisplayProperties) return false
            return this.subtitle == other.subtitle &&
                this.titleHint == other.titleHint &&
                this.iconHint == other.iconHint
        }

        override fun hashCode(): Int {
            var result = subtitle?.hashCode() ?: 0
            result = 31 * result + (titleHint?.hashCode() ?: 0)
            result = 31 * result + (iconHint?.hashCode() ?: 0)
            return result
        }
    }
}
