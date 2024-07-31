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
import android.content.pm.SigningInfo
import androidx.core.os.BuildCompat
import androidx.credentials.provider.SigningInfoCompat.Companion.fromSigningInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
class SigningInfoCompatApi28Test {
    private var signingInfo: SigningInfo? = null

    @Before
    @Throws(PackageManager.NameNotFoundException::class)
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
        assertNotNull(packageInfo.signingInfo)
        signingInfo = packageInfo.signingInfo
    }

    @Test
    fun constructorAndGetters_success() {
        val signingInfoCompat = fromSigningInfo(signingInfo!!)

        assertThat(signingInfoCompat.signingCertificateHistory)
            .containsExactlyElementsIn(signingInfo!!.signingCertificateHistory)
            .inOrder()
        assertThat(signingInfoCompat.apkContentsSigners)
            .containsExactlyElementsIn(signingInfo!!.apkContentsSigners)
            .inOrder()
        if (BuildCompat.isAtLeastV()) {
            if (signingInfoCompat.publicKeys.isNotEmpty()) {
                assertThat(signingInfoCompat.publicKeys)
                    .containsExactlyElementsIn(signingInfo!!.publicKeys)
                    .inOrder()
            }
            assertThat(signingInfoCompat.schemeVersion).isEqualTo(signingInfo!!.schemeVersion)
        }
        assertThat(signingInfoCompat.hasPastSigningCertificates)
            .isEqualTo(signingInfo!!.hasPastSigningCertificates())
        assertThat(signingInfoCompat.hasMultipleSigners)
            .isEqualTo(signingInfo!!.hasMultipleSigners())
    }
}
