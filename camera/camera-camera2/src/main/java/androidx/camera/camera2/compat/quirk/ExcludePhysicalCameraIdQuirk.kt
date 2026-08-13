/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk
import java.util.Locale

/**
 * Quirk that excludes physical camera ID usage on problematic devices.
 *
 * <p>QuirkSummary
 * - Bug Id: b/545953123
 * - Description: Some devices have buggy HALs that crash, timeout, or fail to produce frames when
 *   attempting to configure or stream from physical camera IDs, even if they advertise support for
 *   logical multi-cameras.
 * - Device(s): Samsung Galaxy S25 / S25 Ultra, Samsung Galaxy Z Fold 6 / Z Flip 6, Samsung Galaxy
 *   S24, OPPO Find N2 Flip, Xiaomi 15 Pro / 15 Ultra, Sony Xperia devices.
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class ExcludePhysicalCameraIdQuirk : Quirk {
    public val excludedPhysicalCameraIds: Set<String> = loadExcludedPhysicalCameraIds()

    public companion object {
        private val SAMSUNG_PROBLEM_MODELS =
            mapOf(
                "SM-S938" to setOf("2", "3", "4"), // Samsung S25 Ultra
                "SM-S931" to setOf("2", "3", "4"), // Samsung S25
                "SM-F968" to setOf("2", "3", "4"), // Samsung Z Fold 6 Ultra
                "SM-F966" to setOf("2", "5", "6"), // Samsung Z Fold 6 (US)
                "SM-F956" to setOf("2", "5", "6"), // Samsung Z Fold 6 (Global)
                "SM-F766" to setOf("2", "3", "4"), // Samsung Z Flip 6
                "SM-F741" to setOf("2", "5"), // Samsung Z Flip 6 (Global)
                "SM-S921" to setOf("2", "6"), // Samsung S24
            )

        private val OPPO_PROBLEM_MODELS =
            mapOf(
                "CPH2437" to setOf("2") // OPPO Find N2 Flip
            )

        private val XIAOMI_PROBLEM_MODELS =
            mapOf(
                "24129PN74" to setOf("2", "3", "4"), // Xiaomi 15 Pro
                "25010PN30" to setOf("2", "3", "4"), // Xiaomi 15 Ultra
            )

        private val SONY_PROBLEM_MODELS =
            mapOf(
                "SO-41" to setOf("0", "2", "4"), // Sony Xperia 10 II / Ace II
                "SO-52" to setOf("2", "3", "4"), // Sony Xperia 10 III / IV
                "XQ-DQ72" to setOf("2", "3", "4"), // Sony Xperia 1 V
                "XQ-DC54" to setOf("2", "3", "4"), // Sony Xperia 10 V
            )

        @Volatile
        private var cachedExcludedPhysicalCameraIds:
            Pair<Triple<String, String, String>, Set<String>>? =
            null

        public fun isEnabled(): Boolean {
            return loadExcludedPhysicalCameraIds().isNotEmpty()
        }

        internal fun loadExcludedPhysicalCameraIds(): Set<String> {
            val brand = Build.BRAND.trim().uppercase(Locale.US)
            val manufacturer = Build.MANUFACTURER.trim().uppercase(Locale.US)
            val model = Build.MODEL.trim().uppercase(Locale.US)

            val currentDevice = Triple(brand, manufacturer, model)
            cachedExcludedPhysicalCameraIds?.let { (cachedDevice, cachedIds) ->
                if (cachedDevice == currentDevice) {
                    return cachedIds
                }
            }

            val modelsMap =
                when {
                    brand.contains("SAMSUNG") || manufacturer.contains("SAMSUNG") ->
                        SAMSUNG_PROBLEM_MODELS
                    brand.contains("OPPO") || manufacturer.contains("OPPO") -> OPPO_PROBLEM_MODELS
                    brand.contains("XIAOMI") || manufacturer.contains("XIAOMI") ->
                        XIAOMI_PROBLEM_MODELS
                    brand.contains("SONY") || manufacturer.contains("SONY") -> SONY_PROBLEM_MODELS
                    else -> emptyMap()
                }

            val excludedIds =
                modelsMap.entries
                    .filter { (prefix, _) -> model.startsWith(prefix) }
                    .maxByOrNull { (prefix, _) -> prefix.length }
                    ?.value ?: emptySet()

            cachedExcludedPhysicalCameraIds = Pair(currentDevice, excludedIds)
            return excludedIds
        }
    }
}
