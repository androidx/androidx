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
import android.annotation.SuppressLint
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/249982507) Test should be rewritten to use real SDK in sandbox instead of mocking manager
// TODO(b/249981547) Remove suppress when prebuilt with SdkSandbox APIs dropped to T
@SuppressLint("NewApi")
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SdkSandboxManagerCompatSandboxedTest {

    private lateinit var mContext: Context

    @Before
    fun setUp() {
        mContext = Mockito.spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxAvailable_delegateToPlatformLoadSdk() {
        if (!isSandboxAvailable()) {
            return
        }

        val sdkSandboxManager = mockSandboxManager(mContext)
        setupLoadSdkAnswer(sdkSandboxManager, SandboxedSdk(Binder()))

        val managerCompat = SdkSandboxManagerCompat.obtain(mContext)
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
        if (!isSandboxAvailable()) {
            return
        }

        val sdkSandboxManager = mockSandboxManager(mContext)

        val sandboxedSdk = SandboxedSdk(Binder())
        setupLoadSdkAnswer(sdkSandboxManager, sandboxedSdk)

        val managerCompat = SdkSandboxManagerCompat.obtain(mContext)

        val result = runBlocking {
            managerCompat.loadSdk("test", Bundle())
        }

        assertThat(result.getInterface()).isEqualTo(sandboxedSdk.getInterface())
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxAvailable_rethrowsExceptionFromPlatformLoadSdk() {
        if (!isSandboxAvailable()) {
            return
        }

        val sdkSandboxManager = mockSandboxManager(mContext)

        val loadSdkException = LoadSdkException(
            RuntimeException(),
            Bundle()
        )
        setupLoadSdkAnswer(sdkSandboxManager, loadSdkException)

        val managerCompat = SdkSandboxManagerCompat.obtain(mContext)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("test", Bundle())
            }
        }

        assertThat(result.cause).isEqualTo(loadSdkException.cause)
        assertThat(result.extraInformation).isEqualTo(loadSdkException.extraInformation)
        assertThat(result.loadSdkErrorCode).isEqualTo(loadSdkException.loadSdkErrorCode)
    }

    companion object SandboxApi {

        private fun isSandboxAvailable(): Boolean {
            // TODO(b/249981547) Find a way how to skip test if no sandbox present
            return AdServicesVersion.API_VERSION >= 2
        }

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