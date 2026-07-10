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
import androidx.aab.CsvColumn
import androidx.aab.analysis.LibraryAnalysis.Companion.getLibraryAnalysis
import androidx.aab.analysis.ProfileAnalysis.Companion.getProfileAnalysis
import androidx.aab.analysis.R8Analysis.Companion.getR8Analysis
import androidx.aab.mapAll

/**
 * Container for analyzed bundle information, grouped by analysis section.
 *
 * Note that unlike BundleInfo, this is intentionally not sorted by source of content, but rather
 * developer-facing perf areas
 */
class AnalyzedBundleInfo(val bundleInfo: BundleInfo) {
    val profileAnalysis: ProfileAnalysis = bundleInfo.getProfileAnalysis()
    val r8Analysis: R8Analysis = bundleInfo.getR8Analysis()
    val libraryAnalysis = bundleInfo.getLibraryAnalysis()

    fun printAnalysis() {
        println("\n\n\nAnalysis for ${bundleInfo.path}")
        listOf(profileAnalysis, r8Analysis, libraryAnalysis).map { it.getSubScore() }.print()
    }

    companion object {
        val CSV_COLUMNS: List<CsvColumn<AnalyzedBundleInfo>> =
            ProfileAnalysis.CSV_COLUMNS.mapAll<ProfileAnalysis, AnalyzedBundleInfo> {
                it.profileAnalysis
            } +
                R8Analysis.CSV_COLUMNS.mapAll { it.r8Analysis } +
                LibraryAnalysis.CSV_COLUMNS.mapAll { it.libraryAnalysis } +
                BundleInfo.CSV_COLUMNS.mapAll { it.bundleInfo }
    }
}
