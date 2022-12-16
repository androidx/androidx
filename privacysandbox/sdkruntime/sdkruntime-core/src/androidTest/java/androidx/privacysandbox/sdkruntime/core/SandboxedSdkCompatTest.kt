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
package androidx.privacysandbox.sdkruntime.core

import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdk
import android.os.Binder
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SandboxedSdkCompatTest {

    @Test
    fun getInterface_returnsBinderPassedToCreate() {
        val binder = Binder()

        val sandboxedSdkCompat = SandboxedSdkCompat(binder)

        assertThat(sandboxedSdkCompat.getInterface())
            .isSameInstanceAs(binder)
    }

    @Test
    // TODO(b/249981547) Remove suppress after updating to new lint version (b/262251309)
    @SuppressLint("NewApi")
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @SdkSuppress(minSdkVersion = TIRAMISU)
    fun toSandboxedSdk_whenCreatedFromBinder_returnsSandboxedSdkWithSameBinder() {
        assumeTrue("Requires Sandbox API available", isSandboxApiAvailable())

        val binder = Binder()

        val toSandboxedSdkResult = SandboxedSdkCompat(binder).toSandboxedSdk()

        assertThat(toSandboxedSdkResult.getInterface())
            .isSameInstanceAs(binder)
    }

    @Test
    // TODO(b/249981547) Remove suppress after updating to new lint version (b/262251309)
    @SuppressLint("NewApi")
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @SdkSuppress(minSdkVersion = TIRAMISU)
    fun toSandboxedSdk_whenCreatedFromSandboxedSdk_returnsSameSandboxedSdk() {
        assumeTrue("Requires Sandbox API available", isSandboxApiAvailable())

        val binder = Binder()
        val sandboxedSdk = SandboxedSdk(binder)

        val toSandboxedSdkResult = SandboxedSdkCompat(sandboxedSdk).toSandboxedSdk()

        assertThat(toSandboxedSdkResult)
            .isSameInstanceAs(sandboxedSdk)
    }

    private fun isSandboxApiAvailable() =
        AdServicesInfo.version() >= 4
}