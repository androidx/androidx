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

package androidx.security.state

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_KERNEL_VERSION
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_SYSTEM_SPL
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_SYSTEM_SUPPLEMENTAL_PATCHES
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_VENDOR_SPL
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_VENDOR_SUPPLEMENTAL_PATCHES
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Provides methods to access and manage security state information for various components within a
 * system. This class handles operations related to security patch levels, vulnerability reports,
 * and update management.
 *
 * Usage examples include:
 * - Fetching the current security patch level for specific system components.
 * - Retrieving published security patch levels to compare against current levels.
 * - Listing and applying security updates from designated update providers.
 *
 * The class uses a combination of local data storage and external data fetching to maintain and
 * update security states.
 *
 * Recommended pattern of usage:
 * - call [getVulnerabilityReportUrl] and make a request to download the JSON file containing
 *   vulnerability report data
 * - create SecurityPatchState object, passing in the downloaded JSON as a [String]
 * - call [getPublishedSecurityPatchLevel] or other APIs
 *
 * @param context Application context used for accessing shared preferences, resources, and other
 *   context-dependent features.
 * @param systemModulePackageNames A list of system module package names, defaults to Google
 *   provided system modules if none are provided. The first module on the list must be the system
 *   modules metadata provider package.
 * @param customSecurityStateManagerCompat An optional custom manager for obtaining security state
 *   information. If null, a default manager is instantiated.
 * @param vulnerabilityReportJsonString A JSON string containing vulnerability data to initialize a
 *   [VulnerabilityReport] object.
 *
 *   If you only care about the Device SPL, this parameter is optional. If you need access to
 *   Published SPL and Available SPL, you must provide this JSON string, either here in the
 *   constructor, or later using [loadVulnerabilityReport].
 *
 * @constructor Creates an instance of SecurityPatchState.
 */
