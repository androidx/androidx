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

import android.adservices.AdServicesVersion
import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdk
import android.os.Binder
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat.Companion.create
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat.Companion.toSandboxedSdkCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/249981547) Remove suppress when prebuilt with SdkSandbox APIs dropped to T
@SuppressLint("NewApi")
class SandboxedSdkCompatTest {

    @Test
    fun getInterface_returnsBinderPassedToCreate() {
        val binder = Binder()

        val sandboxedSdkCompat = create(binder)

        assertThat(sandboxedSdkCompat.getInterface())
            .isSameInstanceAs(binder)
    }

    @Test
    @SdkSuppress(minSdkVersion = TIRAMISU)
    fun toSandboxedSdk_whenCreatedFromBinder_returnsSandboxedSdkWithSameBinder() {
        if (!isSandboxAvailable()) {
            return
        }

        val binder = Binder()

        val toSandboxedSdkResult = create(binder).toSandboxedSdk()

        assertThat(toSandboxedSdkResult.getInterface())
            .isSameInstanceAs(binder)
    }

    @Test
    @SdkSuppress(minSdkVersion = TIRAMISU)
    fun toSandboxedSdk_whenCreatedFromSandboxedSdk_returnsSameSandboxedSdk() {
        if (!isSandboxAvailable()) {
            return
        }

        val binder = Binder()
        val sandboxedSdk = SandboxedSdk(binder)

        val toSandboxedSdkResult = toSandboxedSdkCompat(sandboxedSdk).toSandboxedSdk()

        assertThat(toSandboxedSdkResult)
            .isSameInstanceAs(sandboxedSdk)
    }

    private fun isSandboxAvailable(): Boolean {
        // TODO(b/249981547) Find a way how to skip test if no sandbox present
        return AdServicesVersion.API_VERSION >= 2
    }
}