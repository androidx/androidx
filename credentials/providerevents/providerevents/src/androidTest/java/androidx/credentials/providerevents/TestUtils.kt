/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.providerevents

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.SignalCredentialStateRequest
import androidx.credentials.provider.CallingAppInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert

@RequiresApi(28)
fun assertEquals(actual: SignalCredentialStateRequest, expected: SignalCredentialStateRequest) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertThat(actual.requestJson).isEqualTo(expected.requestJson)
    assertThat(actual.origin).isEqualTo(expected.origin)
}

@Suppress("DEPRECATION")
fun getTestCallingAppInfo(origin: String?): CallingAppInfo {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageName = context.packageName
    if (Build.VERSION.SDK_INT >= 28) {
        val packageInfo =
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
        Assert.assertNotNull(packageInfo.signingInfo)
        return CallingAppInfo(packageName, packageInfo.signingInfo!!, origin)
    } else {
        val packageInfo =
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        return CallingAppInfo(packageName, packageInfo.signatures!!.filterNotNull(), origin)
    }
}
