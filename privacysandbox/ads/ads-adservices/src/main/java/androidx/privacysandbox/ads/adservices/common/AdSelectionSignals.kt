/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.common

import android.annotation.SuppressLint
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/**
 * This class holds JSON that will be passed into a JavaScript function during ad selection. Its
 * contents are not used by <a
 * href="https://developer.android.com/design-for-safety/privacy-sandbox/fledge">FLEDGE</a> platform
 * code, but are merely validated and then passed to the appropriate JavaScript ad selection
 * function.
 *
 * @param signals Any valid JSON string to create the AdSelectionSignals with.
 */
@SuppressLint("ClassVerificationFailure")
class AdSelectionSignals public constructor(val signals: String) {
    /**
     * Compares this AdSelectionSignals to the specified object. The result is true if and only if
     * the argument is not null and the signals property of the two objects are equal. Note that
     * this method will not perform any JSON normalization so two AdSelectionSignals objects with
     * the same JSON could be not equal if the String representations of the objects was not equal.
     *
     * @param other The object to compare this AdSelectionSignals against
     * @return true if the given object represents an AdSelectionSignals equivalent to this
     *   AdSelectionSignals, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdSelectionSignals) return false
        return this.signals == other.signals
    }

    /**
     * Returns a hash code corresponding to the string representation of this class obtained by
     * calling [.toString]. Note that this method will not perform any JSON normalization so two
     * AdSelectionSignals objects with the same JSON could have different hash codes if the
     * underlying string representation was different.
     *
     * @return a hash code value for this object.
     */
    override fun hashCode(): Int {
        return signals.hashCode()
    }

    /** @return The String form of the JSON wrapped by this class. */
    override fun toString(): String {
        return "AdSelectionSignals: $signals"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.common.AdSelectionSignals {
        return android.adservices.common.AdSelectionSignals.fromString(signals)
    }
}
