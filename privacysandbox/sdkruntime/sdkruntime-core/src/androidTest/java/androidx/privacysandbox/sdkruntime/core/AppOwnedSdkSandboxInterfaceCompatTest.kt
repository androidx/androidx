/*
 * Copyright 2023 The Android Open Source Project
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

import android.app.sdksandbox.AppOwnedSdkSandboxInterface
import android.os.Binder
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class AppOwnedSdkSandboxInterfaceCompatTest {

    @Before
    fun setUp() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            BuildCompat.AD_SERVICES_EXTENSION_INT >= 8
        )
    }

    @Test
    fun toAppOwnedSdkSandboxInterfaceTest() {
        val compatObj =
            AppOwnedSdkSandboxInterfaceCompat(name = "SDK", version = 1, binder = Binder())

        val platformObj = compatObj.toAppOwnedSdkSandboxInterface()

        assertThat(platformObj.getName()).isEqualTo(compatObj.getName())
        assertThat(platformObj.getVersion()).isEqualTo(compatObj.getVersion())
        assertThat(platformObj.getInterface()).isEqualTo(compatObj.getInterface())
    }

    @Test
    fun fromAppOwnedSdkSandboxInterfaceTest() {
        val platformObj = AppOwnedSdkSandboxInterface("SDK", 1, Binder())
        val compatObj = AppOwnedSdkSandboxInterfaceCompat(platformObj)

        assertThat(compatObj.getName()).isEqualTo(platformObj.getName())
        assertThat(compatObj.getVersion()).isEqualTo(platformObj.getVersion())
        assertThat(compatObj.getInterface()).isEqualTo(platformObj.getInterface())
    }
}
