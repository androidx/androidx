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

package androidx.credentials.registry.provider.digitalcredentials

import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry.Companion.DISPLAY_TYPE_VERIFICATION

/**
 * The display metadata associated with a [DigitalCredentialField] to be rendered in a selector UI
 * style serving the verification purpose.
 *
 * @property displayName the localized display value for the name of this field; for example, the
 *   display name of an ISO mDL age-over-twenty-one property may be "Age over 21" localized
 * @property displayValue the localized display value for the value of this field; for example, the
 *   display value of an ISO mDL age-over-twenty-one property may be "Yes" localized; a null value
 *   means only the display name will be rendered to the user
 * @constructor
 *     @throws IllegalArgumentException if `displayName` is empty or blank
 */
public class VerificationFieldDisplayProperties(
    public val displayName: CharSequence,
    public val displayValue: CharSequence? = null,
) : FieldDisplayProperties(DISPLAY_TYPE_VERIFICATION) {
    init {
        require(displayName.isNotBlank()) { "`displayName` must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerificationFieldDisplayProperties) return false
        return this.displayName == other.displayName && this.displayValue == other.displayValue
    }

    override fun hashCode(): Int {
        var result = displayType.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (displayValue?.hashCode() ?: 0)
        return result
    }
}
