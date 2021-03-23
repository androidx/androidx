/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark

import android.util.Log
import java.io.File
import java.io.IOException

internal object CpuInfo {
    private const val TAG = "Benchmark"

    val coreDirs: List<CoreDir>
    val locked: Boolean
    val maxFreqHz: Long

    /**
     * Representation of clock info in `/sys/devices/system/cpu/cpu#/`
     */
    data class CoreDir(
        // online, or true if can't access
        val online: Boolean,

        // sorted list of scaling_available_frequencies, or listOf(-1) if can't access
        val availableFreqs: List<Int>,

        // scaling_min_freq, or -1 if can't access
        val currentMinFreq: Int,

        // cpuinfo_max_freq, or -1 if can't access
        val maxFreqKhz: Long
    )

    init {
        val cpuDir = File("/sys/devices/system/cpu")
        coreDirs = cpuDir.list { current, name ->
            File(current, name).isDirectory && name.matches(Regex("^cpu[0-9]+"))
        }?.map {
            val path = "${cpuDir.path}/$it"
            CoreDir(
                // online, or true if can't access
                online = readFileTextOrNull("$path/online") != "0",

                // sorted list of scaling_available_frequencies, or listOf(-1) if can't access
                availableFreqs = readFileTextOrNull("$path/cpufreq/scaling_available_frequencies")
                    ?.split(Regex("\\s+"))
                    ?.filter { it.isNotBlank() }
                    ?.map { Integer.parseInt(it) }
                    ?.sorted()
                    ?: listOf(-1),

                // scaling_min_freq, or -1 if can't access
                currentMinFreq = readFileTextOrNull("$path/cpufreq/scaling_min_freq")?.toInt()
                    ?: -1,
                maxFreqKhz = readFileTextOrNull("$path/cpufreq/cpuinfo_max_freq")?.toLong() ?: -1L
            )
        } ?: emptyList()

        maxFreqHz = coreDirs
            .filter { it.maxFreqKhz != -1L }
            .maxByOrNull { it.maxFreqKhz }
            ?.maxFreqKhz?.times(1000) ?: -1

        locked = isCpuLocked(coreDirs)
        coreDirs.forEachIndexed { index, coreDir ->
            Log.d(TAG, "cpu$index $coreDir")
        }
    }

    fun isCpuLocked(coreDirs: List<CoreDir>): Boolean {
        val onlineCores = coreDirs.filter { it.online }

        if (onlineCores.any {
            it.availableFreqs.maxOrNull() != onlineCores[0].availableFreqs.maxOrNull()
        }
        ) {
            Log.d(TAG, "Clocks not locked: cores with different max frequencies")
            return false
        }

        if (onlineCores.any { it.currentMinFreq != onlineCores[0].currentMinFreq }) {
            Log.d(TAG, "Clocks not locked: cores with different current min freq")
            return false
        }

        if (onlineCores.any { it.availableFreqs.minOrNull() == it.currentMinFreq }) {
            Log.d(TAG, "Clocks not locked: online cores with min freq == min avail freq")
            return false
        }

        return true
    }

    /**
     * Read the text of a file as a String, null if file doesn't exist or can't be read.
     */
    private fun readFileTextOrNull(path: String): String? {
        try {
            File(path).run {
                return if (exists()) {
                    readText().trim()
                } else null
            }
        } catch (e: IOException) {
            return null
        }
    }
}
