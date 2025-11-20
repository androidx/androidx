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

import android.app.Activity
import android.app.Application
import android.companion.virtual.VirtualDeviceManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedContext.PROJECTED_DEVICE_NAME
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.ProjectedDisplayController.PresentationModeFlags
import androidx.xr.projected.ProjectedServiceBinding.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IEngagementModeCallback
import androidx.xr.projected.platform.IEngagementModeService
import androidx.xr.projected.platform.IProjectedService
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class)
class ProjectedDisplayControllerTest {

    private val mockProjectedService = mock<IProjectedService>()
    private val mockProjectedServiceStub =
        mock<IProjectedService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockProjectedService)
        }
    private val context: Application = ApplicationProvider.getApplicationContext()

    private lateinit var projectedDisplayController: ProjectedDisplayController

    @Before
    fun setUp() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(COMPONENT_NAME)
            addOrUpdateService(SERVICE_INFO)
            addIntentFilterForService(COMPONENT_NAME, IntentFilter(ACTION_BIND))
            installPackage(PACKAGE_INFO)
        }

        shadowOf(context).apply {
            setComponentNameAndServiceForBindService(COMPONENT_NAME, mockProjectedServiceStub)
            setBindServiceCallsOnServiceConnectedDirectly(true)
        }
    }

    @Test
    fun create_returnsProjectedDisplayControllerInstance() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }

            assertThat(projectedDisplayController).isNotNull()
        }

    @Test
    fun create_bindingToServiceNotPermitted_throwsException() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            shadowOf(context).declareComponentUnbindable(COMPONENT_NAME)

            assertFailsWith<IllegalStateException> {
                runBlocking {
                    projectedDisplayController =
                        ProjectedDisplayController.create(projectedDeviceActivity)
                }
            }
        }

    @Test
    fun create_nonProjectedDeviceActivity_throwsException() =
        launchTestActivity { nonProjectedDeviceActivity ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { ProjectedDisplayController.create(nonProjectedDeviceActivity) }
            }
        }

    @Test
    fun addFlags_callsService() = launchTestProjectedDeviceActivity { projectedDeviceActivity ->
        runBlocking {
            projectedDisplayController = ProjectedDisplayController.create(projectedDeviceActivity)
        }
        val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        projectedDisplayController.addLayoutParamsFlags(flags)

        verify(mockProjectedService).addWindowFlags(flags)
    }

    @Test
    fun removeFlags_callsService() = launchTestProjectedDeviceActivity { projectedDeviceActivity ->
        runBlocking {
            projectedDisplayController = ProjectedDisplayController.create(projectedDeviceActivity)
        }
        val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON

        projectedDisplayController.removeLayoutParamsFlags(flags)
        verify(mockProjectedService).clearWindowFlags(flags)
    }

    @Test
    fun close_disconnectsConnection() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            runBlocking {
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)
            }
            check(shadowOf(context).boundServiceConnections.size == 1)

            projectedDisplayController.close()

            assertThat(shadowOf(context).boundServiceConnections).isEmpty()
        }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
    @Test
    fun addPresentationModeChangedListener_callsService() {
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            val dispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(dispatcher)
            runBlocking {
                val mockEngagementModeService = setUpEngagementModeService()
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)

                val listener = Consumer<PresentationModeFlags> {}
                projectedDisplayController.addPresentationModeChangedListener(listener = listener)

                verify(mockEngagementModeService).registerCallback(any())

                removeEngagementModeService()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
    @Test
    fun addPresentationModeChangedListener_receivesUpdates() {
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            val dispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(dispatcher)
            runBlocking {
                val mockEngagementModeService = setUpEngagementModeService()
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)

                var presentationModes: PresentationModeFlags? = null

                projectedDisplayController.addPresentationModeChangedListener { updatedModes ->
                    presentationModes = updatedModes
                }

                // Trigger the callback.
                val callbackCaptor = argumentCaptor<IEngagementModeCallback>()
                verify(mockEngagementModeService).registerCallback(callbackCaptor.capture())
                callbackCaptor.firstValue.onEngagementModeChanged(
                    EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON
                )

                assertThat(presentationModes).isNotNull()
                assertThat(presentationModes?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                removeEngagementModeService()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
    @Test
    fun addMultipleEngagementModeChangedListener_receivesUpdates() {
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            val dispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(dispatcher)
            runBlocking {
                val mockEngagementModeService = setUpEngagementModeService()
                projectedDisplayController =
                    ProjectedDisplayController.create(projectedDeviceActivity)

                var presentationModes1: PresentationModeFlags? = null
                var callCount1 = 0
                val listener1 =
                    Consumer<PresentationModeFlags> { updatedModes ->
                        presentationModes1 = updatedModes
                        ++callCount1
                    }
                var presentationModes2: PresentationModeFlags? = null
                var callCount2 = 0
                val listener2 =
                    Consumer<PresentationModeFlags> { updatedModes ->
                        presentationModes2 = updatedModes
                        ++callCount2
                    }
                projectedDisplayController.addPresentationModeChangedListener(listener = listener1)
                projectedDisplayController.addPresentationModeChangedListener(listener = listener2)

                // Trigger the callback.
                val callbackCaptor = argumentCaptor<IEngagementModeCallback>()
                verify(mockEngagementModeService).registerCallback(callbackCaptor.capture())
                callbackCaptor.firstValue.onEngagementModeChanged(
                    EngagementModeClient.ENGAGEMENT_MODE_FLAG_VISUALS_ON
                )

                // Verify that both listeners were called once.
                assertThat(callCount1).isEqualTo(1)
                assertThat(presentationModes1).isNotNull()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                assertThat(callCount2).isEqualTo(1)
                assertThat(presentationModes2).isNotNull()
                assertThat(presentationModes2?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()

                // Remove the second callback.
                projectedDisplayController.removePresentationModeChangedListener(listener2)

                // Trigger another callback.
                callbackCaptor.firstValue.onEngagementModeChanged(0)

                // Verify that only the first listener was called again
                assertThat(callCount1).isEqualTo(2)
                assertThat(presentationModes1).isNotNull()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isFalse()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                assertThat(callCount2).isEqualTo(1)
                assertThat(presentationModes2).isNotNull()
                assertThat(presentationModes2?.hasPresentationMode(PresentationMode.VISUALS_ON))
                    .isTrue()
                assertThat(presentationModes1?.hasPresentationMode(PresentationMode.AUDIO_ON))
                    .isTrue()
                removeEngagementModeService()
            }
        }
    }

    private fun setUpEngagementModeService(): IEngagementModeService {
        val mockEngagementModeService = mock<IEngagementModeService>()
        val mockEngagementModeServiceStub =
            mock<IEngagementModeService.Stub> {
                on { queryLocalInterface(any()) }.thenReturn(mockEngagementModeService)
            }
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(ENGAGEMENT_MODE_SERVICE_COMPONENT)
            addOrUpdateService(
                ServiceInfo().apply {
                    packageName = SYSTEM_PACKAGE_NAME
                    name = ENGAGEMENT_MODE_SYSTEM_CLASS_NAME
                }
            )
            addIntentFilterForService(
                ENGAGEMENT_MODE_SERVICE_COMPONENT,
                IntentFilter(EngagementModeClient.SERVICE_ACTION),
            )
        }
        shadowOf(context).apply {
            setComponentNameAndServiceForBindService(
                ENGAGEMENT_MODE_SERVICE_COMPONENT,
                mockEngagementModeServiceStub,
            )
            setBindServiceCallsOnServiceConnectedDirectly(true)
        }
        return mockEngagementModeService
    }

    private fun removeEngagementModeService() {
        shadowOf(context.packageManager).apply { removeService(ENGAGEMENT_MODE_SERVICE_COMPONENT) }
    }

    private fun launchTestProjectedDeviceActivity(block: (Activity) -> Unit) {
        shadowOf(context.packageManager)
            .addOrUpdateActivity(
                ActivityInfo().apply {
                    name = TestProjectedDeviceActivity::class.java.name
                    packageName = context.packageName
                }
            )
        val activityScenario: ActivityScenario<TestProjectedDeviceActivity> =
            ActivityScenario.launch(Intent(context, TestProjectedDeviceActivity::class.java))
        activityScenario.onActivity { activity -> block(activity) }
    }

    private fun launchTestActivity(block: (Activity) -> Unit) {
        shadowOf(context.packageManager)
            .addOrUpdateActivity(
                ActivityInfo().apply {
                    name = TestActivity::class.java.name
                    packageName = context.packageName
                }
            )
        val activityScenario: ActivityScenario<TestActivity> =
            ActivityScenario.launch(Intent(context, TestActivity::class.java))
        activityScenario.onActivity { activity -> block(activity) }
    }

    companion object {
        private const val SYSTEM_PACKAGE_NAME = "com.system.service"
        private const val SYSTEM_CLASS_NAME = "com.system.service.ProjectedService"
        private const val ENGAGEMENT_MODE_SYSTEM_CLASS_NAME =
            "com.system.service.EngagementModeService"
        private val COMPONENT_NAME = ComponentName(SYSTEM_PACKAGE_NAME, SYSTEM_CLASS_NAME)

        private val ENGAGEMENT_MODE_SERVICE_COMPONENT =
            ComponentName(SYSTEM_PACKAGE_NAME, ENGAGEMENT_MODE_SYSTEM_CLASS_NAME)
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
    }
}

private class TestActivity : Activity()

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class)
private class TestProjectedDeviceActivity : Activity() {

    private lateinit var virtualDeviceManager: VirtualDeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        virtualDeviceManager =
            getSystemService(Context.VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager
        createVirtualDevice()
    }

    override fun getDeviceId(): Int {
        return virtualDeviceManager.virtualDevices.first().deviceId
    }

    private fun createVirtualDevice() {
        val virtualDeviceParamsBuilderClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams\$Builder")
        val virtualDeviceParamsClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams")
        var virtualDeviceParamsBuilder =
            ReflectionHelpers.callConstructor(virtualDeviceParamsBuilderClass)
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(
                virtualDeviceParamsBuilder,
                "setName",
                ClassParameter(String::class.java, PROJECTED_DEVICE_NAME),
            )
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(virtualDeviceParamsBuilder, "build")

        ReflectionHelpers.callInstanceMethod<Any?>(
            virtualDeviceManager,
            "createVirtualDevice",
            ClassParameter(Int::class.javaPrimitiveType, 1),
            ClassParameter(virtualDeviceParamsClass, virtualDeviceParamsBuilder),
        )
    }
}