public open class SecurityPatchState
@JvmOverloads
constructor(
    private val context: Context,
    private val systemModulePackageNames: List<String> = DEFAULT_SYSTEM_MODULES,
    private val customSecurityStateManagerCompat: SecurityStateManagerCompat? = null,
    vulnerabilityReportJsonString: String? = null,
) {
    init {
        if (vulnerabilityReportJsonString != null) {
            loadVulnerabilityReport(vulnerabilityReportJsonString)
        }
    }

    private val securityStateManagerCompat =
        customSecurityStateManagerCompat ?: SecurityStateManagerCompat(context = context)
    private var vulnerabilityReport: VulnerabilityReport? = null

    public companion object {
        /** Default list of Android Mainline system modules. */
        @JvmField
        public val DEFAULT_SYSTEM_MODULES: List<String> =
            listOf(
                "com.google.android.modulemetadata",
                "com.google.mainline.telemetry",
                "com.google.mainline.adservices",
                "com.google.mainline.go.primary",
                "com.google.mainline.go.telemetry",
            )

        /** URL for the Google-provided data of vulnerabilities from Android Security Bulletin. */
        public const val DEFAULT_VULNERABILITY_REPORTS_URL: String =
            "https://storage.googleapis.com/osv-android-api"

        /**
         * Timeout in milliseconds to wait for an [IUpdateInfoService] implementation to bind.
         *
         * A 5-second timeout is standard for Android service binding to handle cases where the
         * target service process hangs or fails to attach, preventing this API from suspending
         * indefinitely.
         */
        public const val UPDATE_INFO_SERVICE_BINDING_TIMEOUT_MS: Long = 5000L

        /**
         * System component providing ro.build.version.security_patch property value as
         * DateBasedSpl.
         */
        public const val COMPONENT_SYSTEM: String = "SYSTEM"

        /** System modules component providing DateBasedSpl of system modules patch level. */
        public const val COMPONENT_SYSTEM_MODULES: String = "SYSTEM_MODULES"

        /** Kernel component providing kernel version as VersionedSpl. */
        public const val COMPONENT_KERNEL: String = "KERNEL"

        /**
         * Vendor component providing ro.vendor.build.security_patch property value as DateBasedSpl.
         */
        internal const val COMPONENT_VENDOR: String = "VENDOR"

        /** Disabled until Android provides sufficient guidelines for the usage of Vendor SPL. */
        internal var USE_VENDOR_SPL = false

        /**
         * Retrieves the specific security patch level for a given component based on a security
         * patch level string. This method determines the type of [SecurityPatchLevel] to construct
         * based on the component type, interpreting the string as a date for date-based components
         * or as a version number for versioned components.
         *
         * @param component The component indicating which type of component's patch level is being
         *   requested.
         * @param securityPatchLevel The string representation of the security patch level, which
         *   could be a date or a version number.
         * @return A [SecurityPatchLevel] instance corresponding to the specified component and
         *   patch level string.
         * @throws IllegalArgumentException If the input string is not in a valid format for the
         *   specified component type, or if the component requires a specific format that the
         *   string does not meet.
         */
        @JvmStatic
        public fun getComponentSecurityPatchLevel(
            @Component component: String,
            securityPatchLevel: String,
        ): SecurityPatchLevel {
            val exception = IllegalArgumentException("Unknown component: $component")
            return when (component) {
                COMPONENT_SYSTEM,
                COMPONENT_SYSTEM_MODULES,
                COMPONENT_VENDOR -> {
                    if (component == COMPONENT_VENDOR && !USE_VENDOR_SPL) {
                        throw exception
                    }
                    // These components are expected to use DateBasedSpl
                    DateBasedSecurityPatchLevel.fromString(securityPatchLevel)
                }
                COMPONENT_KERNEL -> {
                    // These components are expected to use VersionedSpl
                    VersionedSecurityPatchLevel.fromString(securityPatchLevel)
                }
                else -> throw exception
            }
        }

        /**
         * Constructs a URL for fetching vulnerability reports based on the device's Android
         * version.
         *
         * @param serverUrl The base URL of the server where vulnerability reports are stored.
         * @return A fully constructed URL pointing to the specific vulnerability report for this
         *   device.
         */
        @JvmOverloads
        @JvmStatic
        @RequiresApi(26)
        public fun getVulnerabilityReportUrl(
            serverUrl: Uri = Uri.parse(DEFAULT_VULNERABILITY_REPORTS_URL)
        ): Uri {
            val newEndpoint = "v1/android_sdk_${Build.VERSION.SDK_INT}.json"
            return serverUrl.buildUpon().appendEncodedPath(newEndpoint).build()
        }

        private const val TAG = "SecurityPatchState"
        private const val ACTION_UPDATE_INFO_SERVICE =
            "androidx.security.state.provider.UPDATE_INFO_SERVICE"
    }

    /** Annotation for defining the component to use. */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        open = true,
        value = [COMPONENT_SYSTEM, COMPONENT_SYSTEM_MODULES, COMPONENT_KERNEL, COMPONENT_VENDOR],
    )
    @SuppressLint(
        "PublicTypedef"
    ) // Exposed so that external clients (UpdateInfo) can see the valid values.
    public annotation class Component

    /** Severity of reported security issues. */
    public enum class Severity {
        /** Critical severity issues from Android Security Bulletin. */
        CRITICAL,
        /** High severity issues from Android Security Bulletin. */
        HIGH,
        /** Moderate severity issues from Android Security Bulletin. */
        MODERATE,
        /** Low severity issues from Android Security Bulletin. */
        LOW,
    }

    /** Abstract base class representing a security patch level. */
    public abstract class SecurityPatchLevel : Comparable<SecurityPatchLevel> {
        abstract override fun toString(): String
    }

    /** Implementation of [SecurityPatchLevel] for a simple string patch level. */
    public class GenericStringSecurityPatchLevel(private val patchLevel: String) :
        SecurityPatchLevel() {

        override fun toString(): String = patchLevel

        override fun compareTo(other: SecurityPatchLevel): Int {
            return when (other) {
                is GenericStringSecurityPatchLevel -> patchLevel.compareTo(other.patchLevel)
                else ->
                    throw IllegalArgumentException(
                        "Cannot compare GenericStringSpl with different type."
                    )
            }
        }
    }

    /** Implementation of [SecurityPatchLevel] for a date-based patch level. */
    public class DateBasedSecurityPatchLevel(
        private val year: Int,
        private val month: Int,
        private val day: Int,
    ) : SecurityPatchLevel() {

        public companion object {
            private val DATE_FORMATS = listOf("yyyy-MM", "yyyy-MM-dd")

            /**
             * Creates a new [DateBasedSecurityPatchLevel] from a string representation of the date.
             *
             * @param value The date string in the format of [DATE_FORMATS].
             * @return A new [DateBasedSecurityPatchLevel] representing the date.
             * @throws IllegalArgumentException if the date string is not in the correct format.
             */
            @JvmStatic
            public fun fromString(value: String): DateBasedSecurityPatchLevel {
                var date: Date? = null
                for (dateFormat in DATE_FORMATS) {
                    try {
                        date =
                            SimpleDateFormat(dateFormat, Locale.US)
                                .apply {
                                    isLenient = false // Set the date parsing to be strict
                                }
                                .parse(value)
                    } catch (e: ParseException) {
                        // Ignore and try other date format.
                    }
                }
                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val year = calendar.get(Calendar.YEAR)
                    /* Calendar.MONTH is zero-based */
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    return DateBasedSecurityPatchLevel(year, month, day)
                } else {
                    throw IllegalArgumentException(
                        "Invalid date format. Expected formats: $DATE_FORMATS"
                    )
                }
            }
        }

        @SuppressLint("DefaultLocale")
        override fun toString(): String = String.format("%d-%02d-%02d", year, month, day)

        override fun compareTo(other: SecurityPatchLevel): Int {
            if (other is DateBasedSecurityPatchLevel) {
                return when {
                    year != other.year -> year - other.year
                    month != other.month -> month - other.month
                    else -> day - other.day
                }
            } else {
                throw IllegalArgumentException("Cannot compare DateBasedSpl with different type.")
            }
        }

        /** Year of the security patch level. */
        public fun getYear(): Int = year

        /** Month of the security patch level. */
        public fun getMonth(): Int = month

        /** Day of the security patch level. */
        public fun getDay(): Int = day
    }

    /** Implementation of [SecurityPatchLevel] for a versioned patch level. */
    public class VersionedSecurityPatchLevel(
        private val majorVersion: Int,
        private val minorVersion: Int,
        private val buildVersion: Int = 0,
        private val patchVersion: Int = 0,
    ) : SecurityPatchLevel() {

        public companion object {
            /**
             * Creates a new [VersionedSecurityPatchLevel] from a string representation of the
             * version.
             *
             * @param value The version string in the format of "major.minor.build.patch".
             * @return A new [VersionedSecurityPatchLevel] representing the version.
             * @throws IllegalArgumentException if the version string is not in the correct format.
             */
            @JvmStatic
            public fun fromString(value: String): VersionedSecurityPatchLevel {
                val parts = value.split(".")
                if (parts.size < 2) {
                    throw IllegalArgumentException(
                        "Invalid version format. Expected at least major and minor versions."
                    )
                }

                val major =
                    parts[0].toIntOrNull()
                        ?: throw IllegalArgumentException("Major version is not a valid number.")
                val minor =
                    parts[1].toIntOrNull()
                        ?: throw IllegalArgumentException("Minor version is not a valid number.")
                val patch: Int
                val build: Int
                if (parts.size > 3) {
                    build = parts[2].toIntOrNull() ?: 0
                    patch = parts[3].toIntOrNull() ?: 0
                } else if (parts.size == 3) {
                    build = 0
                    patch = parts[2].toIntOrNull() ?: 0
                } else {
                    build = 0
                    patch = 0
                }

                return VersionedSecurityPatchLevel(major, minor, build, patch)
            }
        }

        @SuppressLint("DefaultLocale")
        override fun toString(): String {
            // Include the build version if it is non-zero
            return when {
                buildVersion > 0 ->
                    String.format(
                        "%d.%d.%d.%d",
                        majorVersion,
                        minorVersion,
                        buildVersion,
                        patchVersion,
                    )
                patchVersion > 0 ->
                    String.format("%d.%d.%d", majorVersion, minorVersion, patchVersion)
                else -> String.format("%d.%d", majorVersion, minorVersion)
            }
        }

        override fun compareTo(other: SecurityPatchLevel): Int {
            if (other is VersionedSecurityPatchLevel) {
                return when {
                    majorVersion != other.majorVersion -> majorVersion - other.majorVersion
                    minorVersion != other.minorVersion -> minorVersion - other.minorVersion
                    patchVersion != other.patchVersion -> patchVersion - other.patchVersion
                    else -> buildVersion - other.buildVersion
                }
            } else {
                throw IllegalArgumentException(
                    "Cannot compare VersionedSecurityPatchLevel with different type"
                )
            }
        }

        /** Major version of the security patch level. */
        public fun getMajorVersion(): Int = majorVersion

        /** Minor version of the security patch level. */
        public fun getMinorVersion(): Int = minorVersion

        /** Patch version of the security patch level. */
        public fun getPatchVersion(): Int = patchVersion

        /** Build version of the security patch level. */
        public fun getBuildVersion(): Int = buildVersion
    }

    @Serializable
    private data class VulnerabilityReport(
        /* Key is the SPL date yyyy-MM-dd */
        val vulnerabilities: Map<String, List<VulnerabilityGroup>>,

        /* Key is the SPL date yyyy-MM-dd, values are kernel versions */
        @SerialName("kernel_lts_versions") val kernelLtsVersions: Map<String, List<String>>,
    )

    @Serializable
    private data class VulnerabilityGroup(
        @SerialName("cve_identifiers") val cveIdentifiers: List<String>,
        @SerialName("asb_identifiers") val asbIdentifiers: List<String>,
        val severity: String,
        val components: List<String>,
    )

    /**
     * Retrieves a list of all system modules, defaulting to a predefined list of Google system
     * modules if no custom modules are provided.
     *
     * @return A list of strings representing system module identifiers.
     */
    internal fun getSystemModules(): List<String> {
        return systemModulePackageNames.ifEmpty { DEFAULT_SYSTEM_MODULES }
    }

    /**
     * Parses a JSON string to extract vulnerability report data. This method validates the format
     * of the input JSON and constructs a [VulnerabilityReport] object, preparing the class to
     * provide published and available security state information.
     *
     * @param jsonString The JSON string containing the vulnerability data.
     * @throws IllegalArgumentException if the JSON input is malformed or contains invalid data.
     */
    @WorkerThread
    public fun loadVulnerabilityReport(jsonString: String) {
        val result: VulnerabilityReport

        try {
            val json = Json { ignoreUnknownKeys = true }
            result = json.decodeFromString<VulnerabilityReport>(jsonString)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Malformed JSON input: ${e.message}")
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        dateFormat.isLenient = false

        result.vulnerabilities.keys.forEach { date ->
            try {
                dateFormat.parse(date)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Invalid format in date key for vulnerabilities (yyyy-MM-dd): $date"
                )
            }
        }

        result.kernelLtsVersions.forEach { kv ->
            try {
                dateFormat.parse(kv.key)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Invalid format in date key for kernel LTS versions (yyyy-MM-dd): ${kv.key}"
                )
            }

            kv.value.forEach {
                val majorVersion: Int
                try {
                    majorVersion = VersionedSecurityPatchLevel.fromString(it).getMajorVersion()
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid format in kernel LTS version: $it")
                }

                if (majorVersion < 4 || majorVersion > 20) {
                    throw IllegalArgumentException("Invalid format in kernel LTS version: $it")
                }
            }
        }

        val cvePattern = Pattern.compile("CVE-\\d{4}-\\d{4,}")
        val asbPattern = Pattern.compile("(ASB|PUB)-A-\\d{4,}")

        result.vulnerabilities.values.flatten().forEach { group ->
            group.cveIdentifiers.forEach { cve ->
                if (!cvePattern.matcher(cve).matches()) {
                    throw IllegalArgumentException(
                        "CVE identifier does not match the required format (CVE-XXXX-XXXX): $cve"
                    )
                }
            }

            group.asbIdentifiers.forEach { asb ->
                if (!asbPattern.matcher(asb).matches()) {
                    throw IllegalArgumentException(
                        "ASB identifier $asb does not match the required format: $asbPattern"
                    )
                }
            }

            try {
                Severity.valueOf(group.severity.uppercase(Locale.US))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Severity must be: critical, high, moderate, low. Found: ${group.severity}"
                )
            }
        }

        vulnerabilityReport = result
    }

    private fun getMaxComponentSecurityPatchLevel(
        @Component component: String
    ): DateBasedSecurityPatchLevel? {
        if (vulnerabilityReport == null) return null

        // Iterate through all SPL dates, find the latest date where
        // the specified component is included
        return vulnerabilityReport!!
            .vulnerabilities
            .filter { entry -> entry.value.any { group -> component in group.components } }
            .keys
            .maxByOrNull { it }
            ?.let { latestDate -> DateBasedSecurityPatchLevel.fromString(latestDate) }
    }

    private fun componentToString(@Component component: String): String {
        return component.lowercase(Locale.US)
    }

    private fun checkVulnerabilityReport() {
        if (vulnerabilityReport == null)
            throw IllegalStateException("No vulnerability report data available.")
    }

    /**
     * Returns the effective Security Patch Level (SPL) for System Modules (Mainline).
     *
     * This method determines the SPL based on the compliance of the device's installed modules
     * against the loaded Vulnerability Report.
     *
     * Behavior:
     * 1. **Outdated:** If any monitored system module is older than its required version in the
     *    Vulnerability Report, this returns the version of the *oldest* module (the limiting
     *    factor).
     * 2. **Compliant:** If all monitored modules are up-to-date with their specific requirements,
     *    this returns the **Latest SPL from the entire Vulnerability Report (Global Max)**.
     *
     * This "Global Max" behavior ensures that in months where the Bulletin does not list specific
     * Mainline updates (e.g., due to Risk Based Update System (RBUS) policies), the reported SPL
     * "upgrades" to the current Bulletin date to signal ongoing compliance.
     *
     * @throws IllegalStateException if the vulnerability report is not loaded or contains no data.
     */
    private fun getSystemModulesSecurityPatchLevel(): DateBasedSecurityPatchLevel {
        checkVulnerabilityReport()

        val modules: List<String> = getSystemModules()
        var minSpl = DateBasedSecurityPatchLevel(1970, 1, 1)
        var unpatched = false

        // Determine the target "Upgraded" SPL (Global Max) from the Bulletin
        val globalMaxSpl =
            getLatestBulletinDate()
                ?: throw IllegalStateException("No SPL data available for system modules.")

        modules.forEach { module ->
            val maxComponentSpl = getMaxComponentSecurityPatchLevel(module) ?: return@forEach
            val packageSpl: DateBasedSecurityPatchLevel
            try {
                packageSpl =
                    DateBasedSecurityPatchLevel.fromString(
                        securityStateManagerCompat.getPackageVersion(module)
                    )
            } catch (e: Exception) {
                // Prevent malformed package versions from interrupting the loop.
                return@forEach
            }

            // Check if the specific module is outdated relative to its last KNOWN update
            if (packageSpl < maxComponentSpl) {
                if (unpatched) {
                    if (minSpl > packageSpl) minSpl = packageSpl
                } else {
                    minSpl = packageSpl
                    unpatched = true
                }
            }
        }

        // If any module is outdated, return the lowest version found (Device State).
        if (unpatched) {
            return minSpl
        }

        // If all modules meet their requirements, "Upgrade" the reported SPL to the
        // Global Max (Bulletin Date). This handles Risk Based Update System (RBUS) months
        // (hidden patches) and empty bulletins correctly.
        return globalMaxSpl
    }

    /**
     * Retrieves the latest security patch level date found in the entire vulnerability report.
     *
     * This date represents the "Global Maximum" SPL for the bulletin, effectively acting as the
     * compliance baseline for the device. It is determined by finding the maximum date key in the
     * vulnerabilities map, regardless of which specific components (System, Vendor, Kernel, or
     * System Modules) are included in that date's entry.
     *
     * This value is used to determine the Published SPL for components that track the overall
     * Android Security Bulletin cadence, ensuring that they reflect the latest compliance date even
     * if the bulletin did not include specific patches for that component (e.g., due to Risk Based
     * Update System (RBUS) policies).
     *
     * @return The latest [DateBasedSecurityPatchLevel] found in the report, or null if the report
     *   is not loaded or contains no dates.
     */
    private fun getLatestBulletinDate(): DateBasedSecurityPatchLevel? {
        if (vulnerabilityReport == null) return null
        return vulnerabilityReport!!
            .vulnerabilities
            .keys
            .maxByOrNull { it }
            ?.let { DateBasedSecurityPatchLevel.fromString(it) }
    }

    /**
     * Retrieves the current security patch level for a specified component.
     *
     * @param component The component for which the security patch level is requested.
     * @return A [SecurityPatchLevel] representing the current patch level of the component.
     * @throws IllegalStateException if the patch level data is not available.
     * @throws IllegalArgumentException if the component name is unrecognized.
     */
    public open fun getDeviceSecurityPatchLevel(@Component component: String): SecurityPatchLevel {
        val globalSecurityState =
            securityStateManagerCompat.getGlobalSecurityState(getSystemModules()[0])

        return when (component) {
            COMPONENT_SYSTEM_MODULES -> {
                getSystemModulesSecurityPatchLevel()
            }
            COMPONENT_KERNEL -> {
                val kernelVersion =
                    globalSecurityState.getString(KEY_KERNEL_VERSION)
                        ?: throw IllegalStateException("Kernel version not available.")

                VersionedSecurityPatchLevel.fromString(kernelVersion)
            }
            COMPONENT_SYSTEM -> {
                val systemSpl =
                    globalSecurityState.getString(KEY_SYSTEM_SPL)
                        ?: throw IllegalStateException("System SPL not available.")

                DateBasedSecurityPatchLevel.fromString(systemSpl)
            }
            COMPONENT_VENDOR -> {
                val vendorSpl =
                    globalSecurityState.getString(KEY_VENDOR_SPL)
                        ?: throw IllegalStateException("Vendor SPL not available.")

                DateBasedSecurityPatchLevel.fromString(vendorSpl)
            }
            else -> throw IllegalArgumentException("Unknown component: $component")
        }
    }

    /**
     * Retrieves the published security patch level for a specified component. This patch level is
     * based on the most recent vulnerability reports, which is machine-readable data from Android
     * and other security bulletins.
     *
     * For **System** and **System Modules (Mainline)**, this method employs a "Global Max"
     * strategy: it returns the latest date found in the entire Vulnerability Report, regardless of
     * whether that specific date included updates for the requested component. This ensures that
     * the Published SPL aligns with the overall Android Security Bulletin date (e.g. 2026-01-05),
     * preventing stale reporting during months where the Bulletin may only list patches for other
     * components (e.g. Vendor-only updates) or is advisory-only under Risk Based Update System
     * (RBUS) policies.
     *
     * For **Kernel** and **Vendor** components, it returns the latest patch level specifically
     * associated with those components in the report.
     *
     * @param component The component for which the published patch level is requested.
     * @return A list of [SecurityPatchLevel] representing the published patch levels. The list
     *   contains a single element for most components. For KERNEL, it lists kernel LTS version
     *   numbers for all supported major kernel versions. For example: ``` [ "4.19.314", "5.15.159",
     *   "6.1.91" ] ```
     * @throws IllegalStateException if the vulnerability report is not loaded or if patch level
     *   data is unavailable.
     * @throws IllegalArgumentException if the component name is unrecognized.
     */
    public open fun getPublishedSecurityPatchLevel(
        @Component component: String
    ): List<SecurityPatchLevel> {
        checkVulnerabilityReport()

        val splDataMissingException = IllegalStateException("SPL data not available: $component")

        return when (component) {
            // Both System and System Modules use the "Global Max" strategy to support Risk Based
            // Update System (RBUS) policies. This ensures they return the latest Bulletin date even
            // if the Bulletin only lists Vendor patches or is an empty Advisory-only update.
            COMPONENT_SYSTEM,
            COMPONENT_SYSTEM_MODULES -> {
                listOf(getLatestBulletinDate() ?: throw splDataMissingException)
            }
            COMPONENT_VENDOR -> {
                // Vendor logic is guarded by an internal flag due to varying ecosystem policies.
                if (!USE_VENDOR_SPL) {
                    throw splDataMissingException
                }
                listOf(
                    getMaxComponentSecurityPatchLevel(componentToString(component))
                        ?: throw splDataMissingException
                )
            }
            COMPONENT_KERNEL -> getPublishedKernelVersions()
            else -> throw IllegalArgumentException("Unknown component: $component")
        }
    }

    /**
     * Fetches the latest available security patch level for a specific component.
     *
     * This is a convenience method that determines the effective security state by aggregating
     * results from all trusted providers and comparing them against the device's current state.
     *
     * **Performance:** This method performs IPC (Inter-Process Communication) to query trusted
     * services. While the providers themselves may return cached data without triggering a network
     * call, the service binding process is asynchronous and significantly heavier than local memory
     * lookups.
     *
     * **Aggregation Logic:** If multiple providers report updates for the same component (e.g.,
     * both an OEM updater and GOTA report a "SYSTEM" update), this method conservatively selects
     * the **newest** (highest version/date) patch level among them.
     *
     * **Note:** This value is based on the server-side state known to the update clients. It may
     * not represent a real-time check if the update client has restricted background syncs (e.g.,
     * due to rate limiting or battery saver).
     *
     * @param component The component to check (e.g., [COMPONENT_SYSTEM],
     *   [COMPONENT_SYSTEM_MODULES]).
     * @param timeoutMillis The maximum time to wait for the query to complete, in milliseconds.
     *   Defaults to [UPDATE_INFO_SERVICE_BINDING_TIMEOUT_MS].
     * @return The latest [SecurityPatchLevel] found. If no updates are available, or if the
     *   available updates are older than or equal to the current device state, this returns the
     *   current Device SPL.
     */
    public suspend fun fetchAvailableSecurityPatchLevel(
        @Component component: String,
        timeoutMillis: Long = UPDATE_INFO_SERVICE_BINDING_TIMEOUT_MS,
    ): SecurityPatchLevel {
        val deviceSpl = getDeviceSecurityPatchLevel(component)
        val results = queryAllAvailableUpdates(timeoutMillis)

        val maxAvailableSpl =
            results
                .asSequence()
                .flatMap { it.updates }
                .filter { update -> update.component == component }
                .mapNotNull { update ->
                    try {
                        getComponentSecurityPatchLevel(component, update.securityPatchLevel)
                    } catch (e: IllegalArgumentException) {
                        Log.w(
                            TAG,
                            "Ignoring invalid SPL format from provider: ${update.securityPatchLevel}",
                        )
                        null
                    }
                }
                .maxOrNull()

        if (maxAvailableSpl != null && maxAvailableSpl > deviceSpl) {
            return maxAvailableSpl
        }

        return deviceSpl
    }

    /**
     * Queries for available security updates from all trusted update providers.
     *
     * This method performs a comprehensive check by:
     * 1. **Discovering** all trusted services on the device that implement the `UpdateInfoService`
     *    protocol (e.g., c, Google Play Store).
     * 2. **Querying** each service concurrently to retrieve its status.
     * 3. **Collecting** the results into a list.
     *
     * **Freshness & Caching:** The freshness of the returned data depends on the internal policies
     * of the individual update providers. Providers are expected to maintain a reasonably fresh
     * cache (typically refreshing at least once per hour). If a provider determines its cache is
     * stale, this call may block while it performs a network fetch.
     *
     * @param timeoutMillis The maximum time to wait for each provider to respond, in milliseconds.
     *   Defaults to [UPDATE_INFO_SERVICE_BINDING_TIMEOUT_MS].
     * @return A list of [UpdateCheckResult] objects. Each element represents the status reported by
     *   a distinct update provider, containing its list of updates and the timestamp of its last
     *   successful synchronization.
     */
    public suspend fun queryAllAvailableUpdates(
        timeoutMillis: Long = UPDATE_INFO_SERVICE_BINDING_TIMEOUT_MS
    ): List<UpdateCheckResult> =
        withContext(Dispatchers.IO) {
            val trustedServices = getTrustedUpdateInfoServices()

            if (trustedServices.isEmpty()) {
                Log.i(TAG, "No trusted update providers found.")
                return@withContext emptyList()
            }

            // Bind to all providers concurrently to minimize total latency
            val deferredResults =
                trustedServices.map { serviceComponent ->
                    async { fetchFromUpdateInfoService(serviceComponent, timeoutMillis) }
                }

            return@withContext deferredResults.awaitAll()
        }

    /**
     * Binds to a specific [IUpdateInfoService] implementation, retrieves its status, and unbinds.
     *
     * This method handles the asynchronous lifecycle of the Android [ServiceConnection], wrapping
     * the callback-based [Context.bindService] API into a suspending function.
     *
     * To ensure safety and responsiveness:
     * 1. It enforces a strict timeout (specified by [timeoutMillis]) to prevent indefinite
     *    suspension.
     * 2. It executes the blocking IPC call on a background thread to avoid ANRs on the main thread.
     *
     * **Telemetry & Identity:** The `Intent` used to bind includes the client's package name as a
     * data URI (`package:com.example.client`). This serves two purposes:
     * 1. **Identity:** Allows the service to identify the caller in its `onClientConnected` and
     *    `onClientDisconnected` lifecycle hooks (e.g., for tracking session duration).
     * 2. **Session Tracking:** Forces the Android system to treat each client's connection as a
     *    unique binding. By default, Android caches the Binder for identical Intents and suppresses
     *    subsequent `onBind` calls. Adding unique data ensures the service receives a lifecycle
     *    event for *every* client session.
     *
     * @param component The [ComponentName] of the [IUpdateInfoService] to bind to.
     * @param timeoutMillis The maximum time to wait for the service to respond.
     * @return An [UpdateCheckResult] containing the data from the provider. If the operation fails,
     *   returns an empty result with the provider's package name.
     */
    private suspend fun fetchFromUpdateInfoService(
        component: ComponentName,
        timeoutMillis: Long,
    ): UpdateCheckResult {
        // Default result to return in case of any failure (graceful degradation)
        val emptyResult =
            UpdateCheckResult(
                providerPackageName = component.packageName,
                updates = emptyList(),
                lastCheckTimeMillis = 0L,
            )

        return try {
            // Safety: Apply a strict timeout to prevent indefinite hanging if the
            // target service process is broken or fails to respond.
            withTimeout(timeoutMillis) {
                suspendCancellableCoroutine { continuation ->
                    val intent =
                        Intent(ACTION_UPDATE_INFO_SERVICE).apply {
                            this.component = component
                            // Attach unique data to identify the caller and force a fresh bind for
                            // each client
                            data = Uri.fromParts("package", context.packageName, null)
                        }

                    val connection =
                        object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                                // Critical: onServiceConnected runs on the Main (UI) Thread.
                                // The AIDL call `listAvailableUpdates` is blocking (not oneway) and
                                // may
                                // perform network operations. We must offload this to a background
                                // thread
                                // to avoid freezing the app (ANR).
                                Thread {
                                        try {
                                            val binder =
                                                IUpdateInfoService.Stub.asInterface(service)
                                            val result = binder.listAvailableUpdates()

                                            if (continuation.isActive) continuation.resume(result)
                                        } catch (e: RemoteException) {
                                            // Log warning for "swallowed" exceptions to help
                                            // debugging
                                            Log.w(
                                                TAG,
                                                "Error calling listAvailableUpdates on ${name.packageName}",
                                                e,
                                            )
                                            if (continuation.isActive)
                                                continuation.resume(emptyResult)
                                        } catch (e: Exception) {
                                            // Catch generic exceptions from the background thread
                                            // wrapper
                                            Log.w(
                                                TAG,
                                                "Error in background IPC for ${name.packageName}",
                                                e,
                                            )
                                            if (continuation.isActive)
                                                continuation.resume(emptyResult)
                                        } finally {
                                            // Cleanup: Always unbind after the IPC is done.
                                            // This must happen here (in the background) to ensure
                                            // we don't
                                            // unbind while the binder is still in use.
                                            try {
                                                context.unbindService(this)
                                            } catch (e: Exception) {
                                                // Ignore unbind errors (e.g., service already died)
                                            }
                                        }
                                    }
                                    .start()
                            }

                            override fun onServiceDisconnected(name: ComponentName) {
                                // Handle unexpected disconnection (crash of the remote service)
                                Log.w(TAG, "Service disconnected unexpectedly: ${name.packageName}")
                                if (continuation.isActive) continuation.resume(emptyResult)

                                // Ensure we clean up our side of the connection to prevent leaks
                                try {
                                    context.unbindService(this)
                                } catch (e: Exception) {}
                            }
                        }

                    try {
                        val bound =
                            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                        if (!bound) {
                            Log.w(TAG, "Failed to bind to service: ${component.packageName}")
                            continuation.resume(emptyResult)
                        }
                    } catch (e: SecurityException) {
                        Log.w(
                            TAG,
                            "Security exception binding to service: ${component.packageName}",
                            e,
                        )
                        continuation.resume(emptyResult)
                    }

                    // Ensure we unbind if the coroutine is cancelled by the caller
                    continuation.invokeOnCancellation {
                        try {
                            context.unbindService(connection)
                        } catch (e: Exception) {}
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Handle the timeout gracefully by logging and returning empty
            Log.w(TAG, "Timed out waiting for service: ${component.packageName}")
            emptyResult
        }
    }

    /**
     * Discovers trusted system services that implement the UpdateInfoService protocol.
     *
     * This method queries the [PackageManager] for services that handle the
     * [ACTION_UPDATE_INFO_SERVICE] intent, using the `PackageManager.MATCH_SYSTEM_ONLY` flag. This
     * flag ensures that only components from applications installed on the system image are
     * returned, which includes both original and updated system apps.
     *
     * @return A list of [ComponentName]s for all trusted update services found on the device.
     */
    @VisibleForTesting
    internal fun getTrustedUpdateInfoServices(): List<ComponentName> {
        val intent = Intent(ACTION_UPDATE_INFO_SERVICE)

        val resolveInfos =
            context.packageManager.queryIntentServices(intent, PackageManager.MATCH_SYSTEM_ONLY)

        return resolveInfos.mapNotNull { resolveInfo ->
            val serviceInfo = resolveInfo.serviceInfo
            if (serviceInfo?.applicationInfo == null) {
                null
            } else {
                val isSystem =
                    (serviceInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem) {
                    ComponentName(serviceInfo.packageName, serviceInfo.name)
                } else {
                    Log.w(
                        TAG,
                        "Ignoring non-system provider found with MATCH_SYSTEM_ONLY: ${serviceInfo.packageName}",
                    )
                    null
                }
            }
        }
    }

    /**
     * Retrieves a list of the latest kernel LTS versions from the vulnerability report.
     *
     * @return A list of [VersionedSecurityPatchLevel] representing kernel LTS versions, or an empty
     *   list if no data is available.
     */
    private fun getPublishedKernelVersions(): List<VersionedSecurityPatchLevel> {
        vulnerabilityReport?.let { (_, kernelLtsVersions) ->
            if (kernelLtsVersions.isEmpty()) {
                return emptyList()
            }
            // A map from a kernel LTS version (major.minor) to its latest published version.
            // For example, version 5.4 would map to 5.4.123 if that's the latest published version.
            val kernelVersionToLatest = mutableMapOf<String, VersionedSecurityPatchLevel>()
            // Reduce all the published kernel LTS versions from each SPL into one list.
            val publishedKernelLtsVersions =
                kernelLtsVersions.values
                    .reduce { versions, version -> versions + version }
                    .map { VersionedSecurityPatchLevel.fromString(it) }

            // Update the map so that each kernel LTS version maps to its latest (largest) published
            // version.
            publishedKernelLtsVersions.forEach { version ->
                val kernelVersion = "${version.getMajorVersion()}.${version.getMinorVersion()}"

                kernelVersionToLatest[kernelVersion]?.let {
                    if (version > it) {
                        kernelVersionToLatest[kernelVersion] = version
                    }
                } ?: run { kernelVersionToLatest[kernelVersion] = version }
            }
            return kernelVersionToLatest.values.toList()
        }
        return emptyList()
    }

    /**
     * Lists all security fixes applied on the current device since the baseline Android release of
     * the current system image, filtered for a specified component and patch level, categorized by
     * severity.
     *
     * @param component The component for which security fixes are listed.
     * @param spl The security patch level for which fixes are retrieved.
     * @return A map categorizing CVE identifiers by their severity for the specified patch level.
     *   For example: ``` { Severity.CRITICAL: ["CVE-2023-1234", "CVE-2023-5678"], Severity.HIGH:
     *   ["CVE-2023-9012"], Severity.MODERATE: ["CVE-2023-3456"] } ```
     * @throws IllegalArgumentException if the specified component is not valid for fetching
     *   security fixes.
     * @throws IllegalStateException if the vulnerability report is not loaded.
     */
    public open fun getPatchedCves(
        @Component component: String,
        spl: SecurityPatchLevel,
    ): Map<Severity, Set<String>> {
        // Check if the component is valid for this operation
        val validComponents =
            listOfNotNull(
                COMPONENT_SYSTEM,
                if (USE_VENDOR_SPL) COMPONENT_VENDOR else null,
                COMPONENT_SYSTEM_MODULES,
            )
        if (component !in validComponents) {
            throw IllegalArgumentException(
                "Component must be one of $validComponents but was $component"
            )
        }
        checkVulnerabilityReport()

        vulnerabilityReport!!.let { report ->
            val relevantFixes = mutableMapOf<Severity, MutableList<String>>()

            // Iterate through all vulnerabilities and filter based on component and patch level
            report.vulnerabilities.forEach { (patchLevel, groups) ->
                if (spl.toString() >= patchLevel) {
                    groups
                        .filter { group ->
                            when (component) {
                                COMPONENT_SYSTEM_MODULES ->
                                    group.components.any { it in getSystemModules() }
                                else -> group.components.contains(componentToString(component))
                            }
                        }
                        .forEach { group ->
                            val severity = Severity.valueOf(group.severity.uppercase(Locale.US))
                            relevantFixes
                                .getOrPut(severity, ::mutableListOf)
                                .addAll(group.cveIdentifiers)
                        }
                }
            }
            return relevantFixes.mapValues { it.value.toSet() }.toMap()
        }
    }

    /**
     * Checks if all components of the device have their security patch levels up to date with the
     * published security patch levels. This method compares the device's current security patch
     * level against the latest published levels for each component.
     *
     * @return true if all components are fully updated, false otherwise.
     * @throws IllegalArgumentException if device or published security patch level for a component
     *   cannot be accessed.
     */
    public fun isDeviceFullyUpdated(): Boolean {
        checkVulnerabilityReport()

        val components =
            listOf(COMPONENT_SYSTEM, COMPONENT_SYSTEM_MODULES, COMPONENT_VENDOR, COMPONENT_KERNEL)

        components.forEach { component ->
            if (component == COMPONENT_VENDOR && !USE_VENDOR_SPL) return@forEach
            val deviceSpl =
                try {
                    getDeviceSecurityPatchLevel(component)
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Failed to retrieve device SPL for component: $component",
                        e,
                    )
                }

            try {
                if (component != COMPONENT_KERNEL) {
                    val publishedSpl = getPublishedSecurityPatchLevel(component)[0]

                    if (deviceSpl < publishedSpl) {
                        return false
                    }
                } else {
                    val publishedVersions = getPublishedKernelVersions()
                    val kernelVersion = deviceSpl as VersionedSecurityPatchLevel

                    if (
                        publishedVersions
                            .filter { it.getMajorVersion() == kernelVersion.getMajorVersion() }
                            .any { it > kernelVersion }
                    ) {
                        return false
                    }
                }
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Published SPL not available for component: $component",
                    e,
                )
            }
        }
        return true
    }

    /**
     * Retrieves a list of additional CVEs that have been patched by the OEM supplemental to the
     * declared Security Patch Level (SPL).
     *
     * @return A List of CVE identifier strings (e.g., "CVE-2023-12345"). Returns an empty list if
     *   no supplemental patches are declared or found.
     */
    private fun getSupplementalPatchedCves(): List<String> {
        val globalSecurityState =
            securityStateManagerCompat.getGlobalSecurityState(getSystemModules()[0])

        val systemSupplementalCves =
            globalSecurityState.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES) ?: emptyArray()
        val vendorSupplementalCves =
            globalSecurityState.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES) ?: emptyArray()

        return (systemSupplementalCves + vendorSupplementalCves).toList()
    }

    /**
     * Verifies if all specified CVEs have been patched in the system. This method aggregates the
     * CVEs patched across specified system components and checks if the list includes all CVEs
     * provided.
     *
     * @param cveList A list of CVE identifiers as strings in the form "CVE-YYYY-NNNNN", where YYYY
     *   denotes year, and NNNNN is a number with 3 to 5 digits.
     * @return true if all provided CVEs are patched, false otherwise.
     */
    public fun areCvesPatched(cveList: List<String>): Boolean {
        val componentsToCheck =
            listOfNotNull(
                COMPONENT_SYSTEM,
                if (USE_VENDOR_SPL) COMPONENT_VENDOR else null,
                COMPONENT_SYSTEM_MODULES,
            )
        val allPatchedCves = mutableSetOf<String>()

        // Aggregate all CVEs from security fixes across necessary components
        for (component in componentsToCheck) {
            val spl = getDeviceSecurityPatchLevel(component)
            val fixes = getPatchedCves(component, spl)
            allPatchedCves.addAll(fixes.values.flatten())
        }

        // Add supplemental CVEs
        allPatchedCves.addAll(getSupplementalPatchedCves())

        // Check if all provided CVEs are in the patched CVEs list
        return cveList.all { allPatchedCves.contains(it) }
    }
}
