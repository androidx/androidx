/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

internal data class FrameStatsResult(
    /**
     * Unique name of the window, from WindowManagerGlobal.getWindowName()
     *
     * e.g. "com.pkg/com.pkg.MyActivity/android.view.ViewRootImpl@ade24ea"
     */
    val uniqueName: String,

    /**
     * Most recent clock monotonic (e.g. System.nanoTime()) timestamp of a frame's vsync.
     */
    val lastFrameNs: Long?,

    /**
     * Most recent clock monotonic (e.g. System.nanoTime()) timestamp of a launch frame's vsync.
     *
     * Note: may be null pre-API 29, as it may fail to be detected
     */
    val lastLaunchNs: Long?
) {
    companion object {
        private val NAME_REGEX = Regex("(\\S+) \\(visibility=[0-9]+\\)")

        fun parse(frameStatsOutput: String): List<FrameStatsResult> {
            return frameStatsOutput
                .split(Regex("\r?\n?---PROFILEDATA---\r?\n?"))
                .chunked(2) // pair up metadata (with name) and associated profile data
                .filter { it.size == 2 } // ignore partial trailing item
                .map {
                    /**
                     * Example:
                     * ```
                     * com.pkg/com.pkg.MyActivity/android.view.ViewRootImpl@ade24ea (visibility=8)
                     * Window: com.pkg/com.pkg.MyActivity
                     * ```
                     */
                    val (uniqueName) = it[0]
                        .split("\r?\n")
                        .firstNotNullOf { line ->
                            NAME_REGEX.find(line)
                        }.destructured

                    val profileData = it[1]
                    FrameStatsResult(
                        uniqueName = uniqueName,
                        lastFrameNs = profileDataLatestActivityLaunchNs(
                            profileData,
                            requireFlag = false
                        ),
                        lastLaunchNs = profileDataLatestActivityLaunchNs(
                            profileData,
                            requireFlag = true
                        )
                    )
                }
        }

        /**
         * Returns latest activity launch from profile data sections between `--PROFILEDATA--`
         * markers in `dumpsys gfxinfo <pkg> framestats` output
         *
         * For example, in the following output:
         * ```
         * Flags,IntendedVsync,//...
         * 1,3211995467212,//...
         * 0,3212079627183,//...
         * 1,5077693738881,//...
         * 0,6038928372818,//...
         * ```
         *
         * Would return `5077693738881` - most recent intended vsync of frame with
         * 0x1 flag from table
         */
        private fun profileDataLatestActivityLaunchNs(
            profileData: String,
            requireFlag: Boolean
        ): Long? {
            val lines = profileData.split(Regex("\r?\n"))

            val columnLabels = lines.first().split(",")
            val flagsIndex = columnLabels.indexOf("Flags")
            val intendedVsyncIndex = columnLabels.indexOf("IntendedVsync")

            lines.forEachIndexed { index, s -> println("$index $s") }
            return lines
                .drop(1)
                .mapNotNull {
                    val columns = it.split(",")
                    if (!requireFlag || (columns[flagsIndex].toLong() and 0x1L) != 0L) {
                        // 0x1L mask means initial frame
                        columns[intendedVsyncIndex].toLong()
                    } else {
                        null
                    }
                }
                .maxOfOrNull { it }
        }
    }
}
