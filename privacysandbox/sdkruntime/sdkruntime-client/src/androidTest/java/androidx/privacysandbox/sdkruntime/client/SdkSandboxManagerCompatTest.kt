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
package androidx.privacysandbox.sdkruntime.client

import android.adservices.AdServicesVersion
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat.Companion.obtain
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_SDK_DEFINED_ERROR
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkSandboxManagerCompatTest {

    @Test
    fun obtain_whenCalledOnSameContext_returnSameManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val managerCompat = obtain(context)
        val managerCompat2 = obtain(context)

        assertThat(managerCompat2).isSameInstanceAs(managerCompat)
    }

    @Test
    fun obtain_whenCalledOnDifferentContext_returnDifferentManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ContextWrapper(context)

        val managerCompat = obtain(context)
        val managerCompat2 = obtain(context2)

        assertThat(managerCompat2).isNotSameInstanceAs(managerCompat)
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun loadSdk_whenNoLocalSdkExistsAndSandboxNotAvailable_notDelegateToSandbox() {
        if (isSandboxAvailable()) {
            return
        }
        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = obtain(context)

        assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("sdk-not-exists", Bundle())
            }
        }

        verify(context, Mockito.atLeastOnce()).assets
        verifyNoMoreInteractions(context)
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxNotAvailable_throwsSdkNotFoundException() {
        if (isSandboxAvailable()) {
            return
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = obtain(context)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("sdk-not-exists", Bundle())
            }
        }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_NOT_FOUND)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    fun loadSdk_whenLocalSdkExistsAndAtLeastApi27_returnResultFromCompatLoadSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = obtain(context)

        val result = runBlocking {
            managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", Bundle())
        }

        assertThat(result.getInterface()!!.javaClass.classLoader)
            .isNotSameInstanceAs(managerCompat.javaClass.classLoader)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
    fun loadSdk_LocalSdkExistsAndAtLeastApi27_rethrowsExceptionFromCompatLoadSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = obtain(context)

        val params = Bundle()
        params.putBoolean("needFail", true)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", params)
            }
        }

        assertThat(result.extraInformation).isEqualTo(params)
        assertThat(result.loadSdkErrorCode).isEqualTo(LOAD_SDK_SDK_DEFINED_ERROR)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O)
    fun loadSdk_whenLocalSdkExistsAndApiPre27_throwsSandboxDisabledException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = obtain(context)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", Bundle())
            }
        }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_SDK_SANDBOX_DISABLED)
    }

    private fun isSandboxAvailable(): Boolean {
        // TODO(b/249981547) Find a way how to skip test if sandbox present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return isSandboxAvailableOnApi33()
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    companion object SandboxApi {
        @DoNotInline
        private fun isSandboxAvailableOnApi33(): Boolean {
            return AdServicesVersion.API_VERSION >= 2
        }
    }
}