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

import android.graphics.Bitmap
import androidx.credentials.registry.provider.digitalcredentials.DigitalCredentialRegistry.Companion.DISPLAY_TYPE_VERIFICATION

/**
 * The display metadata associated with a [DigitalCredentialEntry] to be rendered in a selector UI
 * style serving the verification purpose.
 *
 * The [explainer], if set, will be displayed below the entry and rendered as a note (e.g. in a grey
 * background). For example, you can use it to display a note of the terms of your service.
 *
 * The [warning], if set, will be displayed below the entry, in parallel to the [explainer] and
 * highlighted as a warning (e.g. bolded, in a red background). For example, you can use it to
 * display a warning if the calling app is requesting very sensitive information.
 *
 * @constructor
 * @property title the title to display for this entry in the Credential Manager selector UI
 * @property subtitle the subtitle to display for this entry
 * @property icon the icon to display for this entry; this icon should be 32x32 and if not will be
 *   rescaled into a 32x32 pixel PGN for display
 * @property explainer the additional note or explainer to display for this entry, if applicable
 * @property warning the warning to display for this entry, if applicable
 */
public class VerificationEntryDisplayProperties(
    public val title: CharSequence,
    public val subtitle: CharSequence?,
    public val icon: Bitmap,
    public val explainer: CharSequence? = null,
    public val warning: CharSequence? = null,
) : EntryDisplayProperties(DISPLAY_TYPE_VERIFICATION) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerificationEntryDisplayProperties) return false
        return this.title == other.title &&
            this.subtitle == other.subtitle &&
            this.icon == other.icon &&
            this.explainer == other.explainer &&
            this.warning == other.warning
    }

    override fun hashCode(): Int {
        var result = displayType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + icon.hashCode()
        result = 31 * result + (explainer?.hashCode() ?: 0)
        result = 31 * result + (warning?.hashCode() ?: 0)
        return result
    }
}
