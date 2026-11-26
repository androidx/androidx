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

package androidx.compose.ui.text.googlefonts

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import androidx.compose.ui.text.googlefonts.test.R
import androidx.core.content.res.FontResourcesParserCompat
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontFamilyResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
@SmallTest
class FontProviderHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resources = context.resources

    private val prodCertBytes: List<ByteArray> by lazy {
        FontResourcesParserCompat.readCerts(
                resources,
                R.array.com_google_android_gms_fonts_certs_prod,
            )
            .single()
    }

    private val devCertBytes: List<ByteArray> by lazy {
        FontResourcesParserCompat.readCerts(
                resources,
                R.array.com_google_android_gms_fonts_certs_dev,
            )
            .single()
    }

    @Test
    fun checkAvailable_prodCerts_nonSystemApp_returnsTrue() {
        val pm = createMockPackageManager(appFlags = 0)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> prodCertBytes.toSignatures() })
            .isTrue()
    }

    @Test
    fun checkAvailable_systemCerts_systemApp_returnsTrue() {
        val pm = createMockPackageManager(appFlags = ApplicationInfo.FLAG_SYSTEM)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() })
            .isTrue()
    }

    @Test
    fun checkAvailable_systemCerts_updatedSystemApp_returnsTrue() {
        val pm = createMockPackageManager(appFlags = ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() })
            .isTrue()
    }

    @Test
    fun checkAvailable_systemCerts_nonSystemApp_throwsIllegalStateException() {
        val pm = createMockPackageManager(appFlags = 0)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        val ex =
            assertThrows(IllegalStateException::class.java) {
                provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() }
            }
        assertThat(ex).hasMessageThat().contains("Provided signatures did not match")
    }

    @Test
    fun checkAvailable_systemCertsOnly_systemApp_returnsTrue() {
        val pm = createMockPackageManager(appFlags = ApplicationInfo.FLAG_SYSTEM)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = 0,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() })
            .isTrue()
    }

    @Test
    fun checkAvailable_systemCertsOnly_nonSystemApp_throwsIllegalStateException() {
        val pm = createMockPackageManager(appFlags = 0)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = 0,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThrows(IllegalStateException::class.java) {
            provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() }
        }
    }

    @Test
    fun checkAvailable_nullProviderAppInfo_throwsIllegalStateException() {
        val pm =
            createMockPackageManager(
                appFlags = ApplicationInfo.FLAG_SYSTEM,
                nullProviderAppInfo = true,
            )
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = 0,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThrows(IllegalStateException::class.java) {
            provider.checkAvailable(pm, resources) { _, _ -> devCertBytes.toSignatures() }
        }
    }

    @Test
    fun checkAvailable_emptySignaturesWithEmptyCertSet_throwsInsteadOfMatching() {
        val pm = createMockPackageManager(appFlags = ApplicationInfo.FLAG_SYSTEM)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = listOf(emptyList()),
            )

        val ex =
            assertThrows(IllegalStateException::class.java) {
                provider.checkAvailable(pm, resources) { _, _ -> emptyList() }
            }
        assertThat(ex).hasMessageThat().contains("emptyList()")
    }

    @Test
    fun checkAvailable_multiCertSet_reversedSignatureOrder_matches() {
        val certA = byteArrayOf(1, 2, 3)
        val certB = byteArrayOf(4, 5, 6, 7)
        val pm = createMockPackageManager(appFlags = 0)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = listOf(listOf(certA, certB)),
            )

        assertThat(
                provider.checkAvailable(pm, resources) { _, _ ->
                    listOf(certB, certA).toSignatures()
                }
            )
            .isTrue()
    }

    @Test
    fun checkAvailable_packageRemovedBeforeGetSignatures_returnsFalse() {
        val pm = createMockPackageManager(appFlags = 0)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(
                provider.checkAvailable(pm, resources) { _, _ ->
                    throw PackageManager.NameNotFoundException()
                }
            )
            .isFalse()
    }

    @Test
    fun checkAvailable_providerMissing_returnsFalse() {
        val pm = mock<PackageManager> { on { resolveContentProvider(AUTHORITY, 0) } doReturn null }
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> prodCertBytes.toSignatures() })
            .isFalse()
    }

    @Test
    fun checkAvailable_wrongPackage_returnsFalse() {
        val pm =
            createMockPackageManager(
                appFlags = ApplicationInfo.FLAG_SYSTEM,
                actualPackage = "com.example.spoof",
            )
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        assertThat(provider.checkAvailable(pm, resources) { _, _ -> prodCertBytes.toSignatures() })
            .isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 35)
    fun isAvailableOnDevice_devSystemCertOnNonSystemApp_throwsIllegalStateException() {
        val devSignatures = devCertBytes.map { Signature(it) }
        val signingInfo =
            SigningInfo(SigningInfo.VERSION_SIGNING_BLOCK_V3, devSignatures, null, devSignatures)
        val appInfo = ApplicationInfo().apply { flags = 0 }
        val providerInfo =
            ProviderInfo().apply {
                authority = AUTHORITY
                packageName = PACKAGE
                applicationInfo = appInfo
            }
        val packageInfo =
            PackageInfo().apply {
                packageName = PACKAGE
                applicationInfo = appInfo
                this.signingInfo = signingInfo
            }
        val pm =
            mock<PackageManager> {
                on { resolveContentProvider(AUTHORITY, 0) } doReturn providerInfo
                on { getPackageInfo(eq(PACKAGE), any<Int>()) } doReturn packageInfo
            }
        val fakeContext = contextWithPackageManager(pm)
        val provider =
            GoogleFont.Provider(
                providerAuthority = AUTHORITY,
                providerPackage = PACKAGE,
                certificates = R.array.com_google_android_gms_fonts_certs_prod,
                systemCertificates = R.array.com_google_android_gms_fonts_certs_dev,
            )

        val ex =
            assertThrows(IllegalStateException::class.java) {
                provider.isAvailableOnDevice(fakeContext)
            }
        assertThat(ex).hasMessageThat().contains("Provided signatures did not match")
    }

    @Test
    fun fetchFonts_defaultFont_rejectsDevCertOnNonSystemProvider() {
        val appInfo = ApplicationInfo().apply { flags = 0 }
        val providerInfo =
            ProviderInfo().apply {
                authority = AUTHORITY
                packageName = PACKAGE
                applicationInfo = appInfo
            }
        val packageInfo =
            PackageInfo().apply {
                packageName = PACKAGE
                applicationInfo = appInfo
                @Suppress("DEPRECATION")
                signatures = devCertBytes.toSignatures().toTypedArray()
            }
        val pm =
            mock<PackageManager> {
                on { resolveContentProvider(AUTHORITY, 0) } doReturn providerInfo
                on { getPackageInfo(eq(PACKAGE), any<Int>()) } doReturn packageInfo
            }
        val fakeContext = contextWithPackageManager(pm)

        val request = (Font(GoogleFont("Roboto")) as GoogleFontImpl).toFontRequest(fakeContext)
        val result = FontsContractCompat.fetchFonts(fakeContext, null, request)

        assertThat(result.statusCode).isEqualTo(FontFamilyResult.STATUS_WRONG_CERTIFICATES)
    }

    private fun contextWithPackageManager(pm: PackageManager): Context =
        object : ContextWrapper(context) {
            override fun getPackageManager(): PackageManager = pm
        }

    private fun createMockPackageManager(
        appFlags: Int,
        nullProviderAppInfo: Boolean = false,
        actualPackage: String = PACKAGE,
    ): PackageManager {
        val appInfo = ApplicationInfo().apply { flags = appFlags }
        val providerInfo =
            ProviderInfo().apply {
                authority = AUTHORITY
                packageName = actualPackage
                applicationInfo = if (nullProviderAppInfo) null else appInfo
            }
        return mock<PackageManager> {
            on { resolveContentProvider(AUTHORITY, 0) } doReturn providerInfo
        }
    }

    private fun List<ByteArray>.toSignatures(): List<Signature> = map { bytes ->
        mock<Signature> { on { toByteArray() } doReturn bytes }
    }

    private companion object {
        const val AUTHORITY = "com.google.android.gms.fonts"
        const val PACKAGE = "com.google.android.gms"
    }
}
