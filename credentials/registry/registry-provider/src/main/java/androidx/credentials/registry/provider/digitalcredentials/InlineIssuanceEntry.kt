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
import androidx.annotation.RequiresPermission

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
     * Holds display metadata for an [InlineIssuanceEntry] rendered in the selector UI.
     *
     * @property subtitle the subtitle to display for this entry
     * @property titleHint optional title to display in the selector UI. Overriding the default
     *   application label requires the registering application to hold
     *   `android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON`; otherwise, the system
     *   ignores the override and uses the default application label. Most applications should leave
     *   this unset.
     * @property iconHint optional icon to display in the selector UI. Overriding the default
     *   application icon requires the registering application to hold
     *   `android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON`; otherwise, the system
     *   ignores the override and uses the default application icon. Most applications should leave
     *   this unset.
     */
    public class InlineIssuanceDisplayProperties
    @RequiresPermission(
        value = "android.permission.SUBSTITUTE_SHARE_TARGET_APP_NAME_AND_ICON",
        conditional = true,
    )
    constructor(
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
