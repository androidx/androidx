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

import androidx.aab.ApkInfo
import androidx.aab.BundleInfo
import androidx.aab.CsvColumn
import androidx.aab.DexInfo
import androidx.aab.ProfInfo
import androidx.aab.analysis.BaselineProfileIssues.getPartlyCorruptedProfileIssue
import kotlin.math.roundToInt

data class ProfileAnalysis(
    val status: Status,
    val corruptionRatio: Double,
    val dexCrc32ChecksumsMatching: Set<String>,
    val dexCrc32ChecksumsProfileOnly: Set<String>,
    val dexCrc32ChecksumsDexOnly: Set<String>,
    // TODO: Profile method / class presence percentage,
    //  e.g. 100% of methods in profile vs 0.1% of methods in profile
) : ScoreReporter {
    enum class Status {
        MISSING,
        EMPTY,
        PARTLY_CORRUPTED,
        FULLY_CORRUPTED,
        OK,
    }

    fun getScore() =
        when (status) {
            Status.MISSING,
            Status.EMPTY,
            Status.FULLY_CORRUPTED -> 0
            Status.PARTLY_CORRUPTED -> (20 * corruptionRatio).roundToInt()
            Status.OK -> 20
        }

    override fun getSubScore(): SubScore {
        val score: Int = getScore()

        val issue =
            when (status) {
                Status.MISSING -> BaselineProfileIssues.MissingProfile
                Status.EMPTY -> BaselineProfileIssues.EmptyProfile
                Status.PARTLY_CORRUPTED -> this.getPartlyCorruptedProfileIssue()
                Status.FULLY_CORRUPTED -> BaselineProfileIssues.FullyCorruptedProfile
                Status.OK -> null
            }
        return SubScore("Baseline Profile", score, 20, listOfNotNull(issue))
    }

    companion object {
        val CSV_COLUMNS =
            listOf(
                CsvColumn<ProfileAnalysis>(
                    "profile_score",
                    "experimental - Score for profile quality, out of 20",
                    requiresVerbose = true,
                    calculate = { it.getScore().toString() },
                ),
                CsvColumn(
                    "profile_status",
                    "Status of baseline profile (detects presence and corruption)",
                    requiresVerbose = false,
                    calculate = { it.status.toString() },
                ),
            )

        fun getProfileAnalysis(dexInfo: List<DexInfo>, profileInfo: ProfInfo?): ProfileAnalysis {
            val setOfDexCrc32FromDex = (dexInfo.map { it.crc32 }.toSet())
            val setOfDexCrc32FromProfiles =
                profileInfo?.profDexInfoList?.map { it.checksumCrc32 }?.toSet() ?: emptySet()

            val dexCrc32ChecksumsMatching =
                setOfDexCrc32FromDex.intersect(setOfDexCrc32FromProfiles)
            val dexCrc32ChecksumsProfileOnly = setOfDexCrc32FromProfiles - setOfDexCrc32FromDex
            val dexCrc32ChecksumsDexOnly = setOfDexCrc32FromDex - setOfDexCrc32FromProfiles

            val status =
                when {
                    profileInfo == null -> Status.MISSING
                    profileInfo.profDexInfoList.isEmpty() -> Status.EMPTY
                    dexCrc32ChecksumsProfileOnly.isNotEmpty() &&
                        dexCrc32ChecksumsMatching.isEmpty() -> Status.FULLY_CORRUPTED
                    dexCrc32ChecksumsProfileOnly.isNotEmpty() -> Status.PARTLY_CORRUPTED
                    else -> Status.OK
                }

            return ProfileAnalysis(
                status = status,
                corruptionRatio =
                    if (setOfDexCrc32FromProfiles.isNotEmpty()) {
                        dexCrc32ChecksumsProfileOnly.size * 1.0 / (setOfDexCrc32FromProfiles.size)
                    } else 0.0,
                dexCrc32ChecksumsMatching = dexCrc32ChecksumsMatching,
                dexCrc32ChecksumsProfileOnly = dexCrc32ChecksumsProfileOnly,
                dexCrc32ChecksumsDexOnly = dexCrc32ChecksumsDexOnly,
            )
        }

        fun ApkInfo.getProfileAnalysis() = getProfileAnalysis(dexInfo, profileInfo)

        fun BundleInfo.getProfileAnalysis() = getProfileAnalysis(dexInfo, profileInfo)
    }
}
