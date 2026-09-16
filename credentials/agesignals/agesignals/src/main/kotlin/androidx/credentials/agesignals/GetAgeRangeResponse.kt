/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals

import android.os.Bundle
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting

/**
 * Contains the age range signal for the user.
 *
 * @property lowerAgeBound inclusive lower bound of the user's age range as a non-negative number
 * @property upperAgeBound inclusive upper bound of the user's age range as a non-negative number,
 *   or null when the range is open-ended (e.g., 18+)
 * @property assuranceTier tier of assurance for this age signal, defined in [AgeAssuranceTier]
 */
public class GetAgeRangeResponse(
    public val lowerAgeBound: Int,
    @get:Suppress("AutoBoxing") @param:Suppress("AutoBoxing") public val upperAgeBound: Int?,
    @AgeAssuranceTier public val assuranceTier: Int,
) {
    init {
        require(lowerAgeBound >= 0) {
            "lowerAgeBound must be non-negative, but was $lowerAgeBound."
        }
        if (upperAgeBound != null) {
            require(upperAgeBound >= lowerAgeBound) {
                "upperAgeBound ($upperAgeBound) must be greater than or equal to lowerAgeBound ($lowerAgeBound)."
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetAgeRangeResponse) return false
        return this.lowerAgeBound == other.lowerAgeBound &&
            this.upperAgeBound == other.upperAgeBound &&
            this.assuranceTier == other.assuranceTier
    }

    override fun hashCode(): Int {
        var result = lowerAgeBound.hashCode()
        result = 31 * result + (upperAgeBound?.hashCode() ?: 0)
        result = 31 * result + assuranceTier.hashCode()
        return result
    }

    override fun toString(): String {
        return "GetAgeRangeResponse(" +
            "lowerAgeBound=$lowerAgeBound, " +
            "upperAgeBound=$upperAgeBound, " +
            "assuranceTier=$assuranceTier)"
    }

    /**
     * The assurance tier of the age signal returned by the provider.
     *
     * Higher tiers indicate stronger assurance that the user belongs to the specified age range
     * with [ASSURANCE_TIER_A] as the lowest and [ASSURANCE_TIER_D] as the highest tiers
     * respectively.
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.FIELD,
    )
    @IntDef(
        value =
            [
                ASSURANCE_TIER_A,
                ASSURANCE_TIER_B,
                ASSURANCE_TIER_C,
                ASSURANCE_TIER_D,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class AgeAssuranceTier

    public companion object {
        /** User has self declared their age. */
        public const val ASSURANCE_TIER_A: Int = 1

        /** User's age is managed by a parent or a guardian. */
        public const val ASSURANCE_TIER_B: Int = 2

        /**
         * User's age is assessed by using credit card, email address, selfie assessment, Government
         * ID, or Tax ID.
         */
        public const val ASSURANCE_TIER_C: Int = 3

        /**
         * User's age is checked by using a combination of Government ID and selfie assessment, or
         * Digital ID.
         */
        public const val ASSURANCE_TIER_D: Int = 4

        private const val TAG = "GetAgeRangeResponse"

        @VisibleForTesting
        internal const val EXTRA_AGE_LOWER_BOUND =
            "androidx.credentials.agesignals.extra.AGE_LOWER_BOUND"

        @VisibleForTesting
        internal const val EXTRA_AGE_UPPER_BOUND =
            "androidx.credentials.agesignals.extra.AGE_UPPER_BOUND"

        @VisibleForTesting
        internal const val EXTRA_AGE_ASSURANCE_TIER =
            "androidx.credentials.agesignals.extra.AGE_ASSURANCE_TIER"

        /**
         * Helper method to convert the given [response] to a parcelable [Bundle], in case the
         * instance needs to be sent across a process. Consumers of this method should use
         * [fromBundle] to reconstruct the class instance back from the bundle returned here.
         */
        internal fun asBundle(response: GetAgeRangeResponse): Bundle {
            val bundle = Bundle()
            bundle.putInt(EXTRA_AGE_LOWER_BOUND, response.lowerAgeBound)
            response.upperAgeBound?.let { bundle.putInt(EXTRA_AGE_UPPER_BOUND, it) }
            bundle.putInt(EXTRA_AGE_ASSURANCE_TIER, response.assuranceTier)
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle] back to an instance of
         * [GetAgeRangeResponse], or `null` if the bundle is missing required entries or holds
         * values that violate the [GetAgeRangeResponse] invariants.
         *
         * An [AgeAssuranceTier] this version of the library does not recognize is passed through
         * as-is rather than rejected, so that a newer tier introduced by a provider does not cause
         * an otherwise valid response to be dropped. This matches the [GetAgeRangeResponse]
         * constructor, which likewise does not constrain the assurance tier.
         *
         * The bundle should be constructed and retrieved from [asBundle] itself and never be
         * created from scratch to avoid the failure.
         */
        internal fun fromBundle(bundle: Bundle): GetAgeRangeResponse? {
            if (!bundle.containsKey(EXTRA_AGE_LOWER_BOUND)) {
                Log.w(TAG, "Dropping response bundle: missing $EXTRA_AGE_LOWER_BOUND.")
                return null
            }
            if (!bundle.containsKey(EXTRA_AGE_ASSURANCE_TIER)) {
                Log.w(TAG, "Dropping response bundle: missing $EXTRA_AGE_ASSURANCE_TIER.")
                return null
            }

            val assuranceTier = bundle.getInt(EXTRA_AGE_ASSURANCE_TIER)
            val lowerAgeBound = bundle.getInt(EXTRA_AGE_LOWER_BOUND)
            val upperAgeBound =
                if (bundle.containsKey(EXTRA_AGE_UPPER_BOUND)) {
                    bundle.getInt(EXTRA_AGE_UPPER_BOUND)
                } else {
                    null
                }

            return try {
                GetAgeRangeResponse(lowerAgeBound, upperAgeBound, assuranceTier)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Dropping response bundle: it does not describe a valid age range.", e)
                null
            }
        }
    }
}
