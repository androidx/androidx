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

package androidx.camera.camera2.internal;

import static android.hardware.camera2.CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE;

import static androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_HDR_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_SDR;
import static androidx.camera.core.DynamicRange.FORMAT_UNSPECIFIED;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.params.DynamicRangeConversions;
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.AttachedSurfaceInfo;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves and validates dynamic ranges based on device capabilities and constraints.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class DynamicRangeResolver {
    private static final String TAG = "DynamicRangeResolver";
    private static final DynamicRange DYNAMIC_RANGE_HLG10 =
            new DynamicRange(DynamicRange.FORMAT_HLG, DynamicRange.BIT_DEPTH_10_BIT);
    private final CameraCharacteristicsCompat mCharacteristics;
    private final DynamicRangesCompat mDynamicRangesInfo;
    private final boolean mIs10BitSupported;

    DynamicRangeResolver(@NonNull CameraCharacteristicsCompat characteristics) {
        mCharacteristics = characteristics;
        mDynamicRangesInfo = DynamicRangesCompat.fromCameraCharacteristics(characteristics);

        int[] availableCapabilities =
                mCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        boolean is10BitSupported = false;
        if (availableCapabilities != null) {
            for (int capability : availableCapabilities) {
                if (capability == CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT) {
                    is10BitSupported = true;
                    break;
                }
            }
        }
        mIs10BitSupported = is10BitSupported;
    }

    /**
     * Returns whether 10-bit dynamic ranges are supported on this device.
     */
    boolean is10BitDynamicRangeSupported() {
        return mIs10BitSupported;
    }

    /**
     * Returns a set of supported dynamic ranges for the dynamic ranges requested by the list of
     * attached and new use cases.
     *
     * <p>If a new use case requests a dynamic range that isn't supported, an
     * IllegalArgumentException will be thrown.
     */
    Map<UseCaseConfig<?>, DynamicRange> resolveAndValidateDynamicRanges(
            @NonNull List<AttachedSurfaceInfo> existingSurfaces,
            @NonNull List<UseCaseConfig<?>> newUseCaseConfigs,
            @NonNull List<Integer> useCasePriorityOrder) {
        // Create an ordered set of already-attached surface's dynamic ranges. These are assumed
        // to be valid since they are already attached.
        Set<DynamicRange> orderedExistingDynamicRanges = new LinkedHashSet<>();
        for (AttachedSurfaceInfo asi : existingSurfaces) {
            orderedExistingDynamicRanges.add(asi.getDynamicRange());
        }

        // Get the supported dynamic ranges from the device
        Set<DynamicRange> supportedDynamicRanges = mDynamicRangesInfo.getSupportedDynamicRanges();

        // Collect initial dynamic range constraints. This set will potentially shrink as we add
        // more dynamic ranges. We start with the initial set of supported dynamic ranges to
        // denote no constraints.
        Set<DynamicRange> combinedConstraints = new HashSet<>(supportedDynamicRanges);
        for (DynamicRange dynamicRange : orderedExistingDynamicRanges) {
            updateConstraints(combinedConstraints, dynamicRange, mDynamicRangesInfo);
        }

        // We want to resolve and validate dynamic ranges in the following order:
        // 1. First validate fully defined dynamic ranges. No resolving is required here.
        // 2. Resolve and validate partially defined dynamic ranges, such as HDR_UNSPECIFIED or
        // dynamic ranges with concrete formats but BIT_DEPTH_UNSPECIFIED. We can now potentially
        // infer a dynamic range based on constraints of the fully defined dynamic ranges or
        // the list of supported HDR dynamic ranges.
        // 3. Finally, resolve and validate UNSPECIFIED dynamic ranges. These will resolve
        // to dynamic ranges from the first 2 groups, or fall back to SDR if no other dynamic
        // ranges are defined.
        //
        // To accomplish this, we need to partition the use cases into 3 categories.
        List<UseCaseConfig<?>> orderedFullyDefinedUseCaseConfigs = new ArrayList<>();
        List<UseCaseConfig<?>> orderedPartiallyDefinedUseCaseConfigs = new ArrayList<>();
        List<UseCaseConfig<?>> orderedUndefinedUseCaseConfigs = new ArrayList<>();
        for (int priorityIdx : useCasePriorityOrder) {
            UseCaseConfig<?> config = newUseCaseConfigs.get(priorityIdx);
            DynamicRange requestedDynamicRange = config.getDynamicRange();
            if (isFullyUnspecified(requestedDynamicRange)) {
                orderedUndefinedUseCaseConfigs.add(config);
            } else if (isPartiallySpecified(requestedDynamicRange)) {
                orderedPartiallyDefinedUseCaseConfigs.add(config);
            } else {
                orderedFullyDefinedUseCaseConfigs.add(config);
            }
        }

        Map<UseCaseConfig<?>, DynamicRange> resolvedDynamicRanges = new HashMap<>();
        // Keep track of new dynamic ranges for more fine-grained error messages in exceptions.
        // This allows us to differentiate between dynamic ranges from already-attached use cases
        // and requested dynamic ranges from newly added use cases.
        Set<DynamicRange> orderedNewDynamicRanges = new LinkedHashSet<>();
        // Now resolve and validate all of the dynamic ranges in order of the 3 partitions form
        // above.
        List<UseCaseConfig<?>> orderedUseCaseConfigs = new ArrayList<>();
        orderedUseCaseConfigs.addAll(orderedFullyDefinedUseCaseConfigs);
        orderedUseCaseConfigs.addAll(orderedPartiallyDefinedUseCaseConfigs);
        orderedUseCaseConfigs.addAll(orderedUndefinedUseCaseConfigs);
        for (UseCaseConfig<?> config : orderedUseCaseConfigs) {
            DynamicRange resolvedDynamicRange = resolveDynamicRangeAndUpdateConstraints(
                    supportedDynamicRanges, orderedExistingDynamicRanges,
                    orderedNewDynamicRanges, config, combinedConstraints);
            resolvedDynamicRanges.put(config, resolvedDynamicRange);
            if (!orderedExistingDynamicRanges.contains(resolvedDynamicRange)) {
                orderedNewDynamicRanges.add(resolvedDynamicRange);
            }
        }

        return resolvedDynamicRanges;
    }

    private DynamicRange resolveDynamicRangeAndUpdateConstraints(
            @NonNull Set<DynamicRange> supportedDynamicRanges,
            @NonNull Set<DynamicRange> orderedExistingDynamicRanges,
            @NonNull Set<DynamicRange> orderedNewDynamicRanges,
            @NonNull UseCaseConfig<?> config,
            @NonNull Set<DynamicRange> outCombinedConstraints) {
        DynamicRange requestedDynamicRange = config.getDynamicRange();
        DynamicRange resolvedDynamicRange = resolveDynamicRange(requestedDynamicRange,
                outCombinedConstraints, orderedExistingDynamicRanges, orderedNewDynamicRanges,
                config.getTargetName());

        if (resolvedDynamicRange != null) {
            updateConstraints(outCombinedConstraints, resolvedDynamicRange, mDynamicRangesInfo);
        } else {
            throw new IllegalArgumentException(String.format("Unable to resolve supported "
                            + "dynamic range. The dynamic range may not be supported on the device "
                            + "or may not be allowed concurrently with other attached use cases.\n"
                            + "Use case:\n"
                            + "  %s\n"
                            + "Requested dynamic range:\n"
                            + "  %s\n"
                            + "Supported dynamic ranges:\n"
                            + "  %s\n"
                            + "Constrained set of concurrent dynamic ranges:\n"
                            + "  %s",
                    config.getTargetName(), requestedDynamicRange,
                    TextUtils.join("\n  ", supportedDynamicRanges),
                    TextUtils.join("\n  ", outCombinedConstraints)));
        }

        return resolvedDynamicRange;

    }

    /**
     * Resolves the requested dynamic range into a fully specified dynamic range.
     *
     * <p>This uses existing fully-specified dynamic ranges, new fully-specified dynamic ranges,
     * dynamic range constraints and the list of supported dynamic ranges to exhaustively search
     * for a dynamic range if the requested dynamic range is not fully specified, i.e., it has an
     * UNSPECIFIED format or UNSPECIFIED bitrate.
     *
     * <p>Any dynamic range returned will be validated to work according to the constraints and
     * supported dynamic ranges provided.
     *
     * <p>If no suitable dynamic range can be found, returns {@code null}.
     */
    @Nullable
    private DynamicRange resolveDynamicRange(
            @NonNull DynamicRange requestedDynamicRange,
            @NonNull Set<DynamicRange> combinedConstraints,
            @NonNull Set<DynamicRange> orderedExistingDynamicRanges,
            @NonNull Set<DynamicRange> orderedNewDynamicRanges,
            @NonNull String rangeOwnerLabel) {

        // Dynamic range is already resolved if it is fully specified.
        if (isFullySpecified(requestedDynamicRange)) {
            if (combinedConstraints.contains(requestedDynamicRange)) {
                return requestedDynamicRange;
            }
            // Requested dynamic range is full specified but unsupported. No need to continue
            // trying to resolve.
            return null;
        }

        // Explicitly handle the case of SDR with unspecified bit depth.
        // SDR is only supported as 8-bit.
        int requestedFormat = requestedDynamicRange.getFormat();
        int requestedBitDepth = requestedDynamicRange.getBitDepth();
        if (requestedFormat == FORMAT_SDR && requestedBitDepth == BIT_DEPTH_UNSPECIFIED) {
            if (combinedConstraints.contains(DynamicRange.SDR)) {
                return DynamicRange.SDR;
            }
            // If SDR isn't supported, we can't resolve to any other dynamic range.
            return null;
        }

        // First attempt to find another fully specified HDR dynamic range to resolve to from
        // existing dynamic ranges
        DynamicRange resolvedDynamicRange = findSupportedHdrMatch(requestedDynamicRange,
                orderedExistingDynamicRanges, combinedConstraints);
        if (resolvedDynamicRange != null) {
            Logger.d(TAG, String.format("Resolved dynamic range for use case %s from existing "
                            + "attached surface.\n%s\n->\n%s",
                    rangeOwnerLabel, requestedDynamicRange, resolvedDynamicRange));
            return resolvedDynamicRange;
        }

        // Attempt to find another fully specified HDR dynamic range to resolve to from
        // new dynamic ranges
        resolvedDynamicRange = findSupportedHdrMatch(requestedDynamicRange,
                orderedNewDynamicRanges, combinedConstraints);
        if (resolvedDynamicRange != null) {
            Logger.d(TAG, String.format("Resolved dynamic range for use case %s from "
                            + "concurrently bound use case.\n%s\n->\n%s",
                    rangeOwnerLabel, requestedDynamicRange, resolvedDynamicRange));
            return resolvedDynamicRange;
        }

        // Now that we have checked existing HDR dynamic ranges, we must resolve fully unspecified
        // and unspecified 8-bit dynamic ranges to SDR if it is supported. This ensures the
        // default behavior for most use cases is to choose SDR when an HDR dynamic range isn't
        // already present or explicitly requested.
        if (canResolveWithinConstraints(requestedDynamicRange, DynamicRange.SDR,
                combinedConstraints)) {
            Logger.d(TAG, String.format(
                    "Resolved dynamic range for use case %s to no "
                            + "compatible HDR dynamic ranges.\n%s\n->\n%s",
                    rangeOwnerLabel, requestedDynamicRange, DynamicRange.SDR));
            return DynamicRange.SDR;
        }

        // For unspecified HDR formats (10-bit or unspecified bit depth), we have a
        // couple options: the device recommended 10-bit format or the mandated HLG format.
        if (requestedFormat == FORMAT_HDR_UNSPECIFIED && (
                requestedBitDepth == DynamicRange.BIT_DEPTH_10_BIT
                        || requestedBitDepth == BIT_DEPTH_UNSPECIFIED)) {
            Set<DynamicRange> hdrDefaultRanges = new LinkedHashSet<>();

            // Attempt to use the recommended 10-bit dynamic range
            DynamicRange recommendedRange = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                recommendedRange = Api33Impl.getRecommended10BitDynamicRange(mCharacteristics);
                if (recommendedRange != null) {
                    hdrDefaultRanges.add(recommendedRange);
                }
            }
            // Attempt to fall back to HLG since it is a mandated required 10-bit
            // dynamic range.
            hdrDefaultRanges.add(DYNAMIC_RANGE_HLG10);
            resolvedDynamicRange = findSupportedHdrMatch(requestedDynamicRange,
                    hdrDefaultRanges, combinedConstraints);
            if (resolvedDynamicRange != null) {
                Logger.d(TAG, String.format(
                        "Resolved dynamic range for use case %s from %s "
                                + "10-bit supported dynamic range.\n%s\n->\n%s",
                        rangeOwnerLabel,
                        Objects.equals(resolvedDynamicRange, recommendedRange) ? "recommended"
                                : "required",
                        requestedDynamicRange, resolvedDynamicRange));
                return resolvedDynamicRange;
            }
        }

        // Finally, attempt to find an HDR dynamic range for HDR or 10-bit dynamic ranges from
        // the constraints of the other validated dynamic ranges. If there are no other dynamic
        // ranges, this should be the full list of supported dynamic ranges.
        // The constraints are unordered, so it may not produce an "optimal" dynamic range. This
        // works for 8-bit, 10-bit or partially specified HDR dynamic ranges.
        for (DynamicRange candidateRange : combinedConstraints) {
            Preconditions.checkState(isFullySpecified(candidateRange), "Candidate dynamic"
                    + " range must be fully specified.");

            // Only consider HDR constraints
            if (candidateRange.equals(DynamicRange.SDR)) {
                continue;
            }

            if (canResolve(requestedDynamicRange, candidateRange)) {
                Logger.d(TAG, String.format(
                        "Resolved dynamic range for use case %s from validated "
                                + "dynamic range constraints or supported HDR dynamic "
                                + "ranges.\n%s\n->\n%s",
                        rangeOwnerLabel, requestedDynamicRange, candidateRange));
                return candidateRange;
            }
        }

        // Unable to resolve dynamic range
        return null;
    }

    /**
     * Updates the provided dynamic range constraints by combining them with the new constraints
     * from the new dynamic range.
     *
     * @param combinedConstraints The constraints that will be updated. This set must not be empty.
     * @param newDynamicRange     The new dynamic range for which we'll apply new constraints
     * @param dynamicRangesInfo   Information about dynamic ranges to retrieve new constraints.
     */
    private static void updateConstraints(
            @NonNull Set<DynamicRange> combinedConstraints,
            @NonNull DynamicRange newDynamicRange,
            @NonNull DynamicRangesCompat dynamicRangesInfo) {
        Preconditions.checkState(!combinedConstraints.isEmpty(), "Cannot update already-empty "
                + "constraints.");
        Set<DynamicRange> newConstraints =
                dynamicRangesInfo.getDynamicRangeCaptureRequestConstraints(newDynamicRange);
        if (!newConstraints.isEmpty()) {
            // Retain for potential exception message
            Set<DynamicRange> previousConstraints = new HashSet<>(combinedConstraints);
            // Take the intersection of constraints
            combinedConstraints.retainAll(newConstraints);
            if (combinedConstraints.isEmpty()) {
                // This shouldn't happen if we're diligent about checking that dynamic range
                // is within the existing constraints before attempting to call
                // updateConstraints. If it happens, then the dynamic ranges are not mutually
                // compatible.
                throw new IllegalArgumentException(String.format("Constraints of dynamic "
                                + "range cannot be combined with existing constraints.\n"
                                + "Dynamic range:\n"
                                + "  %s\n"
                                + "Constraints:\n"
                                + "  %s\n"
                                + "Existing constraints:\n"
                                + "  %s",
                        newDynamicRange, TextUtils.join("\n  ", newConstraints),
                        TextUtils.join("\n  ", previousConstraints)));
            }
        }
    }

    @Nullable
    private static DynamicRange findSupportedHdrMatch(@NonNull DynamicRange rangeToMatch,
            @NonNull Collection<DynamicRange> fullySpecifiedCandidateRanges,
            @NonNull Set<DynamicRange> constraints) {
        // SDR can never match with HDR
        if (rangeToMatch.getFormat() == FORMAT_SDR) {
            return null;
        }

        for (DynamicRange candidateRange : fullySpecifiedCandidateRanges) {
            Preconditions.checkNotNull(candidateRange,
                    "Fully specified DynamicRange cannot be null.");
            int candidateFormat = candidateRange.getFormat();
            Preconditions.checkState(isFullySpecified(candidateRange),
                    "Fully specified DynamicRange must have fully defined format.");
            if (candidateFormat == FORMAT_SDR) {
                // Only consider HDR formats
                continue;
            }

            if (canResolveWithinConstraints(rangeToMatch, candidateRange, constraints)) {
                return candidateRange;
            }
        }
        return null;
    }

    @RequiresApi(33)
    static final class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        @Nullable
        static DynamicRange getRecommended10BitDynamicRange(
                @NonNull CameraCharacteristicsCompat characteristics) {
            Long recommendedProfile =
                    characteristics.get(REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE);
            if (recommendedProfile != null) {
                return DynamicRangeConversions.profileToDynamicRange(recommendedProfile);
            }
            return null;
        }
    }

    /**
     * Returns {@code true} if the dynamic range is FORMAT_UNSPECIFIED and BIT_DEPTH_UNSPECIFIED.
     */
    private static boolean isFullyUnspecified(@NonNull DynamicRange dynamicRange) {
        return Objects.equals(dynamicRange, DynamicRange.UNSPECIFIED);
    }

    /**
     * Returns {@code true} if both the format and bit depth are not unspecified types.
     */
    private static boolean isFullySpecified(@NonNull DynamicRange dynamicRange) {
        return dynamicRange.getFormat() != FORMAT_UNSPECIFIED
                && dynamicRange.getFormat() != FORMAT_HDR_UNSPECIFIED
                && dynamicRange.getBitDepth() != BIT_DEPTH_UNSPECIFIED;
    }

    /**
     * Returns {@code true} if the dynamic range has an unspecified HDR format, a concrete
     * format with unspecified bit depth, or a concrete bit depth.
     */
    private static boolean isPartiallySpecified(@NonNull DynamicRange dynamicRange) {
        return dynamicRange.getFormat() == FORMAT_HDR_UNSPECIFIED || (
                dynamicRange.getFormat() != FORMAT_UNSPECIFIED
                        && dynamicRange.getBitDepth() == BIT_DEPTH_UNSPECIFIED) || (
                                dynamicRange.getFormat() == FORMAT_UNSPECIFIED
                                        && dynamicRange.getBitDepth() != BIT_DEPTH_UNSPECIFIED);
    }

    /**
     * Returns {@code true} if the test dynamic range can resolve to the candidate, fully specified
     * dynamic range, taking into account constraints.
     *
     * <p>A range can resolve if test fields are unspecified and appropriately match the fields
     * of the fully specified dynamic range, or the test fields exactly match the fields of
     * the fully specified dynamic range.
     */
    private static boolean canResolveWithinConstraints(@NonNull DynamicRange rangeToResolve,
            @NonNull DynamicRange candidateRange,
            @NonNull Set<DynamicRange> constraints) {
        if (!constraints.contains(candidateRange)) {
            Logger.d(TAG, String.format("Candidate Dynamic range is not within constraints.\n"
                            + "Dynamic range to resolve:\n"
                            + "  %s\n"
                            + "Candidate dynamic range:\n"
                            + "  %s",
                    rangeToResolve, candidateRange));
            return false;
        }

        return canResolve(rangeToResolve, candidateRange);
    }

    /**
     * Returns {@code true} if the test dynamic range can resolve to the fully specified dynamic
     * range.
     *
     * <p>A range can resolve if test fields are unspecified and appropriately match the fields
     * of the fully specified dynamic range, or the test fields exactly match the fields of
     * the fully specified dynamic range.
     */
    private static boolean canResolve(@NonNull DynamicRange testRange,
            @NonNull DynamicRange fullySpecifiedRange) {
        Preconditions.checkState(isFullySpecified(fullySpecifiedRange), "Fully specified range is"
                + " not actually fully specified.");
        if (testRange.getFormat() == FORMAT_HDR_UNSPECIFIED
                && fullySpecifiedRange.getFormat() == FORMAT_SDR) {
            return false;
        }

        if (testRange.getFormat() != FORMAT_HDR_UNSPECIFIED
                && testRange.getFormat() != FORMAT_UNSPECIFIED
                && testRange.getFormat() != fullySpecifiedRange.getFormat()) {
            return false;
        }

        return testRange.getBitDepth() == BIT_DEPTH_UNSPECIFIED
                || testRange.getBitDepth() == fullySpecifiedRange.getBitDepth();
    }
}
