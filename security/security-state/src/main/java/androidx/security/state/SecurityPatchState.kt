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
import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.security.state.SecurityStateManager.Companion.KEY_KERNEL_VERSION
import androidx.security.state.SecurityStateManager.Companion.KEY_SYSTEM_SPL
import androidx.security.state.SecurityStateManager.Companion.KEY_VENDOR_SPL
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

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
 * @param context Android context used for accessing shared preferences, resources, and other
 *   context-dependent features.
 * @param systemModules A list of system module package names, defaults to Google provided system
 *   modules if none are provided. The first module on the list must be the system modules metadata
 *   provider package.
 * @param customSecurityStateManager An optional custom manager for obtaining security state
 *   information. If null, a default manager is instantiated.
 * @constructor Creates an instance of SecurityPatchState.
 */
public open class SecurityPatchState(
    private val context: Context,
    private val systemModules: List<String> = listOf(),
    private val customSecurityStateManager: SecurityStateManager? = null
) {
    private val securityStateManager =
        customSecurityStateManager ?: SecurityStateManager(context = context)
    private var vulnerabilityReport: VulnerabilityReport? = null

    public companion object {
        @JvmField
        public val DEFAULT_SYSTEM_MODULES: List<String> =
            listOf(
                "com.google.android.modulemetadata",
                "com.google.mainline.telemetry",
                "com.google.mainline.adservices",
                "com.google.mainline.go.primary",
                "com.google.mainline.go.telemetry"
            )

        public const val DEFAULT_VULNERABILITY_REPORTS_URL: String =
            "https://storage.googleapis.com/osv-android-api"
    }

    /** Types of components whose security state can be accessed. */
    public enum class Component {
        // Provides DateBasedSpl
        SYSTEM,

        // Provides DateBasedSpl
        SYSTEM_MODULES,

        // Provides DateBasedSpl
        VENDOR,

        // Provides VersionedSpl
        KERNEL,

        // Provides VersionedSpl
        WEBVIEW
    }

    /** Severity of reported security issues. */
    public enum class Severity {
        CRITICAL,
        HIGH,
        MODERATE,
        LOW
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
        private val day: Int
    ) : SecurityPatchLevel() {

        public companion object {
            private const val DATE_FORMAT = "yyyy-MM-dd"
            private val dateFormatter =
                SimpleDateFormat(DATE_FORMAT, Locale.US).apply {
                    isLenient = false // Set the date parsing to be strict
                }

            @JvmStatic
            public fun fromString(value: String): DateBasedSecurityPatchLevel {
                val calendar = Calendar.getInstance()
                try {
                    calendar.time =
                        dateFormatter.parse(value)
                            ?: throw IllegalArgumentException(
                                "Invalid date format. Expected format: yyyy-MM-dd"
                            )
                    val year = calendar.get(Calendar.YEAR)
                    /* Calendar.MONTH is zero-based */
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    return DateBasedSecurityPatchLevel(year, month, day)
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Invalid date format. Expected format: yyyy-MM-dd",
                        e
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

        public fun getYear(): Int = year

        public fun getMonth(): Int = month

        public fun getDay(): Int = day
    }

    /** Implementation of [SecurityPatchLevel] for a versioned patch level. */
    public class VersionedSecurityPatchLevel(
        private val majorVersion: Int,
        private val minorVersion: Int,
        private val buildVersion: Int = 0,
        private val patchVersion: Int = 0
    ) : SecurityPatchLevel() {

        public companion object {
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
                        patchVersion
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

        public fun getMajorVersion(): Int = majorVersion

        public fun getMinorVersion(): Int = minorVersion

        public fun getPatchVersion(): Int = patchVersion

        public fun getBuildVersion(): Int = buildVersion
    }

    private data class VulnerabilityReport(
        /* Key is the SPL date yyyy-MM-dd */
        @SerializedName("vulnerabilities")
        val vulnerabilities: Map<String, List<VulnerabilityGroup>>,

        /* Key is the SPL date yyyy-MM-dd, values are kernel versions */
        @SerializedName("kernel_lts_versions") val kernelLtsVersions: Map<String, List<String>>
    )

    private data class VulnerabilityGroup(
        @SerializedName("cve_identifiers") val cveIdentifiers: List<String>,
        @SerializedName("asb_identifiers") val asbIdentifiers: List<String>,
        @SerializedName("severity") val severity: String,
        @SerializedName("components") val components: List<String>
    )

    /**
     * Retrieves a list of all system modules, defaulting to a predefined list of Google system
     * modules if no custom modules are provided.
     *
     * @return A list of strings representing system module identifiers.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun getSystemModules(): List<String> {
        // Use the provided systemModules if not empty; otherwise, use defaultSystemModules
        return systemModules.ifEmpty { DEFAULT_SYSTEM_MODULES }
    }

    private fun buildAllowedComponents(): List<String> {
        // Adding fixed components to the list of system modules
        val fixedComponents = listOf("system", "kernel", "vendor", "webview")

        return getSystemModules() + fixedComponents
    }

    /**
     * Parses a JSON string to extract vulnerability report data. This method validates the format
     * of the input JSON and constructs a [VulnerabilityReport] object, preparing the class to
     * provide published and available security state information.
     *
     * The recommended pattern of usage:
     * - create SecurityPatchState object
     * - call getVulnerabilityReportUrl()
     * - download JSON file containing vulnerability report data
     * - call parseVulnerabilityReport()
     * - call getPublishedSecurityPatchLevel() or other APIs
     *
     * @param jsonString The JSON string containing the vulnerability data.
     * @throws IllegalArgumentException if the JSON input is malformed or contains invalid data.
     */
    public fun loadVulnerabilityReport(jsonString: String) {
        val result: VulnerabilityReport
        val allowedComponents = buildAllowedComponents()

        try {
            result = Gson().fromJson(jsonString, VulnerabilityReport::class.java)
        } catch (e: JsonSyntaxException) {
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
        val asbPattern = Pattern.compile("ASB-\\d{4,}")

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
                        "ASB identifier does not match the required format (ASB-XXXX): $asb"
                    )
                }
            }

            try {
                Severity.valueOf(group.severity.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Severity must be: critical, high, moderate, low. Found: ${group.severity}"
                )
            }

            group.components.forEach { component ->
                if (!allowedComponents.contains(component)) {
                    throw IllegalArgumentException("Invalid component. Found: $component")
                }
            }
        }

        vulnerabilityReport = result
    }

    /**
     * Constructs a URL for fetching vulnerability reports based on the device's Android version.
     *
     * @param serverUrl The base URL of the server where vulnerability reports are stored.
     * @return A fully constructed URL pointing to the specific vulnerability report for this
     *   device.
     * @throws IllegalArgumentException if the Android SDK version is unsupported.
     */
    public fun getVulnerabilityReportUrl(serverUrl: String): Uri {
        val androidSdk = securityStateManager.getAndroidSdkInt()
        if (androidSdk < 26) {
            throw IllegalArgumentException(
                "Unsupported SDK version (must be > 25), found $androidSdk."
            )
        }

        val completeUrl = "$serverUrl/v1/android_sdk_$androidSdk.json"

        return Uri.parse(completeUrl)
    }

    private fun getMaxComponentSecurityPatchLevel(component: String): DateBasedSecurityPatchLevel? {
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

    private fun componentToString(component: Component): String {
        return component.name.lowercase(Locale.getDefault())
    }

    private fun checkVulnerabilityReport() {
        if (vulnerabilityReport == null)
            throw IllegalStateException("No vulnerability report data available.")
    }

    /**
     * Returns min SPL of the unpatched system modules, or max SPL of the system modules if all of
     * them are fully patched.
     */
    private fun getSystemModulesSecurityPatchLevel(): DateBasedSecurityPatchLevel {
        checkVulnerabilityReport()

        val modules: List<String> = getSystemModules()
        var minSpl = DateBasedSecurityPatchLevel(1970, 1, 1)
        var maxSpl = DateBasedSecurityPatchLevel(1970, 1, 1)
        var unpatched = false
        modules.forEach { module ->
            val maxComponentSpl = getMaxComponentSecurityPatchLevel(module) ?: return@forEach
            val packageSpl: DateBasedSecurityPatchLevel
            try {
                packageSpl =
                    DateBasedSecurityPatchLevel.fromString(
                        securityStateManager.getPackageVersion(module)
                    )
            } catch (e: Exception) {
                // Prevent malformed package versions from interrupting the loop.
                return@forEach
            }

            if (packageSpl < maxComponentSpl) {
                if (unpatched) {
                    if (minSpl > packageSpl) minSpl = packageSpl
                } else {
                    minSpl = packageSpl
                    unpatched = true
                }
            }
            if (maxComponentSpl > maxSpl) {
                maxSpl = maxComponentSpl
            }
        }

        if (unpatched) {
            return minSpl
        }
        if (maxSpl.getYear() == 1970) {
            throw IllegalStateException("No SPL data available for system modules.")
        }
        return maxSpl
    }

    private fun getSystemModulesPublishedSecurityPatchLevel(): DateBasedSecurityPatchLevel {
        checkVulnerabilityReport()

        val modules: List<String> = getSystemModules()
        var maxSpl = DateBasedSecurityPatchLevel(1970, 1, 1)
        modules.forEach { module ->
            val maxComponentSpl = getMaxComponentSecurityPatchLevel(module) ?: return@forEach

            if (maxComponentSpl > maxSpl) {
                maxSpl = maxComponentSpl
            }
        }
        return maxSpl
    }

    /**
     * Retrieves the current security patch level for a specified component.
     *
     * @param component The component for which the security patch level is requested.
     * @return A [SecurityPatchLevel] representing the current patch level of the component.
     * @throws IllegalStateException if the patch level data is not available.
     */
    public open fun getDeviceSecurityPatchLevel(component: Component): SecurityPatchLevel {
        val globalSecurityState = securityStateManager.getGlobalSecurityState(getSystemModules()[0])

        return when (component) {
            Component.SYSTEM_MODULES -> {
                getSystemModulesSecurityPatchLevel()
            }
            Component.KERNEL -> {
                val kernelVersion =
                    globalSecurityState.getString(KEY_KERNEL_VERSION)
                        ?: throw IllegalStateException("Kernel version not available.")

                VersionedSecurityPatchLevel.fromString(kernelVersion)
            }
            Component.SYSTEM -> {
                val systemSpl =
                    globalSecurityState.getString(KEY_SYSTEM_SPL)
                        ?: throw IllegalStateException("System SPL not available.")

                DateBasedSecurityPatchLevel.fromString(systemSpl)
            }
            Component.VENDOR -> {
                val vendorSpl =
                    globalSecurityState.getString(KEY_VENDOR_SPL)
                        ?: throw IllegalStateException("Vendor SPL not available.")

                DateBasedSecurityPatchLevel.fromString(vendorSpl)
            }

            // TODO(musashi): Add support for webview package
            Component.WEBVIEW -> TODO()
        }
    }

    /**
     * Retrieves the published security patch level for a specified component. This patch level is
     * based on the most recent vulnerability reports, which is a machine-readable data from Android
     * and other security bulletins.
     *
     * The published security patch level is the most recent value published in a bulletin.
     *
     * @param component The component for which the published patch level is requested.
     * @return A list of [SecurityPatchLevel] representing the published patch levels. The list
     *   contains single element for all components, except for KERNEL, where it lists kernel LTS
     *   version numbers for all supported major kernel versions. For example: ``` [ "4.19.314",
     *   "5.15.159", "6.1.91" ] ```
     * @throws IllegalStateException if the vulnerability report is not loaded or if patch level
     *   data is unavailable.
     */
    public open fun getPublishedSecurityPatchLevel(component: Component): List<SecurityPatchLevel> {
        checkVulnerabilityReport()

        return when (component) {
            Component.SYSTEM_MODULES -> listOf(getSystemModulesPublishedSecurityPatchLevel())
            Component.SYSTEM,
            Component.VENDOR -> {
                listOf(
                    getMaxComponentSecurityPatchLevel(componentToString(component))
                        ?: throw IllegalStateException("SPL data not available.")
                )
            }
            Component.KERNEL -> getPublishedKernelVersions()

            // TODO(musashi): Add support for webview package
            Component.WEBVIEW -> TODO()
        }
    }

    /**
     * Retrieves a list of kernel LTS versions from the latest SPL entry in the vulnerability
     * report.
     *
     * @return A list of strings representing kernel LTS versions, or an empty list if no data is
     *   available.
     */
    private fun getPublishedKernelVersions(): List<VersionedSecurityPatchLevel> {
        val format = SimpleDateFormat("yyyy-MM-dd")
        format.isLenient = false

        vulnerabilityReport?.let { report ->
            // Find the latest SPL date key in the kernel LTS versions map.
            val latestSplDate =
                report.kernelLtsVersions.keys.maxWithOrNull(
                    compareBy {
                        format.parse(it) ?: throw IllegalArgumentException("Invalid date format.")
                    }
                )
            // Return the list of LTS versions for the latest SPL date.
            return report.kernelLtsVersions[latestSplDate]?.map {
                VersionedSecurityPatchLevel.fromString(it)
            } ?: emptyList()
        }
        return emptyList()
    }

    /**
     * Retrieves the available security patch level for a specified component for current device.
     * Available patch level comes from updates that were not downloaded or installed yet, but have
     * been released by device manufacturer to the current device. If no updates are found, it
     * returns the current device security patch level.
     *
     * The information about updates is supplied by update clients through dedicated update
     * information content providers. If no providers are specified, it defaults to predefined URIs,
     * consisting of Google OTA and Play system components update clients.
     *
     * @param component The component for which the available patch level is requested.
     * @param providers Optional list of URIs representing update providers; if null, defaults are
     *   used. Check documentation of OTA update clients for content provider URIs.
     * @return A [SecurityPatchLevel] representing the available patch level for updates.
     */
    @JvmOverloads
    public open fun getAvailableSecurityPatchLevel(
        component: Component,
        providers: List<Uri>? = null
    ): SecurityPatchLevel {
        val updates = listAvailableUpdates(providers)
        return updates
            .filter { it.component == component.toString() }
            .maxOfOrNull {
                getSecurityPatchLevelString(Component.valueOf(it.component), it.securityPatchLevel)
            } ?: getDeviceSecurityPatchLevel(component)
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
        component: Component,
        spl: SecurityPatchLevel
    ): Map<Severity, List<String>> {
        // Check if the component is valid for this operation
        if (component !in listOf(Component.SYSTEM, Component.VENDOR, Component.SYSTEM_MODULES)) {
            throw IllegalArgumentException("Component must be SYSTEM, VENDOR, or SYSTEM_MODULES")
        }
        checkVulnerabilityReport()

        vulnerabilityReport!!.let { report ->
            val relevantFixes = mutableMapOf<Severity, MutableList<String>>()

            // Iterate through all vulnerabilities and filter based on component and patch level
            report.vulnerabilities.forEach { (patchLevel, groups) ->
                if (spl.toString() >= patchLevel) {
                    groups
                        .filter { it.components.contains(componentToString(component)) }
                        .forEach { group ->
                            val severity =
                                Severity.valueOf(group.severity.uppercase(Locale.getDefault()))
                            relevantFixes
                                .getOrPut(severity, ::mutableListOf)
                                .addAll(group.cveIdentifiers)
                        }
                }
            }
            return relevantFixes.mapValues { it.value.toList() }.toMap()
        }
    }

    /**
     * Retrieves the specific security patch level for a given component based on a security patch
     * level string. This method determines the type of [SecurityPatchLevel] to construct based on
     * the component type, interpreting the string as a date for date-based components or as a
     * version number for versioned components.
     *
     * @param component The [Component] enum indicating which type of component's patch level is
     *   being requested.
     * @param securityPatchLevel The string representation of the security patch level, which could
     *   be a date or a version number.
     * @return A [SecurityPatchLevel] instance corresponding to the specified component and patch
     *   level string.
     * @throws IllegalArgumentException If the input string is not in a valid format for the
     *   specified component type, or if the component requires a specific format that the string
     *   does not meet.
     */
    public open fun getSecurityPatchLevelString(
        component: Component,
        securityPatchLevel: String
    ): SecurityPatchLevel {
        return when (component) {
            Component.SYSTEM,
            Component.SYSTEM_MODULES,
            Component.VENDOR -> {
                // These components are expected to use DateBasedSpl
                DateBasedSecurityPatchLevel.fromString(securityPatchLevel)
            }
            Component.KERNEL,
            Component.WEBVIEW -> {
                // These components are expected to use VersionedSpl
                VersionedSecurityPatchLevel.fromString(securityPatchLevel)
            }
        }
    }

    /**
     * Fetches available updates from specified update providers. If no providers are specified, it
     * defaults to predefined URIs. This method queries each provider URI and processes the response
     * to gather update data relevant to the system's components.
     *
     * @param providers An optional list of [Uri] objects representing update providers. If null or
     *   empty, default providers are used.
     * @return A list of [UpdateInfo] objects, each representing an available update.
     */
    public open fun listAvailableUpdates(providers: List<Uri>? = null): List<UpdateInfo> {
        val updateInfoProviders: List<Uri> =
            if (providers.isNullOrEmpty()) {
                // TODO(musashi): Update when content providers are ready.
                listOf(Uri.parse("content://com.google.android.gms.apk/updateinfo"))
            } else {
                providers
            }

        val updates = mutableListOf<UpdateInfo>()
        val contentResolver = context.contentResolver
        val gson = Gson()

        updateInfoProviders.forEach { providerUri ->
            val cursor = contentResolver.query(providerUri, arrayOf("json"), null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val json = it.getString(it.getColumnIndexOrThrow("json"))
                    try {
                        val updateInfo = gson.fromJson(json, UpdateInfo::class.java) ?: continue
                        val component = Component.valueOf(updateInfo.component)
                        val deviceSpl = getDeviceSecurityPatchLevel(component)

                        if (
                            deviceSpl >=
                                getSecurityPatchLevelString(
                                    component,
                                    updateInfo.securityPatchLevel
                                )
                        ) {
                            continue
                        }
                        updates.add(updateInfo)
                    } catch (e: Exception) {
                        throw IllegalStateException("Wrong format of UpdateInfo: {$e}")
                    }
                }
            }
        }

        return updates
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

        val components = Component.values()

        components.forEach { component ->
            // TODO(musashi): Unblock once support for WebView is present.
            if (component == Component.WEBVIEW) return@forEach
            val deviceSpl =
                try {
                    getDeviceSecurityPatchLevel(component)
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "Failed to retrieve device SPL for component: ${component.name}",
                        e
                    )
                }

            try {
                if (component != Component.KERNEL) {
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
                    "Published SPL not available for component: ${component.name}",
                    e
                )
            }
        }
        return true
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
        val componentsToCheck = listOf(Component.SYSTEM, Component.VENDOR, Component.SYSTEM_MODULES)
        val allPatchedCves = mutableSetOf<String>()

        // Aggregate all CVEs from security fixes across necessary components
        for (component in componentsToCheck) {
            val spl = getDeviceSecurityPatchLevel(component)
            val fixes = getPatchedCves(component, spl)
            allPatchedCves.addAll(fixes.values.flatten())
        }

        // Check if all provided CVEs are in the patched CVEs list
        return cveList.all { allPatchedCves.contains(it) }
    }
}
