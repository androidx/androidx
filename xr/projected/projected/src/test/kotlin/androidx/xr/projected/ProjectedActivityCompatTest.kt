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

package androidx.xr.projected

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.xr.projected.ProjectedServiceBinding.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedInputEventListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedInputAction
import androidx.xr.projected.platform.ProjectedInputEvent
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedActivityCompatTest {

    private val mockProjectedService = mock<IProjectedService>()
    private val mockProjectedServiceStub =
        mock<IProjectedService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockProjectedService)
        }
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(COMPONENT_NAME)
            addOrUpdateService(SERVICE_INFO)
            addIntentFilterForService(COMPONENT_NAME, IntentFilter(ACTION_BIND))
            installPackage(PACKAGE_INFO)
        }

        shadowOf(context)
            .setComponentNameAndServiceForBindService(COMPONENT_NAME, mockProjectedServiceStub)
    }

    @Test
    fun create_returnsProjectedActivityCompatInstance() = runBlocking {
        shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)

        val projectedActivityCompat = ProjectedActivityCompat.create(context)

        assertThat(projectedActivityCompat).isNotNull()
    }

    @Test
    fun create_bindingToServiceNotPermitted_throwsIllegalStateException() {
        shadowOf(context).declareComponentUnbindable(COMPONENT_NAME)

        assertFailsWith<IllegalStateException> {
            runBlocking { ProjectedActivityCompat.create(context) }
        }
    }

    @Test
    fun projectedInputEvents_emitsProjectedInputEvent() =
        runTest(UnconfinedTestDispatcher()) {
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            val projectedActivityCompat = ProjectedActivityCompat.create(context)

            launch {
                val receivedInputEvent = projectedActivityCompat.projectedInputEvents.first()
                assertThat(receivedInputEvent.inputAction)
                    .isEqualTo(
                        androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
                            .TOGGLE_APP_CAMERA
                    )
            }

            val inputEventListenerCaptor =
                ArgumentCaptor.forClass(IProjectedInputEventListener::class.java)
            verify(mockProjectedService)
                .registerProjectedInputEventListener(inputEventListenerCaptor.capture())
            inputEventListenerCaptor.firstValue.onProjectedInputEvent(
                ProjectedInputEvent().apply { action = ProjectedInputAction.TOGGLE_APP_CAMERA }
            )
        }

    @Test
    fun projectedInputEvents_flowIsClosed_afterCloseCalled() =
        runTest(UnconfinedTestDispatcher()) {
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            val projectedActivityCompat = ProjectedActivityCompat.create(context)
            var isFlowClosed = false
            val job =
                backgroundScope.launch {
                    try {
                        projectedActivityCompat.projectedInputEvents.collect { /* Do nothing */ }
                    } finally {
                        isFlowClosed = true
                    }
                }

            projectedActivityCompat.close()
            job.join()

            assertThat(isFlowClosed).isTrue()
        }

    @Test
    fun projectedInputEvents_doesNotEmit_whenUnknownActionIsReceived() =
        runTest(UnconfinedTestDispatcher()) {
            shadowOf(context).setBindServiceCallsOnServiceConnectedDirectly(true)
            val projectedActivityCompat = ProjectedActivityCompat.create(context)
            var receivedInputEvent: androidx.xr.projected.ProjectedInputEvent? = null
            val job = launch {
                projectedActivityCompat.projectedInputEvents.collect { receivedInputEvent = it }
            }

            val inputEventListenerCaptor =
                ArgumentCaptor.forClass(IProjectedInputEventListener::class.java)
            verify(mockProjectedService)
                .registerProjectedInputEventListener(inputEventListenerCaptor.capture())
            inputEventListenerCaptor.firstValue.onProjectedInputEvent(
                ProjectedInputEvent().apply { action = INVALID_PROJECTED_ACTION_CODE }
            )

            assertThat(receivedInputEvent).isNull()
            job.cancel()
        }

    @Test
    fun projectedInputAction_fromCode_returnsCorrectEnum() {
        val action =
            androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction.fromCode(
                androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction.TOGGLE_APP_CAMERA
                    .code
            )

        assertThat(action)
            .isEqualTo(
                androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction.TOGGLE_APP_CAMERA
            )
    }

    @Test
    fun projectedInputAction_fromCode_withInvalidCode_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction.fromCode(
                INVALID_PROJECTED_ACTION_CODE
            )
        }
    }

    companion object {
        private const val SYSTEM_PACKAGE_NAME = "com.system.service"
        private const val SYSTEM_CLASS_NAME = "com.system.service.ProjectedService"
        private val COMPONENT_NAME = ComponentName(SYSTEM_PACKAGE_NAME, SYSTEM_CLASS_NAME)
        private val SERVICE_INFO =
            ServiceInfo().apply {
                packageName = SYSTEM_PACKAGE_NAME
                name = SYSTEM_CLASS_NAME
            }
        private val PACKAGE_INFO =
            PackageInfo().apply {
                packageName = SYSTEM_PACKAGE_NAME
                services = arrayOf(SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
        private const val INVALID_PROJECTED_ACTION_CODE = -50
    }
}
