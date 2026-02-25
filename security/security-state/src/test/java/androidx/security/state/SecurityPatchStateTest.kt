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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.security.state.SecurityPatchState.Companion.getComponentSecurityPatchLevel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class SecurityPatchStateTest {

    private val mockContext: Context = mock<Context>()
    private val mockPackageManager: PackageManager = mock(PackageManager::class.java)
    private val mockBinder: IBinder = mock(IBinder::class.java)
    private val mockService: IUpdateInfoService = mock(IUpdateInfoService::class.java)

    private val mockSecurityStateManagerCompat: SecurityStateManagerCompat =
        mock<SecurityStateManagerCompat> {}
    private lateinit var securityState: SecurityPatchState

    @Before
    fun setup() {
        `when`(mockContext.packageName).thenReturn("com.example.test")
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockBinder.queryLocalInterface(anyString())).thenReturn(mockService)
        securityState = SecurityPatchState(mockContext, listOf(), mockSecurityStateManagerCompat)
    }

    @After
    fun tearDown() {
        // Ensure global flags are reset to defaults to prevent test pollution
        SecurityPatchState.USE_VENDOR_SPL = false
    }

    @Test
    fun testGetSystemModules_whenSystemModulesIsEmpty_usesDefaultSystemModules() {
        assert(securityState.getSystemModules() == SecurityPatchState.DEFAULT_SYSTEM_MODULES)
    }

    @Test
    fun testGetComponentSecurityPatchLevel_withSystemComponent_returnsDateBasedSpl() {
        val spl = getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM, "2022-01-01")
        assertTrue(spl is SecurityPatchState.DateBasedSecurityPatchLevel)
        assertEquals("2022-01-01", spl.toString())
    }

    @Test
    fun testGetComponentSecurityPatchLevel_withVendorComponent_whenVendorIsEnabled_returnsDateBasedSpl() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val spl = getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_VENDOR, "2022-01-01")
        assertTrue(spl is SecurityPatchState.DateBasedSecurityPatchLevel)
        assertEquals("2022-01-01", spl.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetComponentSecurityPatchLevel_withVendorComponent_whenVendorIsDisabled_throwsException() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false

        getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_VENDOR, "2022-01-01")
    }

    @Test
    fun testGetComponentSecurityPatchLevel_withKernelComponent_returnsVersionedSpl() {
        val spl = getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL, "1.2.3.4")
        assertTrue(spl is SecurityPatchState.VersionedSecurityPatchLevel)
        assertEquals("1.2.3.4", spl.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetComponentSecurityPatchLevel_withInvalidDateBasedInput_throwsException() {
        getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM, "invalid-date")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetComponentSecurityPatchLevel_withInvalidVersionedInput_throwsException() {
        getComponentSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL, "invalid-version")
    }

    @Test
    fun testLoadVulnerabilityReport_validJson_returnsCorrectData() {
        val jsonString =
            """
            {
                "vulnerabilities": {
                    "2020-01-01": [{
                        "cve_identifiers": ["CVE-2020-1234"],
                        "asb_identifiers": ["ASB-A-2020"],
                        "severity": "high",
                        "components": ["system", "vendor"]
                    }],
                    "2020-05-01": [{
                        "cve_identifiers": ["CVE-2020-5678"],
                        "asb_identifiers": ["PUB-A-5678"],
                        "severity": "moderate",
                        "components": ["system"]
                    }]
                },
                "extra_field": { test: 12345 },
                "kernel_lts_versions": {
                    "2020-01-01": ["4.14"]
                }
            }
        """
        securityState.loadVulnerabilityReport(jsonString)

        val cves =
            securityState.getPatchedCves(
                SecurityPatchState.COMPONENT_SYSTEM,
                SecurityPatchState.DateBasedSecurityPatchLevel(2022, 1, 1),
            )

        assertEquals(1, cves[SecurityPatchState.Severity.HIGH]?.size)
        assertEquals(1, cves[SecurityPatchState.Severity.MODERATE]?.size)
        assertEquals(setOf("CVE-2020-1234"), cves[SecurityPatchState.Severity.HIGH])
        assertEquals(setOf("CVE-2020-5678"), cves[SecurityPatchState.Severity.MODERATE])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLoadVulnerabilityReport_invalidAsb_throwsIllegalArgumentException() {
        val jsonString =
            """
            {
                "vulnerabilities": {
                    "2020-01-01": [{
                        "cve_identifiers": ["CVE-2020-1234"],
                        "asb_identifiers": ["ASB-123"],
                        "severity": "high",
                        "components": ["system", "vendor"]
                    }]
                },
                "extra_field": { test: 12345 },
                "kernel_lts_versions": {
                    "2020-01-01": ["4.14"]
                }
            }
        """
        securityState.loadVulnerabilityReport(jsonString)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLoadVulnerabilityReport_invalidJson_throwsIllegalArgumentException() {
        val invalidJson = "{ invalid json }"
        securityState.loadVulnerabilityReport(invalidJson)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    @Test
    fun testGetVulnerabilityReportUrl_validSdkVersion_returnsCorrectUrl() {
        val sdkVersion = 34 // Android 14
        val baseUrl = SecurityPatchState.DEFAULT_VULNERABILITY_REPORTS_URL
        val expectedUrl = "$baseUrl/v1/android_sdk_$sdkVersion.json"

        val actualUrl = SecurityPatchState.getVulnerabilityReportUrl(Uri.parse(baseUrl)).toString()
        assertEquals(expectedUrl, actualUrl)
    }

    @Test
    fun testGetDeviceSpl_validComponent_returnsCorrectSpl() {
        val systemSpl = "2020-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)

        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
                as SecurityPatchState.DateBasedSecurityPatchLevel
        assertEquals(2020, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test(expected = IllegalStateException::class)
    fun testGetDeviceSpl_noSplAvailable_throwsIllegalStateException() {
        val bundle = Bundle()
        // SPL not set in the bundle for the system component
        doReturn("").`when`(mockSecurityStateManagerCompat).getPackageVersion(Mockito.anyString())
        doReturn(bundle).`when`(mockSecurityStateManagerCompat).getGlobalSecurityState(anyString())

        securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetPublishedSpl_ThrowsWhenNoVulnerabilityReportLoaded() {
        securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
    }

    @Test
    fun testGetPublishedSpl_doesNotThrowWhenVulnerabilityReportLoadedFromConstructor() {
        securityState =
            SecurityPatchState(
                mockContext,
                listOf(),
                mockSecurityStateManagerCompat,
                vulnerabilityReportJsonString = generateMockReport("system", "2023-01-01"),
            )
        securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
    }

    @Test
    fun testGetDeviceSpl_ReturnsCorrectSplForUnpatchedSystemModules() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1235-4321"],
                        "asb_identifiers": ["ASB-A-2025111"],
                        "severity": "high",
                        "components": ["com.google.mainline.telemetry"]
                    }],
                    "2022-09-01": [{
                        "cve_identifiers": ["CVE-1236-4321"],
                        "asb_identifiers": ["ASB-A-2026111"],
                        "severity": "high",
                        "components": ["com.google.mainline.adservices"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2022-01-01")
        `when`(mockSecurityStateManagerCompat.getPackageVersion("com.google.mainline.telemetry"))
            .thenReturn("2023-05-01")
        `when`(mockSecurityStateManagerCompat.getPackageVersion("com.google.mainline.adservices"))
            .thenReturn("2022-05-01")
        `when`(mockSecurityStateManagerCompat.getPackageVersion("com.google.mainline.go.primary"))
            .thenReturn("2021-05-01")
        `when`(mockSecurityStateManagerCompat.getPackageVersion("com.google.mainline.go.telemetry"))
            .thenReturn("2024-05-01")

        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        assertEquals(2022, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetDeviceSpl_UpgradesToGlobalMax_WhenModuleIsCompliant() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                        "asb_identifiers": ["ASB-A-5000"],
                        "severity": "critical",
                        "components": ["vendor", "system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // GIVEN: Device has the Sept 2025 version installed (Compliant with JSON)
        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2025-09-01")

        // WHEN: We get the Device SPL for System Modules
        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // THEN: It should upgrade to the Global Max (Jan 2026)
        assertEquals(2026, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(5, spl.getDay())
    }

    @Test
    fun testGetDeviceSpl_HandlesEmptyBulletin_AndUpgrades() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2025-10-01": []
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // GIVEN: Device has the Sept 2025 version installed
        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2025-09-01")

        // WHEN: We get the Device SPL
        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // THEN: It should upgrade to the empty bulletin date
        assertEquals(2025, spl.getYear())
        assertEquals(10, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetDeviceSpl_CapsToGlobalMax_WhenDeviceIsAhead() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                        "asb_identifiers": ["ASB-A-5000"]
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // GIVEN: Device is on the Feb 2026 train (newer than bulletin)
        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2026-02-01")

        // WHEN: We get the Device SPL
        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // THEN: It should be capped to the Global Max (Jan 5)
        assertEquals(2026, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(5, spl.getDay())
    }

    @Test
    fun testGetDeviceSpl_RespectsOutdatedModule_DoesNotUpgrade() {
        // We simulate a mandatory update in March 2026 for modulemetadata
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2026-03-01": [{
                        "cve_identifiers": ["CVE-2026-9000"],
                        "asb_identifiers": ["ASB-A-9000"],
                        "severity": "critical",
                        "components": ["com.google.android.modulemetadata", "system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // GIVEN: Device is stuck on Sept 2025
        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2025-09-01")

        // WHEN: We get the Device SPL
        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // THEN: It should return the Device Version (Sept), NOT upgrade to March
        // because the device failed the check against the March entry.
        assertEquals(2025, spl.getYear())
        assertEquals(9, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetDeviceSpl_ReturnsLowestVersion_WhenOneModuleIsOutdated() {
        // JSON requires updates for BOTH modules in Sept 2025
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    },
                    {
                        "cve_identifiers": ["CVE-2025-2000"],
                        "asb_identifiers": ["ASB-A-2000"],
                        "severity": "high",
                        "components": ["com.google.mainline.telemetry"]
                    }],
                     "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                        "asb_identifiers": ["ASB-A-5000"]
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // GIVEN: Metadata is compliant (Sept), but Telemetry is old (Aug)
        `when`(
                mockSecurityStateManagerCompat.getPackageVersion(
                    "com.google.android.modulemetadata"
                )
            )
            .thenReturn("2025-09-01")
        `when`(mockSecurityStateManagerCompat.getPackageVersion("com.google.mainline.telemetry"))
            .thenReturn("2025-08-01")

        // WHEN: We get the Device SPL
        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // THEN: It returns the lowest version (Aug), NOT the Global Max
        assertEquals(2025, spl.getYear())
        assertEquals(8, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsGlobalMaxForSystemModules() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                     "2023-02-01": [{
                        "cve_identifiers": ["CVE-1234-9999"],
                        "asb_identifiers": ["ASB-A-2023999"],
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState
                .getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should return Global Max (Feb), not Module Max (Jan)
        assertEquals(2023, spl.getYear())
        assertEquals(2, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsGlobalMaxForSystem() {
        // Verifies that System SPL uses the Global Max strategy, capturing bulletin dates
        // even if they only contain Vendor or Kernel patches.
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-12-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                         "asb_identifiers": ["ASB-A-5000"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should return Jan 2026 (Global Max), ignoring the lack of "system" component
        assertEquals(2026, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(5, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsGlobalMaxForSystem_WithEmptyBulletin() {
        // Verifies System SPL reports dates from empty/advisory bulletins.
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2025-10-01": []
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should return Oct 2025
        assertEquals(2025, spl.getYear())
        assertEquals(10, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsGlobalMax_FromKernelUpdate() {
        // Verifies that Kernel-only updates correctly drive the System SPL.
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2026-02-05": [{
                        "cve_identifiers": ["CVE-2026-8000"],
                        "asb_identifiers": ["ASB-A-8000"],
                        "severity": "high",
                        "components": ["kernel"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should return Feb 2026
        assertEquals(2026, spl.getYear())
        assertEquals(2, spl.getMonth())
        assertEquals(5, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsLatestDay_WhenMonthHasMultipleDates() {
        // Verifies that the library takes the max() date when a month has multiple entries.
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2026-01-01": [{
                        "cve_identifiers": ["CVE-2026-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-2000"],
                        "asb_identifiers": ["ASB-A-2000"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should return Jan 05
        assertEquals(2026, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(5, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_Vendor_DoesNotUseGlobalMax() {
        // Regression Test: Verifies that Vendor SPL remains isolated and uses component-specific
        // max logic, ensuring it doesn't artificially jump due to System updates.
        SecurityPatchState.USE_VENDOR_SPL = true

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2026-01-01": [{
                        "cve_identifiers": ["CVE-2026-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["vendor"]
                    }],
                    "2026-02-01": [{
                        "cve_identifiers": ["CVE-2026-2000"],
                         "asb_identifiers": ["ASB-A-2000"],
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val vendorSpl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_VENDOR)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        // Should remain Jan 1 (Vendor Max), ignoring Feb 1 (System Max)
        assertEquals(2026, vendorSpl.getYear())
        assertEquals(1, vendorSpl.getMonth())
        assertEquals(1, vendorSpl.getDay())
    }

    @Test(expected = IllegalStateException::class)
    fun testGetPublishedSpl_Throws_WhenReportHasNoDates() {
        val jsonInput =
            """
            {
                "vulnerabilities": {},
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
    }

    @Test
    fun testGetPublishedSpl_withVendorComponent_whenVendorIsEnabled_returnsCorrectSpl() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val spl =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_VENDOR)[0]
                as SecurityPatchState.DateBasedSecurityPatchLevel

        assertEquals(2023, spl.getYear())
        assertEquals(5, spl.getMonth())
        assertEquals(15, spl.getDay())
    }

    @Test(expected = IllegalStateException::class)
    fun testGetPublishedSpl_withVendorComponent_whenVendorIsDisabled_throwsException() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)
        securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_VENDOR)
    }

    @Test
    fun testGetPublishedSpl_withKernelComponent_differentVersionsSameSpl_returnsCorrectVersions() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": { "2023-05-01": [ "5.4.123", "6.1.234.25" ] }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val versions =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)
        val version0 = versions[0] as SecurityPatchState.VersionedSecurityPatchLevel
        val version1 = versions[1] as SecurityPatchState.VersionedSecurityPatchLevel

        assertEquals(5, version0.getMajorVersion())
        assertEquals(4, version0.getMinorVersion())
        assertEquals(123, version0.getPatchVersion())
        assertEquals(6, version1.getMajorVersion())
        assertEquals(1, version1.getMinorVersion())
        assertEquals(234, version1.getBuildVersion())
        assertEquals(25, version1.getPatchVersion())

        assertEquals(2, versions.size)
    }

    @Test
    fun testGetPublishedSpl_withKernelComponent_differentVersionsDifferentSpls_returnsCorrectVersions() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {
                    "2024-04-27": ["5.10.209", "5.15.148"],
                    "2024-06-12": ["5.15.149"],
                    "2024-06-21": ["5.10.210"]
                }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val versions =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)
        val version0 = versions[0] as SecurityPatchState.VersionedSecurityPatchLevel
        val version1 = versions[1] as SecurityPatchState.VersionedSecurityPatchLevel

        assertEquals(5, version0.getMajorVersion())
        assertEquals(10, version0.getMinorVersion())
        assertEquals(210, version0.getPatchVersion())
        assertEquals(5, version1.getMajorVersion())
        assertEquals(15, version1.getMinorVersion())
        assertEquals(149, version1.getPatchVersion())

        assertEquals(2, versions.size)
    }

    @Test
    fun testGetPublishedSpl_withKernelComponent_returnsCorrectVersion() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {
                    "2021-11-05": ["5.4.86"],
                    "2022-11-05": ["5.4.147"],
                    "2023-11-05": ["5.4.233"]
                }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val versions =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)
        val version0 = versions[0] as SecurityPatchState.VersionedSecurityPatchLevel

        assertEquals(5, version0.getMajorVersion())
        assertEquals(4, version0.getMinorVersion())
        assertEquals(233, version0.getPatchVersion())

        assertEquals(1, versions.size)
    }

    @Test
    fun testGetPublishedSpl_withKernelComponent_returnsEmptyList() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-15": [{
                        "cve_identifiers": ["CVE-5678-1234"],
                        "asb_identifiers": ["ASB-A-2024222"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        val versions =
            securityState.getPublishedSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)
        assertTrue(versions.isEmpty())
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewerSystemUpdate() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports a newer SYSTEM update (2025-05-01)
            val update =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-05-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(update)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it returns the newer update SPL
            assertEquals("2025-05-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsDeviceSpl_whenNoProvidersFound() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND no providers are discovered
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(emptyList())

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it strictly falls back to the Device SPL
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsDeviceSpl_whenAllProvidersEmpty() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND multiple trusted providers exist
            val provider1 = createUpdateInfoServiceResolveInfo("com.p1", isSystem = true)
            val provider2 = createUpdateInfoServiceResolveInfo("com.p2", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(provider1, provider2))

            setupUpdateInfoServiceBinding()

            // AND they ALL return empty lists (no updates available from anyone)
            setupUpdateInfoServiceResponse(emptyList())

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it safely returns the Device SPL
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsDeviceSpl_whenSystemUpdateIsOlder() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-05-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-05-01")

            // AND the provider reports an OLDER update (2025-01-01)
            // (e.g. stale cache or rolled back server side)
            val oldUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-01-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(oldUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it ignores the update and returns the Device SPL
            assertEquals("2025-05-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsDeviceSpl_whenSystemUpdateIsEqual() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND the provider reports the SAME version (2025-01-01)
            val sameUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-01-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(sameUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it returns the Device SPL (no change)
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewestSystemUpdate_fromMultipleProviders() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND two trusted providers exist for the SYSTEM component
            // (e.g., an OEM Updater and GOTA both claim to manage the System image)
            val provider1 = createUpdateInfoServiceResolveInfo("com.oem.updater", isSystem = true)
            val provider2 =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(provider1, provider2))

            setupUpdateInfoServiceBinding()

            // Provider 1 reports an older update (March)
            val update1 =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-03-01")
                    .build()
            val result1 =
                UpdateCheckResult(
                    providerPackageName = "com.oem.updater",
                    updates = listOf(update1),
                    lastCheckTimeMillis = 1000L,
                )

            // Provider 2 reports a NEWER update (June)
            val update2 =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-06-01")
                    .build()
            val result2 =
                UpdateCheckResult(
                    providerPackageName = "com.google.android.gms",
                    updates = listOf(update2),
                    lastCheckTimeMillis = 1000L,
                )

            // Mock the service to return both results
            `when`(mockService.listAvailableUpdates()).thenReturn(result1).thenReturn(result2)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it returns the newest one
            assertEquals("2025-06-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewestSystemUpdate_fromSingleProviderList() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a single provider reports MULTIPLE updates for SYSTEM
            val olderUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-02-01")
                    .build()

            val newestUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-06-01") // The winner
                    .build()

            val middleUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-04-01")
                    .build()

            // Setup the service to return this list
            setupTrustedUpdateInfoServiceWithUpdates(olderUpdate, newestUpdate, middleUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it correctly identifies the newest one from the list
            assertEquals("2025-06-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsSystemUpdate_fromWorkingProvider_whenAnotherFails() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND two trusted providers exist
            val brokenProvider = createUpdateInfoServiceResolveInfo("com.broken", isSystem = true)
            val workingProvider = createUpdateInfoServiceResolveInfo("com.working", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(brokenProvider, workingProvider))

            // SETUP: "com.working" binds successfully, "com.broken" fails
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenAnswer { invocation ->
                    val intent = invocation.getArgument<Intent>(0)
                    val connection = invocation.getArgument<ServiceConnection>(1)

                    if (intent.component?.packageName == "com.working") {
                        // Working provider: Connect and return a valid SYSTEM update
                        val update =
                            UpdateInfo.Builder()
                                .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                                .setSecurityPatchLevel("2025-06-01")
                                .build()
                        val result =
                            UpdateCheckResult(
                                providerPackageName = "com.working",
                                updates = listOf(update),
                                lastCheckTimeMillis = 1000L,
                            )

                        // Mock the service call for the successful bind
                        `when`(mockService.listAvailableUpdates()).thenReturn(result)

                        connection.onServiceConnected(intent.component, mockBinder)
                        true
                    } else {
                        // Broken provider: Refuses bind (returns false)
                        false
                    }
                }

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the update from the working provider is found and returned
            // (Proving the broken provider was ignored without stopping the process)
            assertEquals("2025-06-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_onTimeout_returnsDeviceSpl() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // AND the service hangs (bindService returns true but no connection happens)
            `when`(mockContext.bindService(any(), any(), anyInt())).thenReturn(true)

            // WHEN we request the Available SPL for SYSTEM with a short timeout (100ms)
            // (This confirms the parameter is propagated to queryAllAvailableUpdates)
            val result =
                securityState.fetchAvailableSecurityPatchLevel(
                    SecurityPatchState.COMPONENT_SYSTEM,
                    timeoutMillis = 100L,
                )

            // THEN it returns the Device SPL (fallback) immediately after the timeout
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsSystemUpdate_fromMixedComponentResults() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND two trusted providers exist:
            // 1. "com.system.updater" (Reports SYSTEM updates)
            // 2. "com.kernel.updater" (Reports KERNEL updates)
            val systemProvider =
                createUpdateInfoServiceResolveInfo("com.system.updater", isSystem = true)
            val kernelProvider =
                createUpdateInfoServiceResolveInfo("com.kernel.updater", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(systemProvider, kernelProvider))

            setupUpdateInfoServiceBinding()

            // SETUP: Create the distinct updates
            val systemUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-06-01")
                    .build()
            val systemResult =
                UpdateCheckResult(
                    providerPackageName = "com.system.updater",
                    updates = listOf(systemUpdate),
                    lastCheckTimeMillis = 1000L,
                )

            val kernelUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_KERNEL)
                    .setSecurityPatchLevel("5.15.1")
                    .build()
            val kernelResult =
                UpdateCheckResult(
                    providerPackageName = "com.kernel.updater",
                    updates = listOf(kernelUpdate),
                    lastCheckTimeMillis = 1000L,
                )

            // Configure the mock service to return different results for the sequential calls.
            `when`(mockService.listAvailableUpdates())
                .thenReturn(systemResult)
                .thenReturn(kernelResult)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN it correctly finds the SYSTEM update (from Provider 1)
            // and ignores the KERNEL update (from Provider 2)
            assertEquals("2025-06-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsSystemUpdate_ignoringMalformedEntries() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider returns a list with one GOOD update and one BAD update
            val goodUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-05-01")
                    .build()

            val badUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("INVALID_SPL_STRING")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(goodUpdate, badUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the invalid one is ignored, and the valid one is returned
            assertEquals("2025-05-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewerKernelVersion() {
        runBlocking {
            // GIVEN the Device Kernel is 5.10.99
            mockDeviceSpl(SecurityPatchState.COMPONENT_KERNEL, "5.10.99")

            // AND a provider reports a newer Kernel 5.15.1
            val kernelUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_KERNEL)
                    .setSecurityPatchLevel("5.15.1")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(kernelUpdate)

            // WHEN we request the Available SPL for KERNEL
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)

            // THEN it correctly returns the newer version
            assertEquals("5.15.1", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewestKernelVersion() {
        runBlocking {
            // GIVEN the Device Kernel is 5.10.99
            mockDeviceSpl(SecurityPatchState.COMPONENT_KERNEL, "5.10.99")

            // AND a provider reports multiple kernel updates
            val vSame =
                UpdateInfo.Builder().setComponent("KERNEL").setSecurityPatchLevel("5.10.99").build()
            val vNewerPatch =
                UpdateInfo.Builder()
                    .setComponent("KERNEL")
                    .setSecurityPatchLevel("5.10.105")
                    .build()
            val vNewestMajor =
                UpdateInfo.Builder().setComponent("KERNEL").setSecurityPatchLevel("5.15.1").build()

            setupTrustedUpdateInfoServiceWithUpdates(vSame, vNewerPatch, vNewestMajor)

            // WHEN we request the Available SPL for KERNEL
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_KERNEL)

            // THEN it returns the highest version number
            assertEquals("5.15.1", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_returnsNewerSystemModulesUpdate() {
        runBlocking {
            // 1. Setup: Load a Vulnerability Report that knows about two versions:
            // - 2025-01-01 (Old)
            // - 2025-06-01 (New/Published)
            val jsonReport =
                """
                {
                    "vulnerabilities": {
                        "2025-01-01": [{
                            "cve_identifiers": ["CVE-2025-0001"],
                            "asb_identifiers": ["ASB-A-2025001"],
                            "severity": "high",
                            "components": ["com.google.android.modulemetadata"]
                        }],
                        "2025-06-01": [{
                            "cve_identifiers": ["CVE-2025-0002"],
                            "asb_identifiers": ["ASB-A-2025002"],
                            "severity": "critical",
                            "components": ["com.google.android.modulemetadata"]
                        }]
                    },
                    "kernel_lts_versions": {}
                }
                """
                    .trimIndent()
            securityState.loadVulnerabilityReport(jsonReport)

            // 2. Mock Device State: The device is still on the OLD version (2025-01-01)
            // Even though the report sees 2025-06-01, the device hasn't installed it.
            // SecurityPatchState.getDeviceSecurityPatchLevel() will correctly calculate
            // 2025-01-01 based on this package version.
            `when`(
                    mockSecurityStateManagerCompat.getPackageVersion(
                        "com.google.android.modulemetadata"
                    )
                )
                .thenReturn("2025-01-01")

            // 3. Setup Provider: The service explicitly offers the "2025-06-01" update.
            // This signals that the "Published" update is now "Available" for this device.
            val update =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                    .setSecurityPatchLevel("2025-06-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(update)

            // 4. Action: Request the Available SPL for SYSTEM_MODULES
            val result =
                securityState.fetchAvailableSecurityPatchLevel(
                    SecurityPatchState.COMPONENT_SYSTEM_MODULES
                )

            // 5. Verify: It identifies and returns the newer update (2025-06-01)
            assertEquals("2025-06-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUpdatesForOtherComponents() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports a new update for "SYSTEM_MODULES"
            // (But we are going to query for SYSTEM)
            val modulesUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                    .setSecurityPatchLevel("2025-05-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(modulesUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the SYSTEM_MODULES update is ignored (result matches device)
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresPackageSpecificUpdatesForSystemModules() {
        runBlocking {
            val jsonReport =
                """
                {
                    "vulnerabilities": {
                        "2025-01-01": [{
                            "cve_identifiers": ["CVE-2025-0001"],
                            "asb_identifiers": ["ASB-A-2025001"],
                            "severity": "high",
                            "components": ["com.google.android.modulemetadata"]
                        }]
                    },
                    "kernel_lts_versions": {}
                }
                """
                    .trimIndent()
            securityState.loadVulnerabilityReport(jsonReport)

            // GIVEN Device SPL for SYSTEM_MODULES is 2025-01-01
            // (Mocking package version for the module mentioned in the report)
            `when`(
                    mockSecurityStateManagerCompat.getPackageVersion(
                        "com.google.android.modulemetadata"
                    )
                )
                .thenReturn("2025-01-01")

            // AND a provider reports an update for a specific module package
            // (The library expects "SYSTEM_MODULES", not package names)
            val packageUpdate =
                UpdateInfo.Builder()
                    .setComponent("com.google.android.modulemetadata")
                    .setSecurityPatchLevel("2025-06-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(packageUpdate)

            // WHEN we request Available SPL for SYSTEM_MODULES
            val result =
                securityState.fetchAvailableSecurityPatchLevel(
                    SecurityPatchState.COMPONENT_SYSTEM_MODULES
                )

            // THEN the package-level update is ignored, returning the Device SPL
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUnknownComponents() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports an update for an unknown component
            val weirdUpdate =
                UpdateInfo.Builder()
                    .setComponent("UNKNOWN_COMPONENT_XYZ")
                    .setSecurityPatchLevel("2025-05-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(weirdUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the unknown component is ignored
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUpdatesWithWrongComponentCase() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports a newer update but uses lowercase "system"
            // (The contract requires "SYSTEM")
            val update =
                UpdateInfo.Builder()
                    .setComponent("system") // Lowercase
                    .setSecurityPatchLevel("2025-06-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(update)

            // WHEN we request the Available SPL for "SYSTEM"
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the lowercase update is ignored, returning Device SPL
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUpdatesWithWrongFormatForComponent() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports a "SYSTEM" update but uses a Kernel Version format
            // (This simulates a provider sending valid-looking but wrong-type data)
            val badTypeUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM) // Says SYSTEM
                    .setSecurityPatchLevel("5.10.199") // But provides Version data (Not a Date)
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(badTypeUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the update is ignored (parsing failed for Date type), returning Device SPL
            // (It does not crash or try to compare "5.10.199" with a Date)
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUpdatesWithMalformedSpl() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports an update with a completely malformed string
            val badUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("NOT_A_DATE")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(badUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the update is ignored (parsing fails), returning Device SPL
            // (Verifies that IllegalArgumentException is caught and logged)
            assertEquals("2025-01-01", result.toString())
        }
    }

    @Test
    fun testFetchAvailableSecurityPatchLevel_ignoresUpdatesWithInvalidDateValues() {
        runBlocking {
            // GIVEN the Device SPL for SYSTEM is 2025-01-01
            mockDeviceSpl(SecurityPatchState.COMPONENT_SYSTEM, "2025-01-01")

            // AND a provider reports an update with a valid format but invalid date
            // (e.g., February 30th does not exist)
            val invalidDateUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-02-30")
                    .build()

            // AND a valid newer update exists
            val validUpdate =
                UpdateInfo.Builder()
                    .setComponent(SecurityPatchState.COMPONENT_SYSTEM)
                    .setSecurityPatchLevel("2025-03-01")
                    .build()

            setupTrustedUpdateInfoServiceWithUpdates(invalidDateUpdate, validUpdate)

            // WHEN we request the Available SPL for SYSTEM
            val result =
                securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the invalid date is ignored, and the valid one is returned
            // (Verifies that parsing exceptions are caught and filtered)
            assertEquals("2025-03-01", result.toString())
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testFetchAvailableSecurityPatchLevel_propagatesDeviceStateExceptions() {
        runBlocking {
            // GIVEN the device state is missing/corrupt (getDeviceSecurityPatchLevel throws)
            `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
                .thenThrow(IllegalStateException("System SPL missing"))

            // WHEN we request the Available SPL for SYSTEM
            securityState.fetchAvailableSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)

            // THEN the exception propagates (Fail Fast)
        }
    }

    private fun generateMockReport(component: String, date: String): String {
        return """
            {
                "vulnerabilities": {
                    "$date": [{
                        "cve_identifiers": ["CVE-1234-6789"],
                        "asb_identifiers": ["ASB-A-2023333"],
                        "severity": "high",
                        "components": ["$component"]
                    }]
                },
                "kernel_lts_versions": {}
            }
        """
            .trimIndent()
    }

    @Test
    fun testGetPatchedCves_ReturnsNoCves() {
        securityState.loadVulnerabilityReport(generateMockReport("system", "2023-01-01"))

        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-01")
        val cves = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM_MODULES, spl)

        assertEquals(null, cves[SecurityPatchState.Severity.CRITICAL])
        assertEquals(null, cves[SecurityPatchState.Severity.HIGH])
        assertEquals(null, cves[SecurityPatchState.Severity.MODERATE])
        assertEquals(null, cves[SecurityPatchState.Severity.LOW])

        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2022-01-01")
        val cves2 = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM, spl2)

        assertEquals(null, cves2[SecurityPatchState.Severity.CRITICAL])
        assertEquals(null, cves2[SecurityPatchState.Severity.HIGH])
        assertEquals(null, cves2[SecurityPatchState.Severity.MODERATE])
        assertEquals(null, cves2[SecurityPatchState.Severity.LOW])
    }

    @Test
    fun testGetPatchedCves_withSystemComponent_returnsCorrectCvesCategorizedBySeverity() {
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-01")
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2023-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val cves = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM, spl)

        assertEquals(2, cves[SecurityPatchState.Severity.HIGH]?.size)
        assertEquals(
            setOf("CVE-2023-0001", "CVE-2023-0002"),
            cves[SecurityPatchState.Severity.HIGH],
        )

        assertEquals(null, cves[SecurityPatchState.Severity.MODERATE])
    }

    @Test
    fun testGetPatchedCves_withSystemModulesComponent_returnsCorrectCvesCategorizedBySeverity() {
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-15")
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2023-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["com.google.android.modulemetadata"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val cves = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM_MODULES, spl)

        assertEquals(1, cves[SecurityPatchState.Severity.MODERATE]?.size)
        assertEquals(setOf("CVE-2023-0010"), cves[SecurityPatchState.Severity.MODERATE])

        assertEquals(null, cves[SecurityPatchState.Severity.HIGH])
    }

    @Test
    fun testGetPatchedCves_withVendorComponent_whenVendorIsEnabled_returnsCorrectCvesCategorizedBySeverity() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-15")
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2023-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val cves = securityState.getPatchedCves(SecurityPatchState.COMPONENT_VENDOR, spl)

        assertEquals(1, cves[SecurityPatchState.Severity.MODERATE]?.size)
        assertEquals(setOf("CVE-2023-0010"), cves[SecurityPatchState.Severity.MODERATE])

        assertEquals(null, cves[SecurityPatchState.Severity.HIGH])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPatchedCves_withVendorComponent_whenVendorIsDisabled_throwsException() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2022-01-01")

        securityState.getPatchedCves(SecurityPatchState.COMPONENT_VENDOR, spl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetPatchedCves_ThrowsExceptionForInvalidComponent() {
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-01")

        securityState.getPatchedCves(SecurityPatchState.COMPONENT_KERNEL, spl)
    }

    @Test
    fun testVersionedSpl_FromString_ValidInput_ReturnsCorrectVersionedSpl() {
        val version = "1.2.3.4"
        val spl = SecurityPatchState.VersionedSecurityPatchLevel.fromString(version)
        assertEquals(1, spl.getMajorVersion())
        assertEquals(2, spl.getMinorVersion())
        assertEquals(3, spl.getBuildVersion())
        assertEquals(4, spl.getPatchVersion())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testVersionedSpl_FromString_InvalidFormat_ThrowsException() {
        val version = "1"
        SecurityPatchState.VersionedSecurityPatchLevel.fromString(version)
    }

    @Test
    fun testVersionedSpl_ToString_ReturnsCorrectFormat() {
        val spl = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        assertEquals("1.2.3.4", spl.toString())
        val splNoPatch = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 0, 0)
        assertEquals("1.2", splNoPatch.toString())
        val splPatchOnly = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 0, 1)
        assertEquals("1.2.1", splPatchOnly.toString())
    }

    @Test
    fun testVersionedSpl_CompareTo_EqualObjects_ReturnsZero() {
        val spl1 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        val spl2 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        assertEquals(0, spl1.compareTo(spl2))
    }

    @Test
    fun testVersionedSpl_CompareTo_MajorVersionDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        val spl2 = SecurityPatchState.VersionedSecurityPatchLevel(2, 2, 3, 4)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testVersionedSpl_CompareTo_MinorVersionDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        val spl2 = SecurityPatchState.VersionedSecurityPatchLevel(1, 3, 3, 4)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testVersionedSpl_CompareTo_PatchVersionDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        val spl2 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 5)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testVersionedSpl_CompareTo_BuildVersionDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 3, 4)
        val spl2 = SecurityPatchState.VersionedSecurityPatchLevel(1, 2, 4, 4)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testDateBasedSpl_FromString_ValidInput_ReturnsCorrectDateBasedSpl() {
        val dateString = "2023-09-15"
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString(dateString)
        assertEquals(2023, spl.getYear())
        assertEquals(9, spl.getMonth())
        assertEquals(15, spl.getDay())
    }

    @Test
    fun testDateBasedSpl_FromString_MissingDay_ReturnsCorrectDateBasedSpl() {
        val dateString = "2023-09" // Some SPLs only have year and month.
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString(dateString)
        assertEquals(2023, spl.getYear())
        assertEquals(9, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDateBasedSpl_FromString_MissingMonth_ThrowsException() {
        val invalidDate = "2023"
        SecurityPatchState.DateBasedSecurityPatchLevel.fromString(invalidDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDateBasedSpl_FromString_InvalidDate_ThrowsException() {
        val invalidDate = "2023-13-15"
        SecurityPatchState.DateBasedSecurityPatchLevel.fromString(invalidDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDateBasedSpl_FromString_InvalidFormat_ThrowsException() {
        val invalidDate = "2023/09/15"
        SecurityPatchState.DateBasedSecurityPatchLevel.fromString(invalidDate)
    }

    @Test
    fun testDateBasedSpl_ToString_ReturnsCorrectFormat() {
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        assertEquals("2023-09-15", spl.toString())
    }

    @Test
    fun testDateBasedSpl_CompareTo_EqualObjects_ReturnsZero() {
        val spl1 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        assertEquals(0, spl1.compareTo(spl2))
    }

    @Test
    fun testDateBasedSpl_CompareTo_YearDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel(2024, 9, 15)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testDateBasedSpl_CompareTo_MonthDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 8, 15)
        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testDateBasedSpl_CompareTo_DayDifference_ReturnsDifference() {
        val spl1 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 14)
        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 9, 15)
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testGenericStringSpl_CompareTo_ReturnsDifference() {
        val spl1 = SecurityPatchState.GenericStringSecurityPatchLevel("ale")
        val spl2 = SecurityPatchState.GenericStringSecurityPatchLevel("great")
        assertTrue(spl1 < spl2)
        assertTrue(spl2 > spl1)
    }

    @Test
    fun testIsDeviceFullyUpdated_withUpdatedSpl_returnsTrue() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-05-01")
        bundle.putString("vendor_spl", "2023-02-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-1321"],
                        "asb_identifiers": ["ASB-A-2023121"],
                        "severity": "critical",
                        "components": ["system"]
                    }],
                    "2023-02-01": [{
                        "cve_identifiers": ["CVE-1234-3321"],
                        "asb_identifiers": ["ASB-A-2023151"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": { "2023-05-01": [ "5.4.123", "6.1.234.25" ] }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        assertTrue(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_withOutdatedVendorSpl_whenVendorIsEnabled_returnsFalse() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-01-01")
        bundle.putString("vendor_spl", "2020-01-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-1321"],
                        "asb_identifiers": ["ASB-A-2023121"],
                        "severity": "critical",
                        "components": ["system"]
                    }],
                    "2023-02-01": [{
                        "cve_identifiers": ["CVE-1234-3321"],
                        "asb_identifiers": ["ASB-A-2023151"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": { "2023-05-01": [ "5.4.123", "6.1.234.25" ] }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        assertFalse(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_withOutdatedVendorSpl_whenVendorIsDisabled_returnsTrue() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-05-01")
        bundle.putString("vendor_spl", "2020-01-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-1321"],
                        "asb_identifiers": ["ASB-A-2023121"],
                        "severity": "critical",
                        "components": ["system"]
                    }],
                    "2023-02-01": [{
                        "cve_identifiers": ["CVE-1234-3321"],
                        "asb_identifiers": ["ASB-A-2023151"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": { "2023-05-01": [ "5.4.123", "6.1.234.25" ] }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        assertTrue(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_withOutdatedSpl_returnsFalse() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2022-01-01")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2024-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-1321"],
                        "asb_identifiers": ["ASB-A-2023121"],
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": { "2023-05-01": [ "5.4.123", "6.1.234.25" ] }
            }
            """
                .trimIndent()

        securityState.loadVulnerabilityReport(jsonInput)

        assertFalse(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_ReturnsFalse_WhenSystemSplIsOlderThanGlobalMax() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2026-01-01")
        bundle.putString("vendor_spl", "2026-01-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2026-01-01")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2026-01-01").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2026-01-01": [{
                        "cve_identifiers": ["CVE-2026-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2026-02-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                        "asb_identifiers": ["ASB-A-5000"],
                        "severity": "critical",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // False: Device (Jan 1) < Global Max (Feb 5)
        assertFalse(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_ReturnsTrue_WhenSystemSplMatchesEmptyBulletin() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2025-10-01")
        bundle.putString("vendor_spl", "2025-10-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2025-10-01")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2025-10-01").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2025-10-01": []
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // True: Device (Oct 1) == Global Max (Oct 1)
        assertTrue(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_ReturnsTrue_WhenMainlineUpgradesToGlobalMax() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2026-01-05")
        bundle.putString("vendor_spl", "2026-01-05")
        bundle.putString("kernel_version", "5.4.123")
        // Device Mainline is Sept 2025
        bundle.putString("com.google.android.modulemetadata", "2025-09-01")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2025-09-01").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                     "2026-01-05": [{
                        "cve_identifiers": ["CVE-2026-5000"],
                        "asb_identifiers": ["ASB-A-5000"],
                        "severity": "critical",
                        "components": ["system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // True: Mainline upgrades to Jan 5, matching the Published SPL.
        assertTrue(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testIsDeviceFullyUpdated_ReturnsFalse_WhenMainlineIsTrulyOutdated() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2026-03-01")
        bundle.putString("vendor_spl", "2026-03-01")
        bundle.putString("kernel_version", "5.4.123")
        // Device Mainline is Sept 2025
        bundle.putString("com.google.android.modulemetadata", "2025-09-01")

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn("2025-09-01").`when`(mockSecurityStateManagerCompat).getPackageVersion(anyString())

        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2025-09-01": [{
                        "cve_identifiers": ["CVE-2025-1000"],
                        "asb_identifiers": ["ASB-A-1000"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2026-03-01": [{
                     "cve_identifiers": ["CVE-2026-9000"],
                     "asb_identifiers": ["ASB-A-9000"],
                     "severity": "critical",
                        "components": ["com.google.android.modulemetadata", "system"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        // False: Device (Sept) < Published (March). Upgrade was blocked.
        assertFalse(securityState.isDeviceFullyUpdated())
    }

    @Test
    fun testAreCvesPatched_ReturnsTrue() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2023-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }],
                    "2025-12-01": [{
                        "cve_identifiers": ["CVE-2025-0001"],
                        "asb_identifiers": ["ASB-A-2025011"],
                        "severity": "moderate",
                        "components": ["system"]
                    }],
                    "2025-12-15": [{
                        "cve_identifiers": ["CVE-2025-0002"],
                        "asb_identifiers": ["ASB-A-2025022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val systemSpl = "2024-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)
        bundle.putString("vendor_spl", systemSpl)
        bundle.putString("com.google.android.modulemetadata", systemSpl)
        bundle.putStringArray("system_supplemental_security_patches", arrayOf("CVE-2025-0001"))
        bundle.putStringArray("vendor_supplemental_security_patches", arrayOf("CVE-2025-0002"))

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn(systemSpl)
            .`when`(mockSecurityStateManagerCompat)
            .getPackageVersion(Mockito.anyString())

        assertTrue(
            securityState.areCvesPatched(
                listOf("CVE-2023-0001", "CVE-2023-0002", "CVE-2025-0001", "CVE-2025-0002")
            )
        )
    }

    @Test
    fun testAreCvesPatched_ReturnsFalseOldSpl() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2021-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2022-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2021-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val systemSpl = "2020-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)
        bundle.putString("vendor_spl", systemSpl)
        bundle.putString("com.google.android.modulemetadata", systemSpl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn(systemSpl)
            .`when`(mockSecurityStateManagerCompat)
            .getPackageVersion(Mockito.anyString())

        assertFalse(
            securityState.areCvesPatched(listOf("CVE-2023-0010", "CVE-2023-0001", "CVE-2023-0002"))
        )
    }

    @Test
    fun testAreCvesPatched_ReturnsFalseExtraCve() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2021-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2022-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2021-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val systemSpl = "2023-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)
        bundle.putString("vendor_spl", systemSpl)
        bundle.putString("com.google.android.modulemetadata", systemSpl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn(systemSpl)
            .`when`(mockSecurityStateManagerCompat)
            .getPackageVersion(Mockito.anyString())

        assertFalse(
            securityState.areCvesPatched(listOf("CVE-2024-1010", "CVE-2023-0001", "CVE-2023-0002"))
        )
    }

    @Test
    fun testAreCvesPatched_withVendorCve_whenVendorIsEnabled_returnsTrue() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = true
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2021-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2022-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2021-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val systemSpl = "2023-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)
        bundle.putString("vendor_spl", systemSpl)
        bundle.putString("com.google.android.modulemetadata", systemSpl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn(systemSpl)
            .`when`(mockSecurityStateManagerCompat)
            .getPackageVersion(Mockito.anyString())

        assertTrue(securityState.areCvesPatched(listOf("CVE-2023-0010")))
    }

    @Test
    fun testAreCvesPatched_withVendorCve_whenVendorIsDisabled_returnsFalse() {
        SecurityPatchState.Companion.USE_VENDOR_SPL = false
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2021-05-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
                    }],
                    "2022-01-01": [{
                        "cve_identifiers": ["CVE-2023-0001", "CVE-2023-0002"],
                        "asb_identifiers": ["ASB-A-2023011"],
                        "severity": "high",
                        "components": ["system"]
                    }],
                    "2021-01-15": [{
                        "cve_identifiers": ["CVE-2023-0010"],
                        "asb_identifiers": ["ASB-A-2023022"],
                        "severity": "moderate",
                        "components": ["vendor"]
                    }]
                },
                "kernel_lts_versions": {}
            }
            """
                .trimIndent()
        securityState.loadVulnerabilityReport(jsonInput)

        val systemSpl = "2023-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)
        bundle.putString("vendor_spl", systemSpl)
        bundle.putString("com.google.android.modulemetadata", systemSpl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
        doReturn(systemSpl)
            .`when`(mockSecurityStateManagerCompat)
            .getPackageVersion(Mockito.anyString())

        assertFalse(securityState.areCvesPatched(listOf("CVE-2023-0010")))
    }

    // ============================================================================================
    // Tests for queryAllAvailableUpdates()
    //
    // The following tests verify the behavior of querying for all available updates, including:
    // 1. Service Discovery (finding trusted providers)
    // 2. Data Retrieval (marshalling results)
    // 3. Error Handling (connection failures, timeouts, runtime exceptions)
    // 4. Threading & Safety (background execution, cancellation cleanup)
    // ============================================================================================

    // --------------------------------------------------------------------------------------------
    // Phase 1: Discovery & Filtering
    // --------------------------------------------------------------------------------------------

    @Test
    fun testQueryAllAvailableUpdates_returnsEmpty_whenNoProvidersFound() {
        runBlocking {
            // GIVEN no services handle the intent (or none are trusted)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(emptyList())

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get an empty list immediately
            assertTrue(results.isEmpty())
            // AND no attempt was made to bind
            verify(mockContext, times(0)).bindService(any(), any(), anyInt())
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_ignoresNonSystemApps() {
        runBlocking {
            // GIVEN a third-party app exposes the service
            val untrustedInfo =
                createUpdateInfoServiceResolveInfo("com.thirdparty", isSystem = false)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(untrustedInfo))

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN it is ignored (result list is empty)
            assertTrue(results.isEmpty())
            // AND we never attempted to bind
            verify(mockContext, times(0)).bindService(any(), any(), anyInt())
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_includesSystemApps() {
        runBlocking {
            // GIVEN a system app exposes the service
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            setupUpdateInfoServiceResponse(emptyList(), packageName = "com.google.android.gms")
            setupUpdateInfoServiceBinding()

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get a result from that provider
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_sendsPackageNameInIntentData() {
        runBlocking {
            // GIVEN the client app has a specific package name
            val clientPackageName = "com.example.myclient"
            `when`(mockContext.packageName).thenReturn(clientPackageName)

            // AND a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            setupUpdateInfoServiceResponse(emptyList(), "com.google.android.gms")

            // CAPTURE the Intent passed to bindService
            val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
            `when`(
                    mockContext.bindService(
                        intentCaptor.capture(),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenReturn(true)

            // WHEN we query for all available updates
            securityState.queryAllAvailableUpdates()

            // THEN the Intent contains the correct Data URI
            val capturedIntent = intentCaptor.value
            val data = capturedIntent.data

            assertNotNull("Intent data should not be null", data)
            assertEquals("scheme should be 'package'", "package", data?.scheme)
            assertEquals(
                "schemeSpecificPart should be package name",
                clientPackageName,
                data?.schemeSpecificPart,
            )
        }
    }

    // --------------------------------------------------------------------------------------------
    // Phase 2: Success Scenarios
    // --------------------------------------------------------------------------------------------

    @Test
    fun testQueryAllAvailableUpdates_returnsMetadataOnly_whenSystemUpToDate() {
        runBlocking {
            // GIVEN a trusted provider reports "System Up To Date" (empty list)
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            setupUpdateInfoServiceBinding()

            // BUT it includes a valid timestamp indicating a successful check
            val checkTime = 123456789L
            val result = UpdateCheckResult("com.google.android.gms", emptyList(), checkTime)
            setupUpdateInfoServiceResponse(result)

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN the updates list is empty, but the timestamp and provider are preserved
            assertEquals(1, results.size)
            assertTrue(results[0].updates.isEmpty())
            assertEquals(checkTime, results[0].lastCheckTimeMillis)
            assertEquals("com.google.android.gms", results[0].providerPackageName)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_returnsDataFromProvider() {
        runBlocking {
            // GIVEN a trusted provider returns specific updates
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            val expectedUpdates =
                listOf(
                    UpdateInfo.Builder()
                        .setComponent("SYSTEM")
                        .setSecurityPatchLevel("2025-01-01")
                        .build()
                )
            val expectedTime = 123456789L

            val mockResult =
                UpdateCheckResult("com.google.android.gms", expectedUpdates, expectedTime)
            setupUpdateInfoServiceResponse(mockResult)
            setupUpdateInfoServiceBinding()

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN the data is correctly marshalled back
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertEquals(expectedUpdates, results[0].updates)
            assertEquals(expectedTime, results[0].lastCheckTimeMillis)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_aggregatesMultipleProviders() {
        runBlocking {
            // GIVEN two trusted providers
            val gotaInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            val playInfo =
                createUpdateInfoServiceResolveInfo("com.android.vending", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(gotaInfo, playInfo))

            setupUpdateInfoServiceBinding()

            // Both return empty lists for this basic aggregation test
            setupUpdateInfoServiceResponse(emptyList())

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get two results (one from each provider)
            assertEquals(2, results.size)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound(times = 2)
        }
    }

    // --------------------------------------------------------------------------------------------
    // Phase 3: Connection Failures (Bind Phase)
    // --------------------------------------------------------------------------------------------

    @Test
    fun testQueryAllAvailableUpdates_handlesBindFailureGracefully() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // BUT binding fails (returns false)
            `when`(mockContext.bindService(any(), any(), anyInt())).thenReturn(false)

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get an empty result container (not a crash)
            // The implementation returns an empty result for failed providers so the app knows it
            // checked.
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertTrue(results[0].updates.isEmpty())
            assertEquals(0L, results[0].lastCheckTimeMillis)
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_handlesSecurityExceptionOnBind() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // BUT bindService throws a SecurityException (e.g., permission denied)
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenThrow(SecurityException("Permission denied"))

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get a clean empty result (Graceful Failure)
            assertEquals(1, results.size)
            assertTrue(results[0].updates.isEmpty())
            // The provider package name is preserved so the client knows who failed
            assertEquals("com.google.android.gms", results[0].providerPackageName)
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_handlesPartialBindFailure() {
        runBlocking {
            // GIVEN two trusted providers
            val workingInfo = createUpdateInfoServiceResolveInfo("com.working", isSystem = true)
            val brokenInfo = createUpdateInfoServiceResolveInfo("com.broken", isSystem = true)

            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(workingInfo, brokenInfo))

            // SETUP: bindService succeeds for "com.working" but fails for "com.broken"
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenAnswer { invocation ->
                    val intent = invocation.getArgument<Intent>(0)
                    val connection = invocation.getArgument<ServiceConnection>(1)

                    if (intent.component?.packageName == "com.working") {
                        // Simulate success
                        connection.onServiceConnected(intent.component, mockBinder)
                        true
                    } else {
                        // Simulate failure (refused to bind)
                        false
                    }
                }

            // Mock a valid response for the working service
            val updates = listOf(UpdateInfo.Builder().setComponent("SYSTEM").build())
            val validResult = UpdateCheckResult("com.working", updates, 0L)
            `when`(mockService.listAvailableUpdates()).thenReturn(validResult)

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get results for BOTH (one with data, one empty/failed)
            assertEquals(2, results.size)

            val workingResult = results.find { it.providerPackageName == "com.working" }
            val brokenResult = results.find { it.providerPackageName == "com.broken" }

            assertNotNull(workingResult)
            assertEquals(1, workingResult?.updates?.size)

            assertNotNull(brokenResult)
            assertTrue(brokenResult!!.updates.isEmpty()) // Fallback to empty
        }
    }

    // --------------------------------------------------------------------------------------------
    // Phase 4: Runtime Failures (Execution Phase)
    // --------------------------------------------------------------------------------------------

    @Test
    fun testQueryAllAvailableUpdates_handlesServiceDisconnectGracefully() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // BUT the service disconnects during the call
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenAnswer { invocation ->
                    // Simulate onServiceDisconnected being called synchronously
                    val connection = invocation.getArgument<ServiceConnection>(1)
                    val component = ComponentName("com.google.android.gms", "MockUpdateInfoService")
                    connection.onServiceDisconnected(component)
                    true // bindService itself returns true
                }

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get a clean empty result, not a crash
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertTrue(results[0].updates.isEmpty())
            assertEquals(0L, results[0].lastCheckTimeMillis)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_handlesRemoteExceptionGracefully() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // AND the service will throw a RemoteException during IPC
            `when`(mockService.listAvailableUpdates()).thenThrow(RemoteException("Test Exception"))
            setupUpdateInfoServiceBinding()

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get a clean empty result
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertTrue(results[0].updates.isEmpty())
            assertEquals(0L, results[0].lastCheckTimeMillis)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_handlesGenericExceptionGracefully() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // AND the remote service throws an unexpected RuntimeException during the IPC call.
            // This simulates a crash or logic error within the background thread execution.
            `when`(mockService.listAvailableUpdates())
                .thenThrow(RuntimeException("Unexpected Crash"))

            // Trigger the service connection. This simulates the immediate success of bindService,
            // which in turn spawns the background thread to perform the IPC call.
            setupUpdateInfoServiceBinding()

            // WHEN we query for all available updates
            val results = securityState.queryAllAvailableUpdates()

            // THEN we get a clean empty result
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertTrue(results[0].updates.isEmpty())
            assertEquals(0L, results[0].lastCheckTimeMillis)

            // AND verify that unbindService was still called to prevent leaking the connection.
            // We use the helper to handle the race condition where the background thread's
            // 'finally' block might execute slightly after the coroutine resumes.
            verifyUpdateInfoServiceUnbound()
        }
    }

    // --------------------------------------------------------------------------------------------
    // Phase 5: Safety & Lifecycle (Timeouts & Cancellation)
    // -------------------------------------------------------------------------------------------

    @Test
    fun testQueryAllAvailableUpdates_serviceBindingTimesOut_returnsEmptyResult() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // AND bindService returns true (system accepted request)
            // BUT we do NOT call onServiceConnected to simulate a hang
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenReturn(true)

            // WHEN we query for updates with a custom short timeout (100ms) )
            val results = securityState.queryAllAvailableUpdates(timeoutMillis = 100L)

            // THEN the result should be empty (graceful failure)
            assertEquals(1, results.size)
            assertEquals("com.google.android.gms", results[0].providerPackageName)
            assertTrue(results[0].updates.isEmpty())
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_onTimeout_unbindsService() {
        runBlocking {
            // GIVEN a trusted provider and a hanging service
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // SETUP: bindService returns true, but we never trigger a callback
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        any(ServiceConnection::class.java),
                        anyInt(),
                    )
                )
                .thenReturn(true)

            // WHEN the query times out (waits only 100ms)
            securityState.queryAllAvailableUpdates(timeoutMillis = 100L)

            // AND verify we unbound from the service
            verifyUpdateInfoServiceUnbound()
        }
    }

    @Test
    fun testQueryAllAvailableUpdates_onCancellation_unbindsService() {
        runBlocking {
            // GIVEN a trusted provider exists
            val trustedInfo =
                createUpdateInfoServiceResolveInfo("com.google.android.gms", isSystem = true)
            `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
                .thenReturn(listOf(trustedInfo))

            // Capture the connection to verify it is the exact instance unbound later
            val connectionCaptor = ArgumentCaptor.forClass(ServiceConnection::class.java)

            // SETUP: bindService returns true (request accepted), but we DO NOT trigger
            // onServiceConnected. This simulates the state where the coroutine is suspended
            // indefinitely, waiting for the service to attach.
            `when`(
                    mockContext.bindService(
                        any(Intent::class.java),
                        connectionCaptor.capture(),
                        anyInt(),
                    )
                )
                .thenReturn(true)

            // WHEN the query is launched in a separate coroutine
            val job = launch { securityState.queryAllAvailableUpdates() }

            // CRITICAL: Yield the main test thread to allow the launched coroutine to start
            // executing. Without this, the coroutine would sit in the queue while 'verify'
            // blocked the thread, causing a deadlock.
            yield()

            // SYNCHRONIZATION: Wait up to 2 seconds for bindService to be called.
            // We use 'timeout' because the operation happens on a background thread
            // (Dispatchers.IO).
            // This acts as a latch: it proceeds immediately once the call is observed,
            // ensuring the coroutine has reached the "bound" state before we attempt cancellation.
            verify(mockContext, timeout(2000))
                .bindService(any(Intent::class.java), any(ServiceConnection::class.java), anyInt())

            // NOW cancel the job while it is suspended waiting for the service connection.
            job.cancel()
            job.join() // Wait for the cancellation to fully process and cleanup to run

            // THEN unbindService must be called to prevent leaking the connection.
            // This verifies that the 'invokeOnCancellation' block in the implementation
            // correctly cleans up resources even if the operation never completed.
            verifyUpdateInfoServiceUnbound()
        }
    }

    /** Helper to mock the current Device Security Patch Level. */
    private fun mockDeviceSpl(component: String, spl: String) {
        val bundle = Bundle()
        // Map the component to the bundle key used by SecurityPatchState
        val key =
            when (component) {
                SecurityPatchState.COMPONENT_SYSTEM -> "system_spl"
                SecurityPatchState.COMPONENT_VENDOR -> "vendor_spl"
                SecurityPatchState.COMPONENT_KERNEL -> "kernel_version"
                else ->
                    throw IllegalArgumentException(
                        "mockDeviceSpl only supports global components (SYSTEM, VENDOR, KERNEL). " +
                            "For SYSTEM_MODULES, mock package versions directly."
                    )
            }
        bundle.putString(key, spl)

        `when`(mockSecurityStateManagerCompat.getGlobalSecurityState(anyString()))
            .thenReturn(bundle)
    }

    /**
     * Helper to setup a complete "Happy Path" scenario where a single trusted [IUpdateInfoService]
     * is discovered, bound, and configured to return the specified updates.
     *
     * This helper accepts a variable number of arguments, so it can be called with one update or
     * multiple updates.
     */
    private fun setupTrustedUpdateInfoServiceWithUpdates(vararg updates: UpdateInfo) {
        val info =
            createUpdateInfoServiceResolveInfo(
                packageName = "com.google.android.gms",
                isSystem = true,
            )

        `when`(mockPackageManager.queryIntentServices(any(Intent::class.java), anyInt()))
            .thenReturn(listOf(info))

        setupUpdateInfoServiceBinding()

        val result =
            UpdateCheckResult(
                providerPackageName = "com.google.android.gms",
                updates = updates.toList(),
                lastCheckTimeMillis = 1000L,
            )
        setupUpdateInfoServiceResponse(result)
    }

    /** Creates a [ResolveInfo] object that mimics a discovered UpdateInfoService. */
    private fun createUpdateInfoServiceResolveInfo(
        packageName: String,
        isSystem: Boolean,
    ): ResolveInfo {
        val info = ResolveInfo()
        info.serviceInfo = ServiceInfo()
        info.serviceInfo.packageName = packageName
        info.serviceInfo.name = "MockUpdateInfoService"
        info.serviceInfo.applicationInfo = ApplicationInfo()

        if (isSystem) {
            info.serviceInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM
        }
        return info
    }

    /** Configures the mock [IUpdateInfoService] to return a pre-constructed result object. */
    private fun setupUpdateInfoServiceResponse(result: UpdateCheckResult) {
        `when`(mockService.listAvailableUpdates()).thenReturn(result)
    }

    /** Configures the mock [IUpdateInfoService] to return a result based on a list of updates. */
    private fun setupUpdateInfoServiceResponse(
        updates: List<UpdateInfo>,
        packageName: String = "com.test",
    ) {
        val result = UpdateCheckResult(packageName, updates, 0L)
        setupUpdateInfoServiceResponse(result)
    }

    /** Mocks a successful [Context.bindService] call that connects to the [IUpdateInfoService]. */
    private fun setupUpdateInfoServiceBinding() {
        `when`(
                mockContext.bindService(
                    any(Intent::class.java),
                    any(ServiceConnection::class.java),
                    anyInt(),
                )
            )
            .thenAnswer { invocation ->
                val intent = invocation.getArgument<Intent>(0)
                val connection = invocation.getArgument<ServiceConnection>(1)
                val targetComponent =
                    intent.component ?: ComponentName("com.test", "MockUpdateInfoService")
                connection.onServiceConnected(targetComponent, mockBinder)
                true
            }
    }

    /**
     * Helper to verify that the mock [IUpdateInfoService] was unbound.
     *
     * Uses a timeout to handle cases where unbinding happens on a background thread (which
     * introduces a race condition) or during cancellation cleanup.
     */
    private fun verifyUpdateInfoServiceUnbound(times: Int = 1) {
        verify(mockContext, timeout(2000).times(times))
            .unbindService(any(ServiceConnection::class.java))
    }
}
