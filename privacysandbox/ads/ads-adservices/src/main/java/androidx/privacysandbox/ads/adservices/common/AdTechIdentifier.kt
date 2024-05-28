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
 * An Identifier representing an ad buyer or seller.
 *
 * @param identifier The identifier.
 */
@SuppressLint("ClassVerificationFailure")
class AdTechIdentifier public constructor(val identifier: String) {

    /**
     * Compares this AdTechIdentifier to the specified object. The result is true if and only if the
     * argument is not null and the identifier property of the two objects are equal. Note that this
     * method will not perform any eTLD+1 normalization so two AdTechIdentifier objects with the
     * same eTLD+1 could be not equal if the String representations of the objects was not equal.
     *
     * @param other The object to compare this AdTechIdentifier against
     * @return true if the given object represents an AdTechIdentifier equivalent to this
     *   AdTechIdentifier, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdTechIdentifier) return false
        return this.identifier == other.identifier
    }

    /**
     * Returns a hash code corresponding to the string representation of this class obtained by
     * calling [.toString]. Note that this method will not perform any eTLD+1 normalization so two
     * AdTechIdentifier objects with the same eTLD+1 could have different hash codes if the
     * underlying string representation was different.
     *
     * @return a hash code value for this object.
     */
    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    /** @return The identifier in String form. */
    override fun toString(): String {
        return "$identifier"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertToAdServices(): android.adservices.common.AdTechIdentifier {
        return android.adservices.common.AdTechIdentifier.fromString(identifier)
    }
}
