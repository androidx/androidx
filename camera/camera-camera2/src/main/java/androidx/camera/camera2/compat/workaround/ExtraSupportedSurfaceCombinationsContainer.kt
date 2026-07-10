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

package androidx.camera.camera2.compat.workaround

import androidx.camera.camera2.compat.quirk.DeviceQuirks
import androidx.camera.camera2.compat.quirk.ExtraSupportedSurfaceCombinationsQuirk
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import java.util.Locale

/**
 * A container that manages extra supported surface combinations.
 *
 * It combines combinations provided by device quirks with those injected via
 * [androidx.camera.core.CameraXConfig].
 */
public class ExtraSupportedSurfaceCombinationsContainer(
    extraSupportedSurfaceCombinationsStr: String? = null
) {
    private val quirk: ExtraSupportedSurfaceCombinationsQuirk? =
        DeviceQuirks[ExtraSupportedSurfaceCombinationsQuirk::class.java]

    private val overrideCombinations: List<SurfaceCombinationOverride> =
        parseCombinations(extraSupportedSurfaceCombinationsStr)

    private data class SurfaceCombinationOverride(
        val cameraId: String?,
        val combination: SurfaceCombination,
    )

    private fun parseCombinations(configStr: String?): List<SurfaceCombinationOverride> {
        if (configStr.isNullOrEmpty()) return emptyList()

        return configStr
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { comboStr -> parseSingleCombination(comboStr) }
    }

    private fun parseSingleCombination(comboStr: String): SurfaceCombinationOverride? {
        if (!COMBINATION_REGEX.matches(comboStr)) {
            Camera2Logger.error { "Invalid surface combination format: '$comboStr'" }
            return null
        }

        var cameraId: String? = null
        var configsPart = comboStr
        if (comboStr.contains("=")) {
            val parts = comboStr.split("=", limit = 2)
            cameraId = parts[0].trim()
            configsPart = parts[1].trim()
        }

        val surfaceConfigs =
            configsPart
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { surfaceStr ->
                    parseSurfaceConfig(surfaceStr, comboStr) ?: return null // Discard entire combo
                }

        if (surfaceConfigs.isEmpty()) return null

        val surfaceCombination =
            SurfaceCombination().apply { surfaceConfigs.forEach { addSurfaceConfig(it) } }

        Camera2Logger.info {
            "Parsed extra surface combination '$configsPart'" +
                (if (cameraId != null) " for camera $cameraId" else "")
        }

        return SurfaceCombinationOverride(cameraId, surfaceCombination)
    }

    private fun parseSurfaceConfig(surfaceStr: String, context: String): SurfaceConfig? {
        val parts = surfaceStr.split(":")
        if (parts.size != 2) {
            Camera2Logger.error { "Invalid surface config format '$surfaceStr' in '$context'." }
            return null
        }

        return try {
            val type = ConfigType.valueOf(parts[0].trim().uppercase(Locale.US))
            val size = ConfigSize.valueOf(parts[1].trim().uppercase(Locale.US))
            SurfaceConfig.create(type, size)
        } catch (e: Exception) {
            Camera2Logger.error(e) {
                "Failed to parse config '$surfaceStr' in '$context'. " +
                    "Valid types: ${ConfigType.entries}, " +
                    "Valid sizes: ${ConfigSize.entries}"
            }
            null
        }
    }

    /** Retrieves the extra surface combinations which can be supported on the device. */
    public operator fun get(cameraId: String): List<SurfaceCombination> {
        val combinations = mutableListOf<SurfaceCombination>()
        quirk?.getExtraSupportedSurfaceCombinations(cameraId)?.let { combinations.addAll(it) }

        overrideCombinations
            .filter { it.cameraId == null || it.cameraId == cameraId }
            .forEach { override ->
                combinations.add(override.combination)
                Camera2Logger.debug {
                    "Added extra surface combination for camera $cameraId: " + override.combination
                }
            }

        return combinations
    }

    private companion object {
        // Regex to validate the overall format: [cameraId=]TYPE:SIZE, TYPE:SIZE, ...
        // Camera ID: [a-zA-Z0-9]+
        // Type/Size: [a-zA-Z0-9_]+
        private val COMBINATION_REGEX =
            Regex(
                "^(([a-zA-Z0-9]+)=)?([a-zA-Z0-9_]+:[a-zA-Z0-9_]+)(,\\s*[a-zA-Z0-9_]+:[a-zA-Z0-9_]+)*$"
            )
    }
}
