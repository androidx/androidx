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
import androidx.aab.analysis.LibraryAnalysis.Companion.getLibraryAnalysis
import androidx.aab.analysis.ProfileAnalysis.Companion.getProfileAnalysis
import androidx.aab.analysis.R8Analysis.Companion.getR8Analysis
import androidx.aab.mapAll

/**
 * Container for analyzed apk information, grouped by analysis section.
 *
 * Note that unlike ApkInfo, this is intentionally not sorted by source of content, but rather
 * developer-facing perf areas
 */
class AnalyzedApkInfo(val apkInfo: ApkInfo) {
    val profileAnalysis: ProfileAnalysis = apkInfo.getProfileAnalysis()
    val r8Analysis: R8Analysis = apkInfo.getR8Analysis()
    val libraryAnalysis = apkInfo.getLibraryAnalysis()

    fun printAnalysis() {
        println("\n\n\nAnalysis for ${apkInfo.path}")
        listOf(profileAnalysis, r8Analysis, libraryAnalysis).map { it.getSubScore() }.print()
    }

    companion object {
        val CSV_COLUMNS =
            ProfileAnalysis.CSV_COLUMNS.mapAll<ProfileAnalysis, AnalyzedApkInfo> {
                it.profileAnalysis
            } +
                R8Analysis.CSV_COLUMNS.mapAll { it.r8Analysis } +
                ApkInfo.CSV_COLUMNS.mapAll { it.apkInfo }
    }
}
