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

import android.app.sdksandbox.SandboxedSdk
import android.content.pm.SharedLibraryInfo
import android.os.Binder
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`

@SmallTest
@RunWith(AndroidJUnit4::class)
class SandboxedSdkCompatTest {

    @Test
    fun getInterface_returnsBinderPassedToCreate() {
        val binder = Binder()

        val sandboxedSdkCompat = SandboxedSdkCompat(binder)

        assertThat(sandboxedSdkCompat.getInterface()).isSameInstanceAs(binder)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun toSandboxedSdk_whenCreatedFromBinder_returnsSandboxedSdkWithSameBinder() {
        val binder = Binder()

        val toSandboxedSdkResult = SandboxedSdkCompat(binder).toSandboxedSdk()

        assertThat(toSandboxedSdkResult.getInterface()).isSameInstanceAs(binder)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun toSandboxedSdk_whenCreatedFromSandboxedSdk_returnsSameSandboxedSdk() {
        val binder = Binder()
        val sandboxedSdk = SandboxedSdk(binder)

        val toSandboxedSdkResult = SandboxedSdkCompat(sandboxedSdk).toSandboxedSdk()

        assertThat(toSandboxedSdkResult).isSameInstanceAs(sandboxedSdk)
    }

    @Test
    fun getSdkInfo_whenCreatedFromBinder_returnsNull() {
        val binder = Binder()
        val sandboxedSdkCompat = SandboxedSdkCompat(binder)

        assertThat(sandboxedSdkCompat.getSdkInfo()).isNull()
    }

    @Test
    fun getSdkInfo_whenCreatedFromBinderAndSdkInfo_returnsSdkInfo() {
        val binder = Binder()
        val sdkInfo = SandboxedSdkInfo(name = "sdkname", version = 42)
        val sandboxedSdkCompat = SandboxedSdkCompat(binder, sdkInfo)

        assertThat(sandboxedSdkCompat.getSdkInfo()).isEqualTo(sdkInfo)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getSdkInfo_whenCreatedFromSandboxedSdk_returnsSdkInfo() {
        val sdkName = "sdkName"
        val sdkVersion = 1L
        val sharedLibraryInfo = Api34Impl.mockSharedLibraryInfo(sdkName, sdkVersion)
        val sandboxedSdk = Api34Impl.mockSandboxedSdkWithSharedLibraryInfo(sharedLibraryInfo)

        val sandboxedSdkCompat = SandboxedSdkCompat(sandboxedSdk)
        val sdkInfo = sandboxedSdkCompat.getSdkInfo()

        assertThat(sdkInfo).isEqualTo(SandboxedSdkInfo(sdkName, sdkVersion))
    }

    private object Api34Impl {
        @RequiresApi(28)
        fun mockSharedLibraryInfo(sdkName: String, sdkVersion: Long): SharedLibraryInfo {
            // No public constructor for SharedLibraryInfo available.
            val sharedLibraryInfo = Mockito.mock(SharedLibraryInfo::class.java)
            `when`(sharedLibraryInfo.name).thenReturn(sdkName)
            `when`(sharedLibraryInfo.longVersion).thenReturn(sdkVersion)
            return sharedLibraryInfo
        }

        @RequiresApi(34)
        fun mockSandboxedSdkWithSharedLibraryInfo(
            sharedLibraryInfo: SharedLibraryInfo
        ): SandboxedSdk {
            val binder = Binder()
            val sandboxedSdk = SandboxedSdk(binder)
            val sandboxedSdkSpy = spy(sandboxedSdk)
            // Platform uses attachSharedLibraryInfo (hidden) to set sharedLibraryInfo.
            doReturn(sharedLibraryInfo).`when`(sandboxedSdkSpy).sharedLibraryInfo
            return sandboxedSdkSpy
        }
    }
}
