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

package androidx.glance.wear.core

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RendererVersionTest {

    @Test
    fun fromPlHostPackage_parsesValidVersionName() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo().apply { versionName = "1.6.0.12" }
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfo)

        val version = RendererVersion.fromPlHostPackage(mockContext)

        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(6)
        assertThat(version.revision).isEqualTo(0)
    }

    @Test
    fun fromPlHostPackage_parsesIncrementedVersionName() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo().apply { versionName = "1.6.1.20" }
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfo)

        val version = RendererVersion.fromPlHostPackage(mockContext)

        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(6)
        assertThat(version.revision).isEqualTo(1)
    }

    @Test
    fun fromPlHostPackage_parsesTwoPartVersionName() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo().apply { versionName = "1.600" }
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfo)

        val version = RendererVersion.fromPlHostPackage(mockContext)

        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(600)
        assertThat(version.revision).isEqualTo(0)
    }

    @Test
    fun fromPlHostPackage_parsesTwoPartShortVersionName() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo().apply { versionName = "1.6" }
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfo)

        val version = RendererVersion.fromPlHostPackage(mockContext)

        assertThat(version.major).isEqualTo(1)
        assertThat(version.minor).isEqualTo(6)
        assertThat(version.revision).isEqualTo(0)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenNameNotFound() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenThrow(PackageManager.NameNotFoundException())

        val version = RendererVersion.fromPlHostPackage(mockContext)

        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenVersionName_isEmpty() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfoEmpty = PackageInfo().apply { versionName = "" }
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfoEmpty)
        val version = RendererVersion.fromPlHostPackage(mockContext)
        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenVersionName_isMalformed() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfoMalformed = PackageInfo().apply { versionName = "1.abc" }
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfoMalformed)
        val version = RendererVersion.fromPlHostPackage(mockContext)
        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenVersionName_isSingleNumber() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfoSingle = PackageInfo().apply { versionName = "1" }
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfoSingle)
        val version = RendererVersion.fromPlHostPackage(mockContext)
        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenInvalidMajorVersion() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfoInvalidMajor = PackageInfo().apply { versionName = "0.6.3" }
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfoInvalidMajor)
        val version = RendererVersion.fromPlHostPackage(mockContext)
        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_fallbackWhenInvalidMinorVersion() {
        val mockContext = mock(Context::class.java)
        val mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        val packageInfoInvalidMinor = PackageInfo().apply { versionName = "1.-1.0" }
        `when`(
                mockPackageManager.getPackageInfo(
                    eq(RendererVersion.PL_RENDERER_HOST_PACKAGE),
                    anyInt(),
                )
            )
            .thenReturn(packageInfoInvalidMinor)
        val version = RendererVersion.fromPlHostPackage(mockContext)
        assertThat(version).isEqualTo(RendererVersion.PL_RENDERER_INITIAL_VERSION)
    }

    @Test
    fun fromPlHostPackage_defaultVersion() {
        val defaultVersion = RendererVersion()
        assertThat(defaultVersion.major).isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_MAJOR)
        assertThat(defaultVersion.minor).isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_MINOR)
        assertThat(defaultVersion.revision)
            .isEqualTo(RendererVersion.DEFAULT_RENDERER_VERSION_REVISION)
    }

    @Test
    fun rendererVersion_comparisonIsCorrect() {
        val v1 = RendererVersion(1, 6, 0)
        val v2 = RendererVersion(1, 6, 1)
        val v3 = RendererVersion(2, 0, 0)

        assertThat(v1).isLessThan(v2)
        assertThat(v2).isLessThan(v3)
        assertThat(v1).isLessThan(v3)
        assertThat(v2).isGreaterThan(v1)

        assertThat(RendererVersion(1, 6, 0)).isEqualTo(RendererVersion(1, 6, 0))
        assertThat(RendererVersion(1, 6, 0).hashCode())
            .isEqualTo(RendererVersion(1, 6, 0).hashCode())
    }
}
