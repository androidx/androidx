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
import org.junit.Assert.assertEquals
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

    @Before
    fun setUp() {
        securityStateManagerCompat = SecurityStateManagerCompat(context)
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
}
