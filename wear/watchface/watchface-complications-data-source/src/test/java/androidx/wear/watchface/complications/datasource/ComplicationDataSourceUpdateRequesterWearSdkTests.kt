/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.watchface.complications.datasource

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.wear.watchface.complications.data.ComplicationType
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/** Tests related to the WearSDK API for [ComplicationDataSourceUpdateRequesterImpl]. */
@RunWith(ComplicationsTestRunner::class)
@SdkSuppress(minSdkVersion = 36, codeName = "Baklava")
@OptIn(ExperimentalCoroutinesApi::class)
class ComplicationDataSourceUpdateRequesterWearSdkTests {
    @get:Rule val mocks: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val providerComponent1 = ComponentName("pkg1", "cls1")
    private val providerComponent2 = ComponentName("pkg1", "cls2")
    private val testDispatcher = StandardTestDispatcher()

    private var requester: ComplicationDataSourceUpdateRequesterImpl? = null
    private var fakeConfig1 =
        WearSdkActiveComplicationConfig(providerComponent1, 1, ComplicationType.SHORT_TEXT, 1)
    private var fakeConfig2 =
        WearSdkActiveComplicationConfig(providerComponent1, 2, ComplicationType.SHORT_TEXT, 1)
    private var fakeConfig3 =
        WearSdkActiveComplicationConfig(providerComponent1, 3, ComplicationType.SMALL_IMAGE, 0)
    private var fakeConfig4 =
        WearSdkActiveComplicationConfig(providerComponent2, 4, ComplicationType.LONG_TEXT, 1)
    internal var configDataMap: Map<WearSdkActiveComplicationConfig, WearSdkComplicationData> =
        mapOf()

    @Mock private lateinit var mockApi: WearSdkComplicationsApi
    @Mock private lateinit var mockData1: WearSdkComplicationData
    @Mock private lateinit var mockData2: WearSdkComplicationData
    @Mock private lateinit var mockData3: WearSdkComplicationData
    @Mock private lateinit var mockData4: WearSdkComplicationData
    @Mock private lateinit var mockRequester: WearSdkComplicationDataRequester
    @Mock private lateinit var mockConnection: ServiceConnection

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        requester = ComplicationDataSourceUpdateRequesterImpl(context, providerComponent1)
        configDataMap =
            mapOf(
                fakeConfig1 to mockData1,
                fakeConfig2 to mockData2,
                fakeConfig3 to mockData3,
                fakeConfig4 to mockData4,
            )
    }

    suspend fun setupSuspendMocks(component: ComponentName = providerComponent1) {
        whenever(mockApi.getActiveConfigs(any<Executor>()))
            .thenReturn(
                configDataMap.keys
                    .filter {
                        it.providerComponent.packageName == component.packageName &&
                            it.providerComponent.className == component.className
                    }
                    .toSet()
            )
        doAnswer { invocation ->
                val request = invocation.getArgument<ComplicationRequest>(0)
                return@doAnswer configDataMap
                    .filter { (config, _) -> config.id == request.complicationInstanceId }
                    .map { (config, data) -> config.id to data }
                    .first()
            }
            .whenever(mockRequester)
            .requestData(any())
    }

    @Test
    fun updateComplicationsUsingWearSdk_noActiveConfigs_noUpdateComplicationCalls() = runTest {
        val component = ComponentName("random", "component")
        requester = ComplicationDataSourceUpdateRequesterImpl(context, component)
        setupSuspendMocks(component)

        requestUpdatesUsingMocks(requester)
        advanceUntilIdle()

        verify(mockApi).getActiveConfigs(any<Executor>())
        verifyNoMoreInteractions(mockApi)
    }

    @Test
    fun updateComplicationsUsingWearSdk_activeConfigs_correspondingUpdateComplicationCalls() =
        runTest {
            setupSuspendMocks()

            requestUpdatesUsingMocks(requester)
            advanceUntilIdle()

            verify(mockApi).getActiveConfigs(any<Executor>())
            verify(mockApi)
                .updateComplication(
                    eq(fakeConfig1.id),
                    eq(configDataMap[fakeConfig1]!!),
                    any<Executor>(),
                )
            verify(mockApi)
                .updateComplication(
                    eq(fakeConfig2.id),
                    eq(configDataMap[fakeConfig2]!!),
                    any<Executor>(),
                )
            verify(mockApi)
                .updateComplication(
                    eq(fakeConfig3.id),
                    eq(configDataMap[fakeConfig3]!!),
                    any<Executor>(),
                )
            verifyNoMoreInteractions(mockApi)
        }

    @Test
    fun updateComplicationsUsingWearSdk_onlyCurrentProvidersCalled() = runTest {
        setupSuspendMocks()

        requestUpdatesUsingMocks(requester)
        advanceUntilIdle()

        val requestCaptor = argumentCaptor<ComplicationRequest>()
        verify(mockRequester, times(3)).requestData(requestCaptor.capture())
        assertThat(requestCaptor.firstValue.complicationInstanceId).isEqualTo(fakeConfig1.id)
        assertThat(requestCaptor.secondValue.complicationInstanceId).isEqualTo(fakeConfig2.id)
        assertThat(requestCaptor.thirdValue.complicationInstanceId).isEqualTo(fakeConfig3.id)
    }

    @Test
    fun updateComplicationsUsingWearSdk_noComplicationRequestsBeforeBind() = runTest {
        val mockProvider = mock<TestRequesterProvider>()
        doReturn(mockDataSourceProvider()).whenever(mockProvider).provider()
        setupSuspendMocks()

        requestUpdatesUsingMocks(requester, provider = mockProvider)
        advanceUntilIdle()

        verify(mockProvider, times(1)).provider()
        verifyNoMoreInteractions(mockProvider)
        verify(mockRequester, times(3)).requestData(any<ComplicationRequest>())
        verifyNoMoreInteractions(mockRequester)
    }

    private fun mockDataSourceProvider() = Pair(mockRequester, mockConnection)

    private fun requestUpdatesUsingMocks(
        requester: ComplicationDataSourceUpdateRequesterImpl?,
        provider: TestRequesterProvider =
            object : TestRequesterProvider {
                override fun provider() = mockDataSourceProvider()
            },
    ) =
        requester?.updateComplicationsUsingWearSdk(IntArray(0), mockApi, testDispatcher) {
            provider.provider()
        }

    private interface TestRequesterProvider {
        fun provider(): Pair<WearSdkComplicationDataRequester, ServiceConnection>
    }
}
