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

import android.annotation.SuppressLint
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/249982507) Test should be rewritten to use real SDK in sandbox instead of mocking manager
// TODO(b/249981547) Remove suppress after updating to new lint version (b/262251309)
@SuppressLint("NewApi")
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = AD_SERVICES, version = 4)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SdkSandboxManagerCompatSandboxedTest {

    private lateinit var mContext: Context

    @Before
    fun setUp() {
        assumeTrue("Requires Sandbox API available", isSandboxApiAvailable())
        mContext = Mockito.spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxAvailable_delegateToPlatformLoadSdk() {
        val sdkSandboxManager = mockSandboxManager(mContext)
        setupLoadSdkAnswer(sdkSandboxManager, SandboxedSdk(Binder()))

        val managerCompat = SdkSandboxManagerCompat.from(mContext)
        val sdkName = "test"
        val params = Bundle()

        runBlocking {
            managerCompat.loadSdk(sdkName, params)
        }

        verify(sdkSandboxManager).loadSdk(
            eq(sdkName),
            eq(params),
            any(),
            any()
        )
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxAvailable_returnResultFromPlatformLoadSdk() {
        val sdkSandboxManager = mockSandboxManager(mContext)

        val sandboxedSdk = SandboxedSdk(Binder())
        setupLoadSdkAnswer(sdkSandboxManager, sandboxedSdk)

        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val result = runBlocking {
            managerCompat.loadSdk("test", Bundle())
        }

        assertThat(result.getInterface()).isEqualTo(sandboxedSdk.getInterface())
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxAvailable_rethrowsExceptionFromPlatformLoadSdk() {
        val sdkSandboxManager = mockSandboxManager(mContext)

        val loadSdkException = LoadSdkException(
            RuntimeException(),
            Bundle()
        )
        setupLoadSdkAnswer(sdkSandboxManager, loadSdkException)

        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("test", Bundle())
            }
        }

        assertThat(result.cause).isEqualTo(loadSdkException.cause)
        assertThat(result.extraInformation).isEqualTo(loadSdkException.extraInformation)
        assertThat(result.loadSdkErrorCode).isEqualTo(loadSdkException.loadSdkErrorCode)
    }

    @Test
    fun getSandboxedSdks_whenLoadedSdkListNotAvailable_dontDelegateToSandbox() {
        assumeFalse("Requires getSandboxedSdks API not available", isSdkListAvailable())

        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        managerCompat.getSandboxedSdks()

        verifyZeroInteractions(sdkSandboxManager)
    }

    @Test
    fun getSandboxedSdks_whenLoadedSdkListNotAvailable_returnsEmptyList() {
        assumeFalse("Requires getSandboxedSdks API not available", isSdkListAvailable())

        // SdkSandboxManagerCompat.from require SandboxManager available for AdServices version >= 4
        mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val sandboxedSdks = managerCompat.getSandboxedSdks()

        assertThat(sandboxedSdks).isEmpty()
    }

    @Test
    // TODO(b/265295473) Update version check after AdServices V5 finalisation.
    @SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
    fun getSandboxedSdks_whenLoadedSdkListAvailable_returnResultFromPlatformGetSandboxedSdks() {
        assumeTrue("Requires getSandboxedSdks API available", isSdkListAvailable())

        val sdkSandboxManager = mockSandboxManager(mContext)
        val sandboxedSdk = SandboxedSdk(Binder())
        `when`(sdkSandboxManager.sandboxedSdks)
            .thenReturn(listOf(sandboxedSdk))
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val sandboxedSdks = managerCompat.getSandboxedSdks()
        assertThat(sandboxedSdks).hasSize(1)
        val result = sandboxedSdks[0]

        assertThat(result.getInterface())
            .isEqualTo(sandboxedSdk.getInterface())
    }

    companion object SandboxApi {

        private fun isSandboxApiAvailable() =
            AdServicesInfo.version() >= 4

        private fun isSdkListAvailable() =
            AdServicesInfo.isAtLeastV5()

        private fun mockSandboxManager(spyContext: Context): SdkSandboxManager {
            val sdkSandboxManager = Mockito.mock(SdkSandboxManager::class.java)
            `when`(spyContext.getSystemService(SdkSandboxManager::class.java))
                .thenReturn(sdkSandboxManager)
            return sdkSandboxManager
        }

        private fun setupLoadSdkAnswer(
            sdkSandboxManager: SdkSandboxManager,
            sandboxedSdk: SandboxedSdk
        ) {
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<SandboxedSdk, LoadSdkException>>(3)
                receiver.onResult(sandboxedSdk)
                null
            }
            doAnswer(answer)
                .`when`(sdkSandboxManager).loadSdk(
                    any(),
                    any(),
                    any(),
                    any()
                )
        }

        private fun setupLoadSdkAnswer(
            sdkSandboxManager: SdkSandboxManager,
            loadSdkException: LoadSdkException
        ) {
            val answer = { args: InvocationOnMock ->
                val receiver = args.getArgument<OutcomeReceiver<SandboxedSdk, LoadSdkException>>(3)
                receiver.onError(loadSdkException)
                null
            }
            doAnswer(answer)
                .`when`(sdkSandboxManager).loadSdk(
                    any(),
                    any(),
                    any(),
                    any()
                )
        }
    }
}