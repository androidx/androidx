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

package androidx.aab.analysis

import androidx.aab.BundleInfo
import androidx.aab.R8JsonFileInfo

data class Issue(
    val severity: Severity,
    val title: String,
    val summary: String,
    val impact: String? = null,
    val suggestion: String? = null,
) {
    enum class Severity(val order: Int) {
        ERROR(order = 0),
        MISSING_METADATA(order = 1),
        WARNING(order = 2),
        INFO(order = 3),
    }
}

object BaselineProfileIssues {
    val EmptyProfile =
        Issue(
            severity = Issue.Severity.ERROR,
            title = "EMPTY BASELINE PROFILE",
            summary = "A baseline profile is present in this app bundle, but has no dex entries",
            impact = null,
            suggestion = null,
        )
    val MissingProfile =
        Issue(
            severity = Issue.Severity.WARNING,
            title = "MISSING BASELINE PROFILE",
            summary = "No baseline profiles present in this app bundle.",
            impact =
                """
            This will significantly reduce launch application performance as every
            time your application is updated it will have its entire compilation state
            reset entirely, exposing users to worst case JIT performance.
            """
                    .trimIndent(),
            suggestion =
                """
            Many standard AndroidX and 3rd party Android libraries have embedded
            library profiles for years. Ensure that you're using a recent enough
            version of AGP:
            https://developer.android.com/topic/performance/baselineprofiles/overview#recommended-versions
            Or if you're using a separate build system, ensure that library profiles
            (embedded in both jars and aars) are merged, and compiled by profgen
            (or profgen-cli).
        """
                    .trimIndent(),
        )

    fun ProfileAnalysis.getPartlyCorruptedProfileIssue() =
        Issue(
            severity = Issue.Severity.ERROR,
            title = "PARTIALLY CORRUPTED BASELINE PROFILE",
            summary =
                "Some baseline profiles embedded in the app bundle are corrupted," +
                    " ${dexCrc32ChecksumsProfileOnly.size} dex files referenced in profile not found.",
            impact =
                """
            This will significantly reduce launch application performance as every
            time your application is updated it will have its entire compilation state
            reset entirely, exposing users to worst case JIT performance.
            Baseline profiles should alleviate this problem, but this app's profiles
            will be ignored by the Android Runtime due to not matching any dex file.
        """
                    .trimIndent(),
            suggestion =
                """
            Ensure that the dex files produced by R8 (or in an unoptimized app, D8)
            are directly embedded in the bundle. If you're using a dex post-processing
            tool, verify that it is correctly consuming and translating baseline
            profiles along with dex code.
        """
                    .trimIndent(),
        )

    val FullyCorruptedProfile =
        Issue(
            severity = Issue.Severity.ERROR,
            title = "CORRUPTED BASELINE PROFILE",
            summary = "All baseline profiles embedded in the app bundle are corrupted.",
            impact =
                """
            This will significantly reduce launch application performance as every
            time your application is updated it will have its entire compilation state
            reset entirely, exposing users to worst case JIT performance.
            Baseline profiles should alleviate this problem, but this app's profiles
            will be ignored by the Android Runtime due to not matching any dex file.
        """
                    .trimIndent(),
            suggestion =
                """
            Ensure that the dex files produced by R8 (or in an unoptimized app, D8)
            are directly embedded in the bundle. If you're using a dex post-processing
            tool, verify that it is correctly consuming and translating baseline
            profiles along with dex code.
        """
                    .trimIndent(),
        )
}

object R8Issues {
    val MissingR8JsonMetadata =
        Issue(
            severity = Issue.Severity.MISSING_METADATA,
            title = "MISSING R8 METADATA",
            summary = "No r8 metadata is present in at ${R8JsonFileInfo.BUNDLE_LOCATION_R8}",
            impact =
                """
            This tool will not be able to report high level optimization quality
            metrics, or identify if important optimizations are missing.
        """
                    .trimIndent(),
            suggestion =
                """
            Ensure your app is using a sufficiently recent version of AGP (8.8+), or
            if you're using a separate build system, manually extract this from R8,
            for example with R8Command.Builder.setBuildMetadataConsumer(), and place
            this in the app bundle at ${R8JsonFileInfo.BUNDLE_LOCATION_R8}.
        """
                    .trimIndent(),
        )

    val DexChecksumsMismatched =
        Issue(
            severity = Issue.Severity.WARNING,
            title = "R8 DEX CHECKSUMS DO NOT MATCH DEX FILES",
            summary =
                "R8 metadata dex sha256 checksums present in at ${R8JsonFileInfo.BUNDLE_LOCATION_R8}" +
                    " do not match the dex files present in the bundle.",
            impact =
                """
            This tool will not be able to verify any optimizations performed by R8 are
            actually respected in the output dex. It's possible that all of the
            optimization from R8 have not actually been applied to your bundle.
        """
                    .trimIndent(),
            suggestion =
                """
            Ensure dex files produced by R8 are embedded into the app bundle, and any
            and all expected optimizations from R8 are preserved.
        """
                    .trimIndent(),
        )

    val NoMappingFileOrJsonMetadata =
        Issue(
            severity = Issue.Severity.WARNING,
            title = "LIKELY UNOPTIMIZED - MISSING MAPPING FILE AND R8 METADATA",
            summary = "It is likely that this application was not optimized with R8",
            impact =
                """
            This will significantly reduce performance and increase memory usage of this application.
        """
                    .trimIndent(),
            suggestion =
                """
            Enable R8: https://d.android.com/r8
        """
                    .trimIndent(),
        )

    fun R8JsonFileInfo.getPrimaryOptimizationIssue(): Issue? {
        if (!shrinkingEnabled || !obfuscationEnabled || !optimizationEnabled) {
            return Issue(
                severity = Issue.Severity.WARNING,
                title = "R8 - PRIMARY OPTIMIZATION DISABLED",
                summary =
                    """
                    |Application missing one of the following primary optimization flags from R8:
                    |    shrinking enabled    = $shrinkingEnabled
                    |    obfuscation enabled  = $obfuscationEnabled
                    |    optimization enabled = $optimizationEnabled
                """
                        .trimMargin(),
                impact =
                    """
            This will significantly reduce performance and increase memory usage
            of this application.
        """
                        .trimIndent(),
                suggestion =
                    """
            Avoid using any of the top level -dont*** flags in R8.
            When building with AGP, You can see your full R8 configuration at a path like:

                `.../build/outputs/mapping/release/configuration.txt`

            Find where these are set by searching this file for each of:
                -dontoptimize -dontshrink -dontoptimize
        """
                        .trimIndent(),
            )
        } else {
            return null
        }
    }
}

object LibraryVersionIssues {
    val MissingMetadata =
        Issue(
            severity = Issue.Severity.MISSING_METADATA,
            title = "MISSING VERSION INFO",
            summary =
                """
            No library version metadata present in either location:
                legacy = <...>/META-INF/*.version
                standard = ${BundleInfo.DEPENDENCIES_PB_LOCATION}
        """
                    .trimIndent(),
            impact =
                """
            This tool will skip several verifications, and library developers will not
            be able to notify this app of high impact security/crash issues via e.g.
            Play SDK Console.
        """
                    .trimIndent(),
            suggestion =
                """
            Either ensure this app is using a sufficiently recent version of
            AGP (TODO: VERSION), or if using a separate build system, ensure that your
            build embeds library information into ${BundleInfo.DEPENDENCIES_PB_LOCATION}.
        """
                    .trimIndent(),
        )
}
