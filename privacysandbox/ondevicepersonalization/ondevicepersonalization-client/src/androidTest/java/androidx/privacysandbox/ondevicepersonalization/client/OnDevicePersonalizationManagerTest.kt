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

package androidx.privacysandbox.ondevicepersonalization.client

import android.adservices.ondevicepersonalization.OnDevicePersonalizationManager
import android.adservices.ondevicepersonalization.SurfacePackageToken
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.os.OutcomeReceiver
import android.os.PersistableBundle
import android.os.ext.SdkExtensions
import android.view.SurfaceControlViewHost.SurfacePackage
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.dx.mockito.inline.extended.StaticMockitoSession
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.quality.Strictness

@SmallTest
@SuppressWarnings("NewApi")
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class OnDevicePersonalizationManagerTest {
    private var mSession: StaticMockitoSession? = null
    private val mValidAdServicesSdkExtVersion =
        SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 12

    @Before
    fun setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext<Context>())
        if (mValidAdServicesSdkExtVersion)
            mSession =
                ExtendedMockito.mockitoSession()
                    .mockStatic(OnDevicePersonalizationManager::class.java)
                    .strictness(Strictness.LENIENT)
                    .startMocking()
    }

    @After
    fun tearDown() {
        mSession?.finishMocking()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34, minSdkVersion = 30)
    fun testODPOlderVersions() {
        Assume.assumeTrue("maxSdkVersion = API 34 ext 12", !mValidAdServicesSdkExtVersion)
        assertThat(
                androidx.privacysandbox.ondevicepersonalization.client
                    .OnDevicePersonalizationManager
                    .obtain(mContext)
            )
            .isNull()
    }

    @Test
    fun testODPManagerExecute() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", mValidAdServicesSdkExtVersion)

        var testResult: OnDevicePersonalizationManager.ExecuteResult =
            mock(OnDevicePersonalizationManager.ExecuteResult::class.java)
        val odpManager = mockODPManager(mContext)
        val managerCompat =
            androidx.privacysandbox.ondevicepersonalization.client.OnDevicePersonalizationManager
                .obtain(mContext)

        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onResult(testResult)
            null
        }
        doAnswer(answer)
            .`when`(odpManager)
            .execute(
                any<ComponentName>(),
                any<PersistableBundle>(),
                any<Executor>(),
                any<
                    OutcomeReceiver<
                        OnDevicePersonalizationManager.ExecuteResult,
                        java.lang.Exception,
                    >
                >(),
            )
        // Actually invoke the compat code.
        runBlocking {
            managerCompat?.executeInIsolatedService(
                ExecuteInIsolatedServiceRequest(TEST_SERVICE_COMPONENT_NAME, TEST_BUNDLE)
            )
        }
        val captor1 = ArgumentCaptor.forClass(ComponentName::class.java)
        val captor2 = ArgumentCaptor.forClass(PersistableBundle::class.java)
        verify(odpManager)
            .execute(
                captor1.capture(),
                captor2.capture(),
                any<Executor>(),
                any<OutcomeReceiver<OnDevicePersonalizationManager.ExecuteResult, Exception>>(),
            )

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == TEST_SERVICE_COMPONENT_NAME)
        assertThat(captor2.value == TEST_BUNDLE)
    }

    @Test
    fun testODPManagerExecuteThrows() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", mValidAdServicesSdkExtVersion)

        val odpManager = mockODPManager(mContext)
        val managerCompat =
            androidx.privacysandbox.ondevicepersonalization.client.OnDevicePersonalizationManager
                .obtain(mContext)
        val errorMessage = "some error occurred"
        val errorCallback = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(3)
            receiver.onError(ClassNotFoundException(errorMessage))
            null
        }
        doAnswer(errorCallback)
            .`when`(odpManager)
            .execute(
                any<ComponentName>(),
                any<PersistableBundle>(),
                any<Executor>(),
                any<
                    OutcomeReceiver<
                        OnDevicePersonalizationManager.ExecuteResult,
                        java.lang.Exception,
                    >
                >(),
            )
        // Actually invoke the compat code.
        runBlocking {
            try {
                managerCompat?.executeInIsolatedService(
                    ExecuteInIsolatedServiceRequest(TEST_SERVICE_COMPONENT_NAME, TEST_BUNDLE)
                )
            } catch (e: ClassNotFoundException) {
                assertThat(e.message).isEqualTo(errorMessage)
            }
        }
    }

    @Test
    fun testODPManagerSurface() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", mValidAdServicesSdkExtVersion)
        val odpManager = mockODPManager(mContext)
        val managerCompat =
            androidx.privacysandbox.ondevicepersonalization.client.OnDevicePersonalizationManager
                .obtain(mContext)

        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(6)
            receiver.onResult(TEST_SURFACE_PACKAGE)
        }
        val surfacePackageToken = mock(SurfacePackageToken::class.java)
        val iBinder = mock(IBinder::class.java)

        doAnswer(answer)
            .`when`(odpManager)
            .requestSurfacePackage(
                any<SurfacePackageToken>(),
                any<IBinder>(),
                any<Int>(),
                any<Int>(),
                any<Int>(),
                any<Executor>(),
                any<OutcomeReceiver<SurfacePackage, Exception>>(),
            )
        // Actually invoke the compat code.
        runBlocking {
            val result: SurfacePackage? =
                managerCompat?.requestSurfacePackage(
                    surfacePackageToken,
                    iBinder,
                    TEST_DISPLAY_ID,
                    TEST_HEIGHT,
                    TEST_WIDTH,
                )
            assertThat(result == TEST_SURFACE_PACKAGE)
        }
        val captor1 = ArgumentCaptor.forClass(SurfacePackageToken::class.java)
        val captor2 = ArgumentCaptor.forClass(IBinder::class.java)
        val captor3 = ArgumentCaptor.forClass(Int::class.java)
        val captor4 = ArgumentCaptor.forClass(Int::class.java)
        val captor5 = ArgumentCaptor.forClass(Int::class.java)

        verify(odpManager)
            .requestSurfacePackage(
                captor1.capture(),
                captor2.capture(),
                captor3.capture(),
                captor4.capture(),
                captor5.capture(),
                any<Executor>(),
                any<OutcomeReceiver<SurfacePackage, Exception>>(),
            )

        // Verify that the request that the compat code makes to the platform is correct.
        assertThat(captor1.value == surfacePackageToken)
        assertThat(captor2.value == iBinder)
        assertThat(captor3.value == TEST_DISPLAY_ID)
        assertThat(captor4.value == TEST_HEIGHT)
        assertThat(captor5.value == TEST_WIDTH)
    }

    @Test
    fun testODPManagerSurfaceThrows() {
        Assume.assumeTrue("minSdkVersion = API 34 ext 12", mValidAdServicesSdkExtVersion)
        val odpManager = mockODPManager(mContext)
        val managerCompat =
            androidx.privacysandbox.ondevicepersonalization.client.OnDevicePersonalizationManager
                .obtain(mContext)

        val errorMessage = "some error occurred"
        val errorCallback = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<Any, Exception>>(6)
            receiver.onError(Exception(errorMessage))
        }
        val surfacePackageToken = mock(SurfacePackageToken::class.java)
        val iBinder = mock(IBinder::class.java)

        doAnswer(errorCallback)
            .`when`(odpManager)
            .requestSurfacePackage(
                any<SurfacePackageToken>(),
                any<IBinder>(),
                any<Int>(),
                any<Int>(),
                any<Int>(),
                any<Executor>(),
                any<OutcomeReceiver<SurfacePackage, Exception>>(),
            )
        // Actually invoke the compat code.
        runBlocking {
            try {
                managerCompat?.requestSurfacePackage(
                    surfacePackageToken,
                    iBinder,
                    TEST_DISPLAY_ID,
                    TEST_HEIGHT,
                    TEST_WIDTH,
                )
            } catch (e: Exception) {
                assertThat(e.message).isEqualTo(errorMessage)
            }
        }
    }

    companion object {
        private lateinit var mContext: Context
        private val TEST_SERVICE_COMPONENT_NAME: ComponentName =
            ComponentName.createRelative("com.example.service", ".Example")
        private val TEST_BUNDLE: PersistableBundle = PersistableBundle.EMPTY
        private const val TEST_DISPLAY_ID: Int = 1234
        private const val TEST_HEIGHT: Int = 600
        private const val TEST_WIDTH: Int = 300

        private val TEST_SURFACE_PACKAGE: SurfacePackage = mock(SurfacePackage::class.java)

        private fun mockODPManager(spyContext: Context): OnDevicePersonalizationManager {
            val odpManager = mock(OnDevicePersonalizationManager::class.java)
            `when`(spyContext.getSystemService(OnDevicePersonalizationManager::class.java))
                .thenReturn(odpManager)
            return odpManager
        }
    }
}
