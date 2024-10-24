/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.text.trim

@Composable
fun PerformanceDemos() {

    val numProcessors = Runtime.getRuntime().availableProcessors()
    val maxCpuClockSpeed = getMaxCpuClockSpeed()
    val totalMemory = getTotalMemory()

    ScalingLazyDemo {
        item { ListHeader { Text("Performance Stats") } }
        item { Statistic("Number of processors", "$numProcessors", numProcessors > 2) }
        item { Statistic("Max clock speed", "$maxCpuClockSpeed", maxCpuClockSpeed > 1000) }
        items(numProcessors) {
            val currentCpuClockSpeed = getCurrentCpuClockSpeed(it)
            Statistic(
                "Current clock speed $it",
                "$currentCpuClockSpeed",
                currentCpuClockSpeed > 1000
            )
        }
        item { Statistic("Total memory", "$totalMemory", totalMemory > 1000f) }
    }
}

@Composable
private fun Statistic(label: String, value: String, passed: Boolean) {
    ChildButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        icon = { PassFailIcon(passed) },
        label = { Text("$label = $value") }
    )
}

@Composable
private fun PassFailIcon(passed: Boolean) {
    Icon(
        imageVector = if (passed) Icons.Filled.Check else Icons.Filled.Close,
        contentDescription = if (passed) "Passed" else "Failed",
        tint = if (passed) Color.Green else Color.Red
    )
}

private fun getMaxCpuClockSpeed(): Float {
    val cpuInfoFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
    if (cpuInfoFile.exists()) {
        return cpuInfoFile.readText().trim().toFloat() / 1000f // Convert to MHz
    }
    return -1f // Indicate error or unavailable information
}

private fun getCurrentCpuClockSpeed(index: Int): Float {
    val cpuInfoFile = File("/sys/devices/system/cpu/cpu${index}/cpufreq/scaling_cur_freq")
    if (cpuInfoFile.exists()) {
        return cpuInfoFile.readText().trim().toFloat() / 1000f // Convert to MHz
    }
    return -1f // Indicate error or unavailable information
}

private fun getTotalMemory(): Float {
    try {
        val reader = BufferedReader(FileReader("/proc/meminfo"))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.startsWith("MemTotal:")) {
                val parts = line!!.split("\\s+".toRegex())
                return parts[1].toFloat() / 1024f // Convert to MB
            }
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return -1f // Indicate error or unavailable information
}
