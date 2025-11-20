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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedContext.PROJECTED_DEVICE_NAME
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedServiceBinding.ACTION_BIND
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedService
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalProjectedApi::class)
class ProjectedDeviceControllerTest {

    private val mockProjectedService = mock<IProjectedService>()
    private val mockProjectedServiceStub =
        mock<IProjectedService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockProjectedService)
        }
    private val context: Application = ApplicationProvider.getApplicationContext()

    private lateinit var projectedDeviceController: ProjectedDeviceController

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
    fun create_bindingToServiceNotPermitted_throwsException() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            shadowOf(context).declareComponentUnbindable(COMPONENT_NAME)

            assertFailsWith<IllegalStateException> {
                runBlocking {
                    projectedDeviceController =
                        ProjectedDeviceController.create(projectedDeviceActivity)
                }
            }
        }

    @Test
    fun capabilities_serviceReturnsTrue_returnsCapabilityVisualUi() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            whenever(mockProjectedService.isDisplayCapable()).thenReturn(true)
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.capabilities).contains(CAPABILITY_VISUAL_UI)
        }

    @Test
    fun capabilities_serviceReturnsFalse_doesNotReturnCapabilityVisualUi() =
        launchTestProjectedDeviceActivity { projectedDeviceActivity ->
            whenever(mockProjectedService.isDisplayCapable()).thenReturn(false)
            runBlocking {
                projectedDeviceController =
                    ProjectedDeviceController.create(projectedDeviceActivity)
            }

            assertThat(projectedDeviceController.capabilities).doesNotContain(CAPABILITY_VISUAL_UI)
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
    }

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
}
