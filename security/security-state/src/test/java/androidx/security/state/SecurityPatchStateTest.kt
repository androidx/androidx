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
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class SecurityPatchStateTest {

    @SuppressLint("NewApi")
    private val updateInfo =
        UpdateInfo.Builder()
            .setUri("content://example.com/updateinfo")
            .setComponent(SecurityPatchState.COMPONENT_SYSTEM.toString())
            .setSecurityPatchLevel("2022-01-01")
            .setPublishedDate(Date.from(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)))
            .build()
    private val mockEmptyEditor: SharedPreferences.Editor = mock<SharedPreferences.Editor> {}
    private val mockEditor: SharedPreferences.Editor =
        mock<SharedPreferences.Editor> {
            on { putString(Mockito.anyString(), Mockito.anyString()) } doReturn mockEmptyEditor
            on { remove(Mockito.anyString()) } doReturn mockEmptyEditor
        }
    private val mockPrefs: SharedPreferences =
        mock<SharedPreferences> {
            on { edit() } doReturn mockEditor
            on { all } doReturn mapOf(Pair("key", Gson().toJson(updateInfo)))
        }
    private val mockPackageManager: PackageManager =
        mock<PackageManager> {
            on { getPackageInfo(Mockito.anyString(), Mockito.eq(0)) } doReturn
                PackageInfo().apply { versionName = "" }
        }
    private val mockCursor: Cursor = mock<Cursor> {}
    private val mockCursorWithData: Cursor =
        mock<Cursor> {
            on { moveToNext() } doReturn true doReturn false doReturn true doReturn false
            on { getColumnIndexOrThrow(Mockito.eq("json")) } doReturn 123
            on { getString(Mockito.eq(123)) } doReturn Gson().toJson(updateInfo)
        }
    private val mockContentResolver: ContentResolver =
        mock<ContentResolver> {
            on {
                query(
                    Uri.parse("content://com.google.android.gms.apk/updateinfo"),
                    arrayOf("json"),
                    null,
                    null,
                    null
                )
            } doReturn mockCursor
            on {
                query(
                    Uri.parse("content://example.com/updateinfo"),
                    arrayOf("json"),
                    null,
                    null,
                    null
                )
            } doReturn mockCursorWithData
        }
    private val mockContext: Context =
        mock<Context> {
            on { getSharedPreferences("UpdateInfoPrefs", Context.MODE_PRIVATE) } doReturn mockPrefs
            on { packageManager } doReturn mockPackageManager
            on { contentResolver } doReturn mockContentResolver
        }
    private val mockSecurityStateManager: SecurityStateManager = mock<SecurityStateManager> {}
    private lateinit var securityState: SecurityPatchState

    @Before
    fun setup() {
        securityState = SecurityPatchState(mockContext, listOf(), mockSecurityStateManager)
    }

    @Test
    fun testGetSystemModules_whenSystemModulesIsEmpty_usesDefaultSystemModules() {
        assert(securityState.getSystemModules() == SecurityPatchState.DEFAULT_SYSTEM_MODULES)
    }

    @Test
    fun testGetSecurityPatchLevelWithDateBasedComponent() {
        val spl =
            securityState.getComponentSecurityPatchLevel(
                SecurityPatchState.COMPONENT_SYSTEM,
                "2022-01-01"
            )
        assertTrue(spl is SecurityPatchState.DateBasedSecurityPatchLevel)
        assertEquals("2022-01-01", spl.toString())
    }

    @Test
    fun testGetSecurityPatchLevelWithVersionedComponent() {
        val spl =
            securityState.getComponentSecurityPatchLevel(
                SecurityPatchState.COMPONENT_KERNEL,
                "1.2.3.4"
            )
        assertTrue(spl is SecurityPatchState.VersionedSecurityPatchLevel)
        assertEquals("1.2.3.4", spl.toString())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetSecurityPatchLevelWithInvalidDateBasedInput() {
        securityState.getComponentSecurityPatchLevel(
            SecurityPatchState.COMPONENT_SYSTEM,
            "invalid-date"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetSecurityPatchLevelWithInvalidVersionedInput() {
        securityState.getComponentSecurityPatchLevel(
            SecurityPatchState.COMPONENT_KERNEL,
            "invalid-version"
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Config(maxSdk = Build.VERSION_CODES.N_MR1)
    @Test(expected = IllegalArgumentException::class)
    fun testGetVulnerabilityReportUrl_withUnsupportedSdk_throwsException() {
        securityState.getVulnerabilityReportUrl(Uri.parse("https://example.com"))
    }

    @Test
    fun testParseVulnerabilityReport_validJson_returnsCorrectData() {
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

        val fixes =
            securityState.getPatchedCves(
                SecurityPatchState.COMPONENT_SYSTEM,
                SecurityPatchState.DateBasedSecurityPatchLevel(2022, 1, 1)
            )

        assertEquals(1, fixes[SecurityPatchState.Severity.HIGH]?.size)
        assertEquals(1, fixes[SecurityPatchState.Severity.MODERATE]?.size)
        assertEquals(setOf("CVE-2020-1234"), fixes[SecurityPatchState.Severity.HIGH])
        assertEquals(setOf("CVE-2020-5678"), fixes[SecurityPatchState.Severity.MODERATE])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseVulnerabilityReport_invalidAsb_throwsIllegalArgumentException() {
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
    fun testParseVulnerabilityReport_invalidJson_throwsIllegalArgumentException() {
        val invalidJson = "{ invalid json }"
        securityState.loadVulnerabilityReport(invalidJson)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testGetVulnerabilityReportUrl_validSdkVersion_returnsCorrectUrl() {
        val sdkVersion = 34 // Android 14
        val baseUrl = SecurityPatchState.DEFAULT_VULNERABILITY_REPORTS_URL
        val expectedUrl = "$baseUrl/v1/android_sdk_34.json"

        doReturn(sdkVersion).`when`(mockSecurityStateManager).getAndroidSdkInt()

        val actualUrl = securityState.getVulnerabilityReportUrl(Uri.parse(baseUrl)).toString()
        assertEquals(expectedUrl, actualUrl)
    }

    @Test
    fun testGetDeviceSpl_validComponent_returnsCorrectSpl() {
        val systemSpl = "2020-01-01"
        val bundle = Bundle()
        bundle.putString("system_spl", systemSpl)

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)

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
        doReturn("").`when`(mockSecurityStateManager).getPackageVersion(Mockito.anyString())
        doReturn(bundle).`when`(mockSecurityStateManager).getGlobalSecurityState(anyString())

        securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetPublishedSpl_ThrowsWhenNoVulnerabilityReportLoaded() {
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

        `when`(mockSecurityStateManager.getPackageVersion("com.google.android.modulemetadata"))
            .thenReturn("2022-01-01")
        `when`(mockSecurityStateManager.getPackageVersion("com.google.mainline.telemetry"))
            .thenReturn("2023-05-01")
        `when`(mockSecurityStateManager.getPackageVersion("com.google.mainline.adservices"))
            .thenReturn("2022-05-01")
        `when`(mockSecurityStateManager.getPackageVersion("com.google.mainline.go.primary"))
            .thenReturn("2021-05-01")
        `when`(mockSecurityStateManager.getPackageVersion("com.google.mainline.go.telemetry"))
            .thenReturn("2024-05-01")

        val spl =
            securityState.getDeviceSecurityPatchLevel(SecurityPatchState.COMPONENT_SYSTEM_MODULES)
                as SecurityPatchState.DateBasedSecurityPatchLevel

        assertEquals(2022, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsCorrectSplForSystemModules() {
        val jsonInput =
            """
            {
                "vulnerabilities": {
                    "2023-01-01": [{
                        "cve_identifiers": ["CVE-1234-4321"],
                        "asb_identifiers": ["ASB-A-2023111"],
                        "severity": "high",
                        "components": ["com.google.android.modulemetadata"]
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

        assertEquals(2023, spl.getYear())
        assertEquals(1, spl.getMonth())
        assertEquals(1, spl.getDay())
    }

    @Test
    fun testGetPublishedSpl_ReturnsCorrectSplForVendor() {
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

    @Test
    fun testGetPublishedKernelVersions_ReturnsCorrectVersions() {
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
    }

    @Test
    fun testGetAvailableSpl_ReturnsUpdateWhenHigherThanCurrent() {
        val availableSpl = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 2, 1)
        val component = SecurityPatchState.COMPONENT_SYSTEM
        val availableUpdateJson =
            """
            {
                "uri": "content://example.com/updateinfo",
                "component": "SYSTEM",
                "securityPatchLevel": "2023-02-01",
                "publishedDate": "2023-02-01T00:00:00Z"
            }
            """
                .trimIndent()
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-01-01")

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        `when`(mockCursor.moveToNext()).thenReturn(true).thenReturn(false)
        `when`(mockCursor.getColumnIndexOrThrow("json")).thenReturn(0)
        `when`(mockCursor.getString(0)).thenReturn(availableUpdateJson)
        securityState.loadVulnerabilityReport(generateMockReport("system", "2023-02-01"))

        val result = securityState.getAvailableSecurityPatchLevel(component)

        assertEquals(availableSpl.toString(), result.toString())
    }

    @Test
    fun testGetAvailableSpl_FallsBackToCurrentWhenNoHigherUpdate() {
        val currentSpl = SecurityPatchState.DateBasedSecurityPatchLevel(2023, 5, 15)
        val component = SecurityPatchState.COMPONENT_SYSTEM
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-05-15")

        `when`(mockCursor.moveToNext()).thenReturn(false)
        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        securityState.loadVulnerabilityReport(generateMockReport("system", "2023-04-01"))

        val result = securityState.getAvailableSecurityPatchLevel(component)

        assertEquals(currentSpl.toString(), result.toString())
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
    fun testGetSecurityFixes_ReturnsNoFixes() {
        securityState.loadVulnerabilityReport(generateMockReport("vendor", "2023-01-01"))

        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-01")
        val fixes = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM, spl)

        assertEquals(null, fixes[SecurityPatchState.Severity.CRITICAL])
        assertEquals(null, fixes[SecurityPatchState.Severity.HIGH])
        assertEquals(null, fixes[SecurityPatchState.Severity.MODERATE])
        assertEquals(null, fixes[SecurityPatchState.Severity.LOW])

        val spl2 = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2022-01-01")
        val fixes2 = securityState.getPatchedCves(SecurityPatchState.COMPONENT_VENDOR, spl2)

        assertEquals(null, fixes2[SecurityPatchState.Severity.CRITICAL])
        assertEquals(null, fixes2[SecurityPatchState.Severity.HIGH])
        assertEquals(null, fixes2[SecurityPatchState.Severity.MODERATE])
        assertEquals(null, fixes2[SecurityPatchState.Severity.LOW])
    }

    @Test
    fun testGetSecurityFixes_ReturnsCorrectFixesCategorizedBySeverity() {
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

        val fixes = securityState.getPatchedCves(SecurityPatchState.COMPONENT_SYSTEM, spl)

        assertEquals(2, fixes[SecurityPatchState.Severity.HIGH]?.size)
        assertEquals(
            setOf("CVE-2023-0001", "CVE-2023-0002"),
            fixes[SecurityPatchState.Severity.HIGH]
        )

        assertEquals(null, fixes[SecurityPatchState.Severity.MODERATE])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetSecurityFixes_ThrowsExceptionForInvalidComponent() {
        val spl = SecurityPatchState.DateBasedSecurityPatchLevel.fromString("2023-01-01")

        securityState.getPatchedCves(SecurityPatchState.COMPONENT_WEBVIEW, spl)
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
    fun testIsDeviceFullyUpdated_returnsTrue() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2023-01-01")
        bundle.putString("vendor_spl", "2023-02-01")
        bundle.putString("kernel_version", "5.4.123")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManager).getPackageVersion(anyString())

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
    fun testIsDeviceFullyUpdated_returnsFalse() {
        val bundle = Bundle()
        bundle.putString("system_spl", "2022-01-01")
        bundle.putString("com.google.android.modulemetadata", "2023-10-05")

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        doReturn("2023-10-05").`when`(mockSecurityStateManager).getPackageVersion(anyString())

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

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        doReturn(systemSpl).`when`(mockSecurityStateManager).getPackageVersion(Mockito.anyString())

        assertTrue(
            securityState.areCvesPatched(listOf("CVE-2023-0010", "CVE-2023-0001", "CVE-2023-0002"))
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

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        doReturn(systemSpl).`when`(mockSecurityStateManager).getPackageVersion(Mockito.anyString())

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

        `when`(mockSecurityStateManager.getGlobalSecurityState(anyString())).thenReturn(bundle)
        doReturn(systemSpl).`when`(mockSecurityStateManager).getPackageVersion(Mockito.anyString())

        assertFalse(
            securityState.areCvesPatched(listOf("CVE-2024-1010", "CVE-2023-0001", "CVE-2023-0002"))
        )
    }
}
