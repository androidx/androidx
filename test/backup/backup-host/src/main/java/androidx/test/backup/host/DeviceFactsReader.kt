/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

import com.android.adblib.DevicePropertyNames
import java.util.Locale
import kotlinx.coroutines.CancellationException

/** Holds static device metadata read once per device serial per test run. */
internal data class DeviceFacts(
    val model: String,
    val api: Int,
    val image: String,
    val playServices: String?,
)

/** Reads device metadata and formats summary attributes for [TestDetailRecord] entries. */
internal object DeviceFactsReader {

    private val GENERIC_MODEL_PREFIXES =
        listOf("sdk_gphone", "sdk_", "android sdk built for", "aosp on ")

    private val AVD_API_SUFFIX = Regex("""(?i)[_-]api[_-]?\d+.*$""")
    private val WHITESPACE_OR_UNDERSCORE = Regex("""[_ ]+""")

    /** Reads model, system image flavor, and Google Play services version from [shell]. */
    suspend fun read(shell: BackupDeviceShell, apiLevel: Int): DeviceFacts {
        val props = safeCall { shell.readonlyProperties() }.orEmpty()
        val model =
            resolveModelName(
                emuModel = props["ro.emu.model"],
                productModel = props[DevicePropertyNames.RO_PRODUCT_MODEL],
                avdName =
                    props[DevicePropertyNames.RO_BOOT_QEMU_AVD_NAME]
                        ?: props[DevicePropertyNames.RO_KERNEL_QEMU_AVD_NAME],
                fallbackSerial = shell.serialNumber,
            )

        val playServices = safeCall {
            shell.packageVersionCode("com.google.android.gms")
        }
            ?.let(::gmsVersionName)
        val hasPlayStore = safeCall { shell.isPackageInstalled("com.android.vending") } == true
        val image =
            resolveImageLabel(
                flavor = props["ro.build.flavor"],
                characteristics = props[DevicePropertyNames.RO_BUILD_CHARACTERISTICS],
                tags = props[DevicePropertyNames.RO_BUILD_TAGS],
                hasPlayStore = hasPlayStore,
                hasGms = playServices != null,
            )

        return DeviceFacts(
            model = model,
            api = apiLevel,
            image = image,
            playServices = playServices,
        )
    }

    /** Formats a Google Play services [versionCode] as a dotted `XX.YY.ZZ` version string. */
    internal fun gmsVersionName(versionCode: Long): String {
        val version = versionCode / 1000L
        if (version <= 0L) return versionCode.toString()
        return String.format(
            Locale.ROOT,
            "%d.%02d.%02d",
            version / 10000L,
            version / 100L % 100L,
            version % 100L,
        )
    }

    internal fun resolveModelName(
        emuModel: String?,
        productModel: String?,
        avdName: String?,
        fallbackSerial: String,
    ): String {
        emuModel
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isGenericSdkModel(it) }
            ?.let {
                return cleanDisplayName(it)
            }
        productModel
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !isGenericSdkModel(it) }
            ?.let {
                return it
            }
        avdName
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { rawAvd ->
                val stripped = rawAvd.replace(AVD_API_SUFFIX, "")
                val cleaned = cleanDisplayName(stripped)
                if (cleaned.isNotEmpty()) return cleaned
            }
        return productModel?.trim()?.takeIf { it.isNotEmpty() } ?: fallbackSerial
    }

    internal fun resolveImageLabel(
        flavor: String?,
        characteristics: String?,
        tags: String?,
        hasPlayStore: Boolean,
        hasGms: Boolean,
    ): String {
        val hint =
            listOfNotNull(flavor, characteristics, tags).joinToString(" ").lowercase(Locale.ROOT)
        return when {
            hasPlayStore || "playstore" in hint || "google_play" in hint -> "Google Play"
            hasGms || "google_apis" in hint -> "Google APIs"
            else -> "AOSP"
        }
    }

    internal fun transportDisplayValue(mode: BackupTransportMode): String =
        when (mode) {
            BackupTransportMode.LOCAL -> "Local"
            BackupTransportMode.DEVICE_TO_DEVICE -> "Device to device"
            BackupTransportMode.CLOUD_ENCRYPTED -> "Cloud (encrypted)"
            BackupTransportMode.CLOUD_UNENCRYPTED -> "Cloud (unencrypted)"
            else -> mode.toString()
        }

    internal fun buildSummaryAttributes(
        transportMode: BackupTransportMode?,
        actors: List<TestDetailRecord.ActorRecord>,
    ): List<TestDetailRecord.SummaryAttribute> = buildList {
        if (transportMode != null) {
            add(
                TestDetailRecord.SummaryAttribute(
                    key = "transport",
                    label = "Transport",
                    value = transportMode.toString(),
                    displayValue = transportDisplayValue(transportMode),
                    filterable = true,
                )
            )
        }
        if (actors.isNotEmpty()) {
            val devicesValue = actors.joinToString(" -> ") { "${it.name} (API ${it.api})" }
            add(
                TestDetailRecord.SummaryAttribute(
                    key = "devices",
                    label = "Devices",
                    value = devicesValue,
                    displayValue = devicesValue,
                    filterable = true,
                )
            )
        }
    }

    private fun isGenericSdkModel(value: String): Boolean {
        val lower = value.lowercase(Locale.ROOT)
        return GENERIC_MODEL_PREFIXES.any { lower.startsWith(it) }
    }

    private fun cleanDisplayName(raw: String): String =
        raw.replace(WHITESPACE_OR_UNDERSCORE, " ").trim()

    private inline fun <T> safeCall(block: () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
}
