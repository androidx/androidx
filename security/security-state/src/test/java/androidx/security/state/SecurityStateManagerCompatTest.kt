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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_SYSTEM_SUPPLEMENTAL_PATCHES
import androidx.security.state.SecurityStateManagerCompat.Companion.KEY_VENDOR_SUPPLEMENTAL_PATCHES
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
class SecurityStateManagerCompatTest {

    private val packageManager: PackageManager = mock<PackageManager>()
    private val context: Context = mock<Context>() { on { packageManager } doReturn packageManager }
    private lateinit var securityStateManagerCompat: SecurityStateManagerCompat
    private val SYSTEM_FILE_CONTENT =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <security-patches xmlns="http://schemas.android.com/security/patches/1.0">
            <patch>
                <id>CVE-2022-00000</id>
            </patch>
            <patch>
                <id>CVE-2023-11111</id>
            </patch>
        </security-patches>
        """
            .trimIndent()
    private val VENDOR_FILE_CONTENT =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <security-patches xmlns="http://schemas.android.com/security/patches/1.0">
            <patch>
                <id>CVE-2024-12345</id>
            </patch>
            <patch>
                <id>CVE-2025-54321</id>
            </patch>
        </security-patches>
        """
            .trimIndent()
    private val EMPTY_FILE_CONTENT =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <security-patches xmlns="http://schemas.android.com/security/patches/1.0">
        </security-patches>
        """
            .trimIndent()
    private lateinit var systemFile: File
    private lateinit var vendorFile: File
    private lateinit var emptyXmlFile: File
    private lateinit var malformedXmlFile: File
    private lateinit var testDir: File

    @Before
    fun setUp() {
        systemFile = File.createTempFile("test_system_supplemental_security_patches", ".xml")
        vendorFile = File.createTempFile("test_vendor_supplemental_security_patches", ".xml")
        emptyXmlFile = File.createTempFile("test_empty_supplemental_security_patches", ".xml")
        malformedXmlFile =
            File.createTempFile("test_malformed_supplemental_security_patches", ".xml")
        testDir =
            File.createTempFile("test_dir", "").apply {
                delete()
                mkdir()
            }

        systemFile.writeText(SYSTEM_FILE_CONTENT)
        vendorFile.writeText(VENDOR_FILE_CONTENT)
        emptyXmlFile.writeText(EMPTY_FILE_CONTENT)
        malformedXmlFile.writeText("malformed xml")

        securityStateManagerCompat = SecurityStateManagerCompat(context)
    }

    @After
    fun tearDown() {
        systemFile.delete()
        vendorFile.delete()
        emptyXmlFile.delete()
        malformedXmlFile.delete()
        testDir.deleteRecursively()
    }

    @Config(minSdk = Build.VERSION_CODES.Q)
    @Test
    fun testGetGlobalSecurityState_withGoogleModules() {
        val expectedBundle = Bundle()
        expectedBundle.putString("com.google.android.modulemetadata", "")
        expectedBundle.putString("kernel_version", "")

        Mockito.`when`(packageManager.getPackageInfo(Mockito.anyString(), Mockito.eq(0)))
            .thenReturn(PackageInfo().apply { versionName = "" })

        val result =
            securityStateManagerCompat.getGlobalSecurityState("com.google.android.modulemetadata")
        assertEquals(
            expectedBundle.getString("com.google.android.modulemetadata"),
            result.getString("com.google.android.modulemetadata"),
        )
    }

    @Test
    fun testGetPackageSpl_PackageNotFound() {
        Mockito.`when`(packageManager.getPackageInfo(Mockito.anyString(), Mockito.eq(0)))
            .thenThrow(PackageManager.NameNotFoundException())

        val result = securityStateManagerCompat.getPackageVersion("non.existent.package")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testGetKernelVersion() {
        // This method would normally require reading from the file system,
        // but we can mock this by pretending the expected output of the file read is known.
        val originalKernelVersionMethod =
            securityStateManagerCompat::class.java.getDeclaredMethod("getKernelVersion")
        originalKernelVersionMethod.isAccessible = true
        val kernelVersion = originalKernelVersionMethod.invoke(securityStateManagerCompat) as String
        assertNotNull(kernelVersion)
    }

    @Test
    fun testGetVendorSpl() {
        val originalVendorSplMethod =
            securityStateManagerCompat::class.java.getDeclaredMethod("getVendorSpl")
        originalVendorSplMethod.isAccessible = true
        val vendorSpl = originalVendorSplMethod.invoke(securityStateManagerCompat) as String
        assertNotNull(vendorSpl)
    }

    @Test
    fun testGetSecurityPatchLevelSafe_API_Level_Below_M() {
        val result = securityStateManagerCompat.getSecurityPatchLevelSafe()
        assertEquals("", result)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun testGetGlobalSecurityState_withSupplementalPatches_success() {
        securityStateManagerCompat =
            SecurityStateManagerCompat(context, systemFile.absolutePath, vendorFile.absolutePath)
        val result = securityStateManagerCompat.getGlobalSecurityState()

        val expectedBundle = Bundle()

        expectedBundle.putStringArray(
            KEY_SYSTEM_SUPPLEMENTAL_PATCHES,
            arrayOf("CVE-2022-00000", "CVE-2023-11111"),
        )
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
        )

        expectedBundle.putStringArray(
            KEY_VENDOR_SUPPLEMENTAL_PATCHES,
            arrayOf("CVE-2024-12345", "CVE-2025-54321"),
        )
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun testGetGlobalSecurityState_withSupplementalPatches_missingFiles() {
        securityStateManagerCompat =
            SecurityStateManagerCompat(
                context,
                "nonexistent_system_file.xml",
                "nonexistent_vendor_file.xml",
            )

        val result = securityStateManagerCompat.getGlobalSecurityState()

        // Verify that our manual file loading logic was not triggered by checking
        // that the result bundle contains empty supplemental patch key.
        val expectedBundle = Bundle()

        expectedBundle.putStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES, emptyArray<String>())
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
        )

        expectedBundle.putStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES, emptyArray<String>())
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun testGetGlobalSecurityState_withSupplementalPatches_emptyXml() {
        securityStateManagerCompat =
            SecurityStateManagerCompat(
                context,
                emptyXmlFile.absolutePath,
                emptyXmlFile.absolutePath,
            )

        val result = securityStateManagerCompat.getGlobalSecurityState()

        val expectedBundle = Bundle()

        expectedBundle.putStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES, emptyArray<String>())
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_SYSTEM_SUPPLEMENTAL_PATCHES),
        )

        expectedBundle.putStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES, emptyArray<String>())
        assertArrayEquals(
            expectedBundle.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
            result.getStringArray(KEY_VENDOR_SUPPLEMENTAL_PATCHES),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.CUR_DEVELOPMENT])
    fun testGetGlobalSecurityState_withSupplementalPatches_skipOnSdk36AndAbove() {
        // On SDK > 36, the SecurityStateManager service is used.
        // We verify that our compat code path for manual file loading is skipped.
        securityStateManagerCompat =
            SecurityStateManagerCompat(context, systemFile.absolutePath, vendorFile.absolutePath)

        val result = securityStateManagerCompat.getGlobalSecurityState()

        // Verify that our manual file loading logic was not triggered by checking
        // that the result bundle does not contain the supplemental patch keys.
        assertFalse(result.containsKey(KEY_SYSTEM_SUPPLEMENTAL_PATCHES))
        assertFalse(result.containsKey(KEY_VENDOR_SUPPLEMENTAL_PATCHES))
    }

    @Test
    fun testLoadSupplementalPatchesFromFile_handlesExceptions() {
        // IOException should be caught and handled gracefully (e.g., trying to read a directory).
        val resultFromDir =
            securityStateManagerCompat.loadSupplementalPatchesFromFile(testDir.absolutePath)
        assertArrayEquals(
            "Should return empty array when an IOException occurs.",
            emptyArray<String>(),
            resultFromDir,
        )

        // A generic Exception should be caught (e.g., malformed XML file).
        val resultFromMalformedFile =
            securityStateManagerCompat.loadSupplementalPatchesFromFile(
                malformedXmlFile.absolutePath
            )
        assertArrayEquals(
            "Should return empty array for a malformed XML file",
            emptyArray<String>(),
            resultFromMalformedFile,
        )
    }
}
