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

import android.app.Activity
import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/249982507) Test should be rewritten to use real SDK in sandbox instead of mocking manager
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

    @After
    fun tearDown() {
        SdkSandboxManagerCompat.reset()
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
    fun unloadSdk_whenNoLocalSdkLoadedAndSandboxAvailable_delegateToPlatform() {
        val sdkSandboxManager = mockSandboxManager(mContext)

        val managerCompat = SdkSandboxManagerCompat.from(mContext)
        val sdkName = "test"

        managerCompat.unloadSdk(sdkName)

        verify(sdkSandboxManager).unloadSdk(
            eq(sdkName)
        )
    }

    @Test
    fun addSdkSandboxProcessDeathCallback_whenSandboxAvailable_delegateToPlatform() {
        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val callback = mock(SdkSandboxProcessDeathCallbackCompat::class.java)

        managerCompat.addSdkSandboxProcessDeathCallback(Runnable::run, callback)
        val argumentCaptor = ArgumentCaptor.forClass(
            SdkSandboxManager.SdkSandboxProcessDeathCallback::class.java
        )
        verify(sdkSandboxManager)
            .addSdkSandboxProcessDeathCallback(any(), argumentCaptor.capture())
        val platformCallback = argumentCaptor.value

        platformCallback.onSdkSandboxDied()
        verify(callback).onSdkSandboxDied()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    fun startSdkSandboxActivity_whenSandboxAvailable_delegateToPlatform() {
        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val fromActivityMock = mock(Activity::class.java)
        val tokenMock = mock(IBinder::class.java)
        managerCompat.startSdkSandboxActivity(fromActivityMock, tokenMock)

        verify(sdkSandboxManager).startSdkSandboxActivity(fromActivityMock, tokenMock)
    }

    @Test
    fun removeSdkSandboxProcessDeathCallback_whenSandboxAvailable_removeAddedCallback() {
        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val callback = mock(SdkSandboxProcessDeathCallbackCompat::class.java)

        managerCompat.addSdkSandboxProcessDeathCallback(Runnable::run, callback)
        val addArgumentCaptor = ArgumentCaptor.forClass(
            SdkSandboxManager.SdkSandboxProcessDeathCallback::class.java
        )
        verify(sdkSandboxManager)
            .addSdkSandboxProcessDeathCallback(any(), addArgumentCaptor.capture())
        val addedPlatformCallback = addArgumentCaptor.value

        managerCompat.removeSdkSandboxProcessDeathCallback(callback)
        val removeArgumentCaptor = ArgumentCaptor.forClass(
            SdkSandboxManager.SdkSandboxProcessDeathCallback::class.java
        )
        verify(sdkSandboxManager)
            .removeSdkSandboxProcessDeathCallback(removeArgumentCaptor.capture())
        val removedPlatformCallback = removeArgumentCaptor.value

        assertThat(removedPlatformCallback).isSameInstanceAs(addedPlatformCallback)
    }

    @Test
    fun removeSdkSandboxProcessDeathCallback_whenNoCallbackAdded_doNothing() {
        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val callback = mock(SdkSandboxProcessDeathCallbackCompat::class.java)
        managerCompat.removeSdkSandboxProcessDeathCallback(callback)

        verify(sdkSandboxManager, never())
            .removeSdkSandboxProcessDeathCallback(any())
    }

    @Test
    fun getSandboxedSdks_whenLoadedSdkListNotAvailable_dontDelegateToSandbox() {
        assumeFalse("Requires getSandboxedSdks API not available", isAtLeastV5())

        val sdkSandboxManager = mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        managerCompat.getSandboxedSdks()

        verifyZeroInteractions(sdkSandboxManager)
    }

    @Test
    fun getSandboxedSdks_whenLoadedSdkListNotAvailable_returnsLocallyLoadedSdkList() {
        assumeFalse("Requires getSandboxedSdks API not available", isAtLeastV5())

        // SdkSandboxManagerCompat.from require SandboxManager available for AdServices version >= 4
        mockSandboxManager(mContext)
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.CURRENT.packageName,
                Bundle()
            )
        }

        val sandboxedSdks = managerCompat.getSandboxedSdks()

        assertThat(sandboxedSdks).containsExactly(localSdk)
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = AD_SERVICES, version = 5)
    fun getSandboxedSdks_whenLoadedSdkListAvailable_returnCombinedLocalAndPlatformResult() {
        assumeTrue("Requires getSandboxedSdks API available", isAtLeastV5())

        val sdkSandboxManager = mockSandboxManager(mContext)
        val sandboxedSdk = SandboxedSdk(Binder())
        `when`(sdkSandboxManager.sandboxedSdks)
            .thenReturn(listOf(sandboxedSdk))
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.CURRENT.packageName,
                Bundle()
            )
        }

        val result = managerCompat.getSandboxedSdks().map { it.getInterface() }
        assertThat(result).containsExactly(
            sandboxedSdk.getInterface(), localSdk.getInterface()
        )
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = AD_SERVICES, version = 5)
    fun sdkController_getSandboxedSdks_dontIncludeSandboxedSdk() {
        assumeTrue("Requires getSandboxedSdks API available", isAtLeastV5())

        val sdkSandboxManager = mockSandboxManager(mContext)
        val sandboxedSdk = SandboxedSdk(Binder())
        `when`(sdkSandboxManager.sandboxedSdks)
            .thenReturn(listOf(sandboxedSdk))
        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.forSdkName("v2").packageName,
                Bundle()
            )
        }

        val testSdk = localSdk.asTestSdk()

        val sdkList = testSdk.getSandboxedSdks()
        assertThat(sdkList).hasSize(1)
        val result = sdkList[0].getInterface()

        assertThat(result).isEqualTo(localSdk.getInterface())
    }

    companion object SandboxApi {

        private fun isSandboxApiAvailable() =
            AdServicesInfo.isAtLeastV4()

        private fun isAtLeastV5() =
            AdServicesInfo.isAtLeastV5()

        private fun mockSandboxManager(spyContext: Context): SdkSandboxManager {
            val sdkSandboxManager = mock(SdkSandboxManager::class.java)
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
