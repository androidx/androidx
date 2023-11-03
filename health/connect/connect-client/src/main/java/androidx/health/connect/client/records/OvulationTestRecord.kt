/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Each record represents the result of an ovulation test. */
public class OvulationTestRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /**
     * The result of a user's ovulation test, which shows if they're ovulating or not. Required
     * field.
     */
    @property:Results public val result: Int,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OvulationTestRecord) return false

        if (result != other.result) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = result.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    /** The result of a user's ovulation test. */
    internal object Result {
        const val POSITIVE = "positive"
        const val HIGH = "high"
        const val NEGATIVE = "negative"
        const val INCONCLUSIVE = "inconclusive"
    }

    companion object {
        /**
         * Inconclusive result. Refers to ovulation test results that are indeterminate (e.g. may be
         * testing malfunction, user error, etc.). ". Any unknown value will also be returned as
         * [RESULT_INCONCLUSIVE].
         */
        const val RESULT_INCONCLUSIVE = 0

        /**
         * Positive fertility (may also be referred as "peak" fertility). Refers to the peak of the
         * luteinizing hormone (LH) surge and ovulation is expected to occur in 10-36 hours.
         */
        const val RESULT_POSITIVE = 1

        /**
         * High fertility. Refers to a rise in estrogen or luteinizing hormone that may signal the
         * fertile window (time in the menstrual cycle when conception is likely to occur).
         */
        const val RESULT_HIGH = 2

        /**
         * Negative fertility (may also be referred as "low" fertility). Refers to the time in the
         * cycle where fertility/conception is expected to be low.
         */
        const val RESULT_NEGATIVE = 3

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val RESULT_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                Result.INCONCLUSIVE to RESULT_INCONCLUSIVE,
                Result.POSITIVE to RESULT_POSITIVE,
                Result.HIGH to RESULT_HIGH,
                Result.NEGATIVE to RESULT_NEGATIVE
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val RESULT_INT_TO_STRING_MAP = RESULT_STRING_TO_INT_MAP.reverse()
    }

    /**
     * The result of a user's ovulation test.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [RESULT_INCONCLUSIVE, RESULT_POSITIVE, RESULT_HIGH, RESULT_NEGATIVE])
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class Results
}
