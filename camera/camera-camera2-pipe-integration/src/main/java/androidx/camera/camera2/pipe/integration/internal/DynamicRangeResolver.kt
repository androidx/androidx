package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.DynamicRangeProfilesCompat
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.UseCaseConfig
import androidx.core.util.Preconditions

public class DynamicRangeResolver(public val cameraMetadata: CameraMetadata) {
    private val is10BitSupported: Boolean
    private val dynamicRangesInfo: DynamicRangeProfilesCompat

    init {
        val availableCapabilities: IntArray? =
            cameraMetadata[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
        is10BitSupported =
            availableCapabilities?.contains(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
            ) ?: false
        dynamicRangesInfo = DynamicRangeProfilesCompat.fromCameraMetaData(cameraMetadata)
    }

    /** Returns whether 10-bit dynamic ranges are supported on this device. */
    public fun is10BitDynamicRangeSupported(): Boolean = is10BitSupported

    /**
     * Returns a set of supported dynamic ranges for the dynamic ranges requested by the list of
     * attached and new use cases.
     *
     * If a new use case requests a dynamic range that isn't supported, an IllegalArgumentException
     * will be thrown.
     */
    public fun resolveAndValidateDynamicRanges(
        existingSurfaces: List<AttachedSurfaceInfo>,
        newUseCaseConfigs: List<UseCaseConfig<*>>,
        useCasePriorityOrder: List<Int>
    ): Map<UseCaseConfig<*>, DynamicRange> {
        // Create an ordered set of already-attached surface's dynamic ranges. These are assumed
        // to be valid since they are already attached.
        val orderedExistingDynamicRanges = mutableSetOf<DynamicRange>()
        for (asi in existingSurfaces) {
            orderedExistingDynamicRanges.add(asi.dynamicRange)
        }

        // Get the supported dynamic ranges from the device
        val supportedDynamicRanges = dynamicRangesInfo.supportedDynamicRanges

        // Collect initial dynamic range constraints. This set will potentially shrink as we add
        // more dynamic ranges. We start with the initial set of supported dynamic ranges to
        // denote no constraints.
        val combinedConstraints = supportedDynamicRanges.toMutableSet()
        for (dynamicRange in orderedExistingDynamicRanges) {
            updateConstraints(combinedConstraints, dynamicRange, dynamicRangesInfo)
        }

        // We want to resolve and validate dynamic ranges in the following order:
        // 1. First validate fully defined dynamic ranges. No resolving is required here.
        // 2. Resolve and validate partially defined dynamic ranges, such as HDR_UNSPECIFIED or
        // dynamic ranges with concrete encodings but BIT_DEPTH_UNSPECIFIED. We can now potentially
        // infer a dynamic range based on constraints of the fully defined dynamic ranges or
        // the list of supported HDR dynamic ranges.
        // 3. Finally, resolve and validate UNSPECIFIED dynamic ranges. These will resolve
        // to dynamic ranges from the first 2 groups, or fall back to SDR if no other dynamic
        // ranges are defined.
        //
        // To accomplish this, we need to partition the use cases into 3 categories.
        val orderedFullyDefinedUseCaseConfigs: MutableList<UseCaseConfig<*>> = mutableListOf()
        val orderedPartiallyDefinedUseCaseConfigs: MutableList<UseCaseConfig<*>> = mutableListOf()
        val orderedUndefinedUseCaseConfigs: MutableList<UseCaseConfig<*>> = mutableListOf()
        for (priorityIdx in useCasePriorityOrder) {
            val config = newUseCaseConfigs[priorityIdx]
            val requestedDynamicRange = config.dynamicRange
            if (isFullyUnspecified(requestedDynamicRange)) {
                orderedUndefinedUseCaseConfigs.add(config)
            } else if (isPartiallySpecified(requestedDynamicRange)) {
                orderedPartiallyDefinedUseCaseConfigs.add(config)
            } else {
                orderedFullyDefinedUseCaseConfigs.add(config)
            }
        }
        val resolvedDynamicRanges: MutableMap<UseCaseConfig<*>, DynamicRange> = mutableMapOf()
        // Keep track of new dynamic ranges for more fine-grained error messages in exceptions.
        // This allows us to differentiate between dynamic ranges from already-attached use cases
        // and requested dynamic ranges from newly added use cases.
        val orderedNewDynamicRanges: MutableSet<DynamicRange> = mutableSetOf()
        // Now resolve and validate all of the dynamic ranges in order of the 3 partitions form
        // above.
        val orderedUseCaseConfigs: MutableList<UseCaseConfig<*>> = mutableListOf()
        orderedUseCaseConfigs.addAll(orderedFullyDefinedUseCaseConfigs)
        orderedUseCaseConfigs.addAll(orderedPartiallyDefinedUseCaseConfigs)
        orderedUseCaseConfigs.addAll(orderedUndefinedUseCaseConfigs)
        for (config in orderedUseCaseConfigs) {
            val resolvedDynamicRange: DynamicRange =
                resolveDynamicRangeAndUpdateConstraints(
                    supportedDynamicRanges,
                    orderedExistingDynamicRanges,
                    orderedNewDynamicRanges,
                    config,
                    combinedConstraints
                )
            resolvedDynamicRanges[config] = resolvedDynamicRange
            if (!orderedExistingDynamicRanges.contains(resolvedDynamicRange)) {
                orderedNewDynamicRanges.add(resolvedDynamicRange)
            }
        }
        return resolvedDynamicRanges
    }

    private fun resolveDynamicRangeAndUpdateConstraints(
        supportedDynamicRanges: Set<DynamicRange?>,
        orderedExistingDynamicRanges: Set<DynamicRange>,
        orderedNewDynamicRanges: Set<DynamicRange>,
        config: UseCaseConfig<*>,
        outCombinedConstraints: MutableSet<DynamicRange>
    ): DynamicRange {
        val requestedDynamicRange = config.dynamicRange
        val resolvedDynamicRange: DynamicRange? =
            resolveDynamicRange(
                requestedDynamicRange,
                outCombinedConstraints,
                orderedExistingDynamicRanges,
                orderedNewDynamicRanges,
                config.targetName
            )
        if (resolvedDynamicRange != null) {
            updateConstraints(outCombinedConstraints, resolvedDynamicRange, dynamicRangesInfo)
        } else {
            throw IllegalArgumentException(
                "Unable to resolve supported " +
                    "dynamic range. The dynamic range may not be supported on the device " +
                    "or may not be allowed concurrently with other attached use cases.\n" +
                    "Use case:\n" +
                    "  ${config.targetName}\n" +
                    "Requested dynamic range:\n" +
                    "  $requestedDynamicRange\n" +
                    "Supported dynamic ranges:\n" +
                    "  $supportedDynamicRanges\n" +
                    "Constrained set of concurrent dynamic ranges:\n" +
                    "  $outCombinedConstraints",
            )
        }
        return resolvedDynamicRange
    }

    /**
     * Resolves the requested dynamic range into a fully specified dynamic range.
     *
     * This uses existing fully-specified dynamic ranges, new fully-specified dynamic ranges,
     * dynamic range constraints and the list of supported dynamic ranges to exhaustively search for
     * a dynamic range if the requested dynamic range is not fully specified, i.e., it has an
     * UNSPECIFIED encoding or UNSPECIFIED bitrate.
     *
     * Any dynamic range returned will be validated to work according to the constraints and
     * supported dynamic ranges provided.
     *
     * If no suitable dynamic range can be found, returns `null`.
     */
    private fun resolveDynamicRange(
        requestedDynamicRange: DynamicRange,
        combinedConstraints: Set<DynamicRange>,
        orderedExistingDynamicRanges: Set<DynamicRange>,
        orderedNewDynamicRanges: Set<DynamicRange>,
        rangeOwnerLabel: String
    ): DynamicRange? {

        // Dynamic range is already resolved if it is fully specified.
        if (requestedDynamicRange.isFullySpecified) {
            return if (combinedConstraints.contains(requestedDynamicRange)) {
                requestedDynamicRange
            } else null
            // Requested dynamic range is full specified but unsupported. No need to continue
            // trying to resolve.
        }

        // Explicitly handle the case of SDR with unspecified bit depth.
        // SDR is only supported as 8-bit.
        val requestedEncoding = requestedDynamicRange.encoding
        val requestedBitDepth = requestedDynamicRange.bitDepth
        if (
            requestedEncoding == DynamicRange.ENCODING_SDR &&
                requestedBitDepth == DynamicRange.BIT_DEPTH_UNSPECIFIED
        ) {
            return if (combinedConstraints.contains(DynamicRange.SDR)) {
                DynamicRange.SDR
            } else null
            // If SDR isn't supported, we can't resolve to any other dynamic range.
        }

        // First attempt to find another fully specified HDR dynamic range to resolve to from
        // existing dynamic ranges
        var resolvedDynamicRange =
            findSupportedHdrMatch(
                requestedDynamicRange,
                orderedExistingDynamicRanges,
                combinedConstraints
            )
        if (resolvedDynamicRange != null) {
            Log.debug {
                "DynamicRangeResolver: Resolved dynamic range for use case $rangeOwnerLabel " +
                    "from existing attached surface.\n" +
                    "$requestedDynamicRange\n->\n$resolvedDynamicRange"
            }

            return resolvedDynamicRange
        }

        // Attempt to find another fully specified HDR dynamic range to resolve to from
        // new dynamic ranges
        resolvedDynamicRange =
            findSupportedHdrMatch(
                requestedDynamicRange,
                orderedNewDynamicRanges,
                combinedConstraints
            )
        if (resolvedDynamicRange != null) {
            Log.debug {
                "DynamicRangeResolver: Resolved dynamic range for use case $rangeOwnerLabel from " +
                    "concurrently bound use case." +
                    "\n$requestedDynamicRange\n->\n$resolvedDynamicRange"
            }

            return resolvedDynamicRange
        }

        // Now that we have checked existing HDR dynamic ranges, we must resolve fully unspecified
        // and unspecified 8-bit dynamic ranges to SDR if it is supported. This ensures the
        // default behavior for most use cases is to choose SDR when an HDR dynamic range isn't
        // already present or explicitly requested.
        if (
            canResolveWithinConstraints(
                requestedDynamicRange,
                DynamicRange.SDR,
                combinedConstraints
            )
        ) {
            Log.debug {
                "DynamicRangeResolver: Resolved dynamic range for use case $rangeOwnerLabel to " +
                    "no compatible HDR dynamic ranges.\n$requestedDynamicRange\n" +
                    "->\n${DynamicRange.SDR}"
            }
            return DynamicRange.SDR
        }

        // For unspecified HDR encodings (10-bit or unspecified bit depth), we have a
        // couple options: the device recommended 10-bit encoding or the mandated HLG encoding.
        if (
            requestedEncoding == DynamicRange.ENCODING_HDR_UNSPECIFIED &&
                ((requestedBitDepth == DynamicRange.BIT_DEPTH_10_BIT ||
                    requestedBitDepth == DynamicRange.BIT_DEPTH_UNSPECIFIED))
        ) {
            val hdrDefaultRanges: MutableSet<DynamicRange> = mutableSetOf()

            // Attempt to use the recommended 10-bit dynamic range
            var recommendedRange: DynamicRange? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                recommendedRange = Api33Impl.getRecommended10BitDynamicRange(cameraMetadata)
                if (recommendedRange != null) {
                    hdrDefaultRanges.add(recommendedRange)
                }
            }
            // Attempt to fall back to HLG since it is a mandated required 10-bit
            // dynamic range.
            hdrDefaultRanges.add(DynamicRange.HLG_10_BIT)
            resolvedDynamicRange =
                findSupportedHdrMatch(requestedDynamicRange, hdrDefaultRanges, combinedConstraints)
            if (resolvedDynamicRange != null) {
                Log.debug {
                    "DynamicRangeResolver: Resolved dynamic range for use case $rangeOwnerLabel" +
                        "from ${
                            if ((resolvedDynamicRange == recommendedRange)) "recommended"
                            else "required"
                        } 10-bit supported dynamic range.\n" +
                        "${requestedDynamicRange}\n" +
                        "->\n" +
                        "$resolvedDynamicRange"
                }
                return resolvedDynamicRange
            }
        }

