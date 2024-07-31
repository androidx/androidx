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

package androidx.credentials.provider

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import androidx.credentials.provider.SigningInfoCompat.Companion.fromSignatures
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SigningInfoCompatApi21Test {
    private lateinit var signatures: List<Signature>

    @Before
    @Suppress("DEPRECATION")
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
        assertNotNull(packageInfo.signatures)
        signatures = packageInfo.signatures!!.filterNotNull()
        assertThat(signatures).isNotEmpty()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 27)
    fun constructorAndGetters_belowApi28_success() {

        val signingInfoCompat = fromSignatures(signatures)

        assertThat(signingInfoCompat.signingCertificateHistory)
            .containsExactlyElementsIn(signatures)
            .inOrder()
        assertThat(signingInfoCompat.apkContentsSigners).isEmpty()
        assertThat(signingInfoCompat.hasPastSigningCertificates).isFalse()
        assertThat(signingInfoCompat.hasMultipleSigners).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun constructorAndGetters_aboveApi28_throws() {
        assertThrows(IllegalArgumentException::class.java) { fromSignatures(signatures) }
    }
}
