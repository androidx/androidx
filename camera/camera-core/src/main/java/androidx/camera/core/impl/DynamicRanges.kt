/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.camera.core.impl

import androidx.camera.core.DynamicRange
import androidx.core.util.Preconditions

/** Utility methods for handling dynamic range. */
public object DynamicRanges {

    /**
     * Returns `true` if the test dynamic range can resolve to the fully specified dynamic range
     * set.
     *
     * A range can resolve if test fields are unspecified and appropriately match the fields of the
     * fully specified dynamic range, or the test fields exactly match the fields of the fully
     * specified dynamic range.
     */
    @JvmStatic
    public fun canResolve(
        dynamicRangeToTest: DynamicRange,
        fullySpecifiedDynamicRanges: Set<DynamicRange>,
    ): Boolean {
        return if (dynamicRangeToTest.isFullySpecified) {
            fullySpecifiedDynamicRanges.contains(dynamicRangeToTest)
        } else {
            fullySpecifiedDynamicRanges.firstOrNull { fullySpecifiedDynamicRange ->
                canResolveUnderSpecifiedTo(dynamicRangeToTest, fullySpecifiedDynamicRange)
            } != null
        }
    }

    /**
     * Returns a set of all possible matches from a set of dynamic ranges that may contain
     * under-specified dynamic ranges to a set that contains only fully-specified dynamic ranges.
     *
     * A dynamic range can resolve if test fields are unspecified and appropriately match the fields
     * of the fully specified dynamic range, or the test fields exactly match the fields of the
     * fully specified dynamic range.
     */
    @JvmStatic
    public fun findAllPossibleMatches(
        dynamicRangesToTest: Set<DynamicRange>,
        fullySpecifiedDynamicRanges: Set<DynamicRange>
    ): Set<DynamicRange> {
        if (dynamicRangesToTest.isEmpty()) {
            throw IllegalArgumentException(
                "Candidate dynamic range set must contain at least 1 candidate dynamic range."
            )
        }
        return buildSet {
            dynamicRangesToTest.forEach {
                if (it.isFullySpecified) {
                    // Add matching fully-specified dynamic ranges directly
                    if (fullySpecifiedDynamicRanges.contains(it)) {
                        add(it)
                    }
                } else {
                    // Iterate through fully-specified dynamic ranges to find which could be used
                    // by the corresponding under-specified dynamic ranges
                    fullySpecifiedDynamicRanges.forEach { fullySpecifiedDynamicRange ->
                        if (canResolveUnderSpecifiedTo(it, fullySpecifiedDynamicRange)) {
                            add(fullySpecifiedDynamicRange)
                        }
                    }
                }
            }
        }
    }

    private fun canResolveUnderSpecifiedTo(
        underSpecifiedDynamicRange: DynamicRange,
        fullySpecifiedDynamicRange: DynamicRange
    ): Boolean {
        return canMatchBitDepth(underSpecifiedDynamicRange, fullySpecifiedDynamicRange) &&
            canMatchEncoding(underSpecifiedDynamicRange, fullySpecifiedDynamicRange)
    }

    private fun canMatchBitDepth(
        dynamicRangeToTest: DynamicRange,
        fullySpecifiedDynamicRange: DynamicRange
    ): Boolean {
        Preconditions.checkState(
            fullySpecifiedDynamicRange.isFullySpecified,
            "Fully specified " + "range is not actually fully specified."
        )
        return if (dynamicRangeToTest.bitDepth == DynamicRange.BIT_DEPTH_UNSPECIFIED) {
            true
        } else {
            dynamicRangeToTest.bitDepth == fullySpecifiedDynamicRange.bitDepth
        }
    }

    private fun canMatchEncoding(
        dynamicRangeToTest: DynamicRange,
        fullySpecifiedDynamicRange: DynamicRange
    ): Boolean {
        Preconditions.checkState(
            fullySpecifiedDynamicRange.isFullySpecified,
            "Fully specified " + "range is not actually fully specified."
        )
        val encodingToTest = dynamicRangeToTest.encoding
        if (encodingToTest == DynamicRange.ENCODING_UNSPECIFIED) {
            return true
        }
        val fullySpecifiedEncoding = fullySpecifiedDynamicRange.encoding
        return if (
            encodingToTest == DynamicRange.ENCODING_HDR_UNSPECIFIED &&
                fullySpecifiedEncoding != DynamicRange.ENCODING_SDR
        ) {
            true
        } else {
            encodingToTest == fullySpecifiedEncoding
        }
    }
}