        // Finally, attempt to find an HDR dynamic range for HDR or 10-bit dynamic ranges from
        // the constraints of the other validated dynamic ranges. If there are no other dynamic
        // ranges, this should be the full list of supported dynamic ranges.
        // The constraints are unordered, so it may not produce an "optimal" dynamic range. This
        // works for 8-bit, 10-bit or partially specified HDR dynamic ranges.
        for (candidateRange: DynamicRange in combinedConstraints) {
            check(candidateRange.isFullySpecified) {
                "Candidate dynamic range must be fully specified."
            }

            // Only consider HDR constraints
            if ((candidateRange == DynamicRange.SDR)) {
                continue
            }
            if (canResolveDynamicRange(requestedDynamicRange, candidateRange)) {
                Log.debug {
                    "DynamicRangeResolver: Resolved dynamic range for use case $rangeOwnerLabel " +
                        "from validated dynamic range constraints or supported HDR dynamic " +
                        "ranges.\n$requestedDynamicRange\n->\n$candidateRange"
                }
                return candidateRange
            }
        }

        // Unable to resolve dynamic range
        return null
    }

    /**
     * Updates the provided dynamic range constraints by combining them with the new constraints
     * from the new dynamic range.
     *
     * @param combinedConstraints The constraints that will be updated. This set must not be empty.
     * @param newDynamicRange The new dynamic range for which we'll apply new constraints
     * @param dynamicRangesInfo Information about dynamic ranges to retrieve new constraints.
     */
    private fun updateConstraints(
        combinedConstraints: MutableSet<DynamicRange>,
        newDynamicRange: DynamicRange,
        dynamicRangesInfo: DynamicRangeProfilesCompat
    ) {
        Preconditions.checkState(
            combinedConstraints.isNotEmpty(),
            "Cannot update already-empty constraints."
        )
        val newConstraints =
            dynamicRangesInfo.getDynamicRangeCaptureRequestConstraints(newDynamicRange)
        if (newConstraints.isNotEmpty()) {
            // Retain for potential exception message
            val previousConstraints = combinedConstraints.toSet()
            // Take the intersection of constraints
            combinedConstraints.retainAll(newConstraints)
            // This shouldn't happen if we're diligent about checking that dynamic range
            // is within the existing constraints before attempting to call
            // updateConstraints. If it happens, then the dynamic ranges are not mutually
            // compatible.
            require(combinedConstraints.isNotEmpty()) {
                "Constraints of dynamic " +
                    "range cannot be combined with existing constraints.\n" +
                    "Dynamic range:\n" +
                    "  $newDynamicRange\n" +
                    "Constraints:\n" +
                    "  $newConstraints\n" +
                    "Existing constraints:\n" +
                    "  $previousConstraints"
            }
        }
    }

    private fun findSupportedHdrMatch(
        rangeToMatch: DynamicRange,
        fullySpecifiedCandidateRanges: Collection<DynamicRange>,
        constraints: Set<DynamicRange>
    ): DynamicRange? {
        // SDR can never match with HDR
        if (rangeToMatch.encoding == DynamicRange.ENCODING_SDR) {
            return null
        }
        for (candidateRange in fullySpecifiedCandidateRanges) {
            val candidateEncoding = candidateRange.encoding
            check(candidateRange.isFullySpecified) {
                "Fully specified DynamicRange must have fully defined encoding."
            }
            if (candidateEncoding == DynamicRange.ENCODING_SDR) {
                // Only consider HDR encodings
                continue
            }
            if (canResolveWithinConstraints(rangeToMatch, candidateRange, constraints)) {
                return candidateRange
            }
        }
        return null
    }

    /** Returns `true` if the dynamic range is ENCODING_UNSPECIFIED and BIT_DEPTH_UNSPECIFIED. */
    private fun isFullyUnspecified(dynamicRange: DynamicRange): Boolean {
        return (dynamicRange == DynamicRange.UNSPECIFIED)
    }

    /**
     * Returns `true` if the dynamic range has an unspecified HDR encoding, a concrete encoding with
     * unspecified bit depth, or a concrete bit depth.
     */
    private fun isPartiallySpecified(dynamicRange: DynamicRange): Boolean {
        return dynamicRange.encoding == DynamicRange.ENCODING_HDR_UNSPECIFIED ||
            (dynamicRange.encoding != DynamicRange.ENCODING_UNSPECIFIED &&
                dynamicRange.bitDepth == DynamicRange.BIT_DEPTH_UNSPECIFIED) ||
            (dynamicRange.encoding == DynamicRange.ENCODING_UNSPECIFIED &&
                dynamicRange.bitDepth != DynamicRange.BIT_DEPTH_UNSPECIFIED)
    }

    /**
     * Returns `true` if the test dynamic range can resolve to the candidate, fully specified
     * dynamic range, taking into account constraints.
     *
     * A range can resolve if test fields are unspecified and appropriately match the fields of the
     * fully specified dynamic range, or the test fields exactly match the fields of the fully
     * specified dynamic range.
     */
    private fun canResolveWithinConstraints(
        rangeToResolve: DynamicRange,
        candidateRange: DynamicRange,
        constraints: Set<DynamicRange>
    ): Boolean {
        if (!constraints.contains(candidateRange)) {
            Log.debug {
                "DynamicRangeResolver: Candidate Dynamic range is not within constraints.\n" +
                    "Dynamic range to resolve:\n" +
                    "  $rangeToResolve\n" +
                    "Candidate dynamic range:\n" +
                    "  $candidateRange"
            }
            return false
        }
        return canResolveDynamicRange(rangeToResolve, candidateRange)
    }

    /**
     * Returns `true` if the test dynamic range can resolve to the fully specified dynamic range.
     *
     * A range can resolve if test fields are unspecified and appropriately match the fields of the
     * fully specified dynamic range, or the test fields exactly match the fields of the fully
     * specified dynamic range.
     */
    private fun canResolveDynamicRange(
        testRange: DynamicRange,
        fullySpecifiedRange: DynamicRange
    ): Boolean {
        check(fullySpecifiedRange.isFullySpecified) {
            "Fully specified range $fullySpecifiedRange not actually fully specified."
        }
        if (
            (testRange.encoding == DynamicRange.ENCODING_HDR_UNSPECIFIED &&
                fullySpecifiedRange.encoding == DynamicRange.ENCODING_SDR)
        ) {
            return false
        }
        return if (
            (testRange.encoding != DynamicRange.ENCODING_HDR_UNSPECIFIED) &&
                (testRange.encoding != DynamicRange.ENCODING_UNSPECIFIED) &&
                (testRange.encoding != fullySpecifiedRange.encoding)
        ) {
            false
        } else
            (testRange.bitDepth == DynamicRange.BIT_DEPTH_UNSPECIFIED ||
                testRange.bitDepth == fullySpecifiedRange.bitDepth)
    }

    @RequiresApi(33)
    internal object Api33Impl {
        fun getRecommended10BitDynamicRange(cameraMetadata: CameraMetadata): DynamicRange? {
            val recommendedProfile =
                cameraMetadata[
                    CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE]
            return if (recommendedProfile != null) {
                DynamicRangeConversions.profileToDynamicRange(recommendedProfile)
            } else null
        }
    }
}
