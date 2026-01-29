/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected.testing

import android.annotation.SuppressLint
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
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.xr.projected.ProjectedDeviceController.Capability
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.ProjectedLayoutParamsFlags
import androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IEngagementModeCallback
import androidx.xr.projected.platform.IEngagementModeService
import androidx.xr.projected.platform.IProjectedDeviceStateListener
import androidx.xr.projected.platform.IProjectedInputEventListener
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedDeviceState
import androidx.xr.projected.platform.ProjectedInputEvent
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.Executor
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowVirtualDeviceManager

/**
 * Test rule for the Projected clients.
 *
 * This rule sets up a virtual device and a virtual display using `VirtualDeviceManager` before each
 * test method and ensures they are properly torn down afterward. This allows testing of components
 * that interact with Projected devices.
 *
 * Usage:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class MyProjectedFeatureTest {
 *
 *     @get:Rule val projectedTestRule = ProjectedTestRule()
 *
 *     private val context: ContextWrapper = ApplicationProvider.getApplicationContext()
 *
 *     @Test
 *     fun testFeatureWithProjectedDevice() {
 *         // A Projected device is automatically created before this test.
 *         assertThat(ProjectedContext.createProjectedDeviceContext(context)).isNotNull()
 *     }
 *
 *     @Test
 *     fun testFeatureWithoutProjectedDevice() {
 *         // Manually disconnect the device for this specific test.
 *         projectedTestRule.isDeviceConnected = false
 *         // Test behavior when no projected device is connected.
 *         assertThrows(IllegalStateException::class.java) {
 *             assertThat(ProjectedContext.createProjectedDeviceContext(context)).isNull()
 *         }
 *     }
 * }
 * ```
 *
 * The Projected device is connected by default at the start of each test. The connection state can
 * be controlled via the [isDeviceConnected] property.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@ExperimentalProjectedApi
public class ProjectedTestRule : TestRule {

    /**
     * This property can be used to control whether the device is connected or not. By default, the
     * device is disconnected.
     */
    public var isDeviceConnected: Boolean = true
        set(value) {
            if (value) {
                createVirtualDevice()
            } else {
                ShadowVirtualDeviceManager.ShadowVirtualDevice.reset()
            }
            field = value
        }

    /**
     * Controls whether [androidx.xr.projected.ProjectedActivityCompat.create] or
     * [androidx.xr.projected.ProjectedDeviceController.create] throw an [IllegalStateException]
     * when called. By default, the exception is not being thrown.
     */
    public var throwIllegalStateExceptionWhenCreatingControllers: Boolean = false
        set(value) {
            if (value) {
                disableProjectedService()
                disableEngagementModeService()
            } else {
                enableProjectedService()
                enableEngagementModeService()
            }
            field = value
        }

    /**
     * This property can be used to control the Projected device capabilities. By default, the set
     * of capabilities includes the [Capability.CAPABILITY_VISUAL_UI].
     */
    public var capabilities: Set<Capability> = setOf(Capability.CAPABILITY_VISUAL_UI)
        set(value) {
            if (value.contains(Capability.CAPABILITY_VISUAL_UI)) {
                whenever(mockProjectedService.isDisplayCapable()).thenReturn(true)
            } else {
                whenever(mockProjectedService.isDisplayCapable()).thenReturn(false)
            }
            field = value
        }

    /**
     * Returns the currently set Projected layout param flags, reflecting the state after calls to
     * [androidx.xr.projected.ProjectedDisplayController.addLayoutParamsFlags] and
     * [androidx.xr.projected.ProjectedDisplayController.removeLayoutParamsFlags].
     */
    @ProjectedLayoutParamsFlags
    public var projectedLayoutParamFlags: Int = 0
        private set

    /**
     * This property can be used to control the
     * [androidx.xr.projected.ProjectedDisplayController.PresentationMode] flags. By default, all
     * the presentation mode flags are set.
     */
    public var presentationModeFlags: Set<ProjectedDisplayController.PresentationMode> =
        setOf(
            ProjectedDisplayController.PresentationMode.AUDIO_ON,
            ProjectedDisplayController.PresentationMode.VISUALS_ON,
        )
        set(value) {
            val callbackCaptor = argumentCaptor<IEngagementModeCallback>()
            verify(mockEngagementModeService).registerCallback(callbackCaptor.capture())

            var engagementModeFlags = 0
            if (value.contains(ProjectedDisplayController.PresentationMode.AUDIO_ON)) {
                engagementModeFlags =
                    engagementModeFlags or IEngagementModeService.ENGAGEMENT_STATE_FLAG_AUDIO_ON
            }
            if (value.contains(ProjectedDisplayController.PresentationMode.VISUALS_ON)) {
                engagementModeFlags =
                    engagementModeFlags or IEngagementModeService.ENGAGEMENT_STATE_FLAG_VISUALS_ON
            }

            callbackCaptor.firstValue.onEngagementModeChanged(engagementModeFlags)
            field = value
        }

    /**
     * This property can be used to control the lifecycle state of the Projected device. By default,
     * the Projected device is in the [Lifecycle.State.INITIALIZED] state.
     */
    public var lifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(value) {
            val projectedDeviceState =
                when (value) {
                    Lifecycle.State.INITIALIZED,
                    Lifecycle.State.CREATED -> ProjectedDeviceState.INACTIVE
                    Lifecycle.State.STARTED,
                    Lifecycle.State.RESUMED -> ProjectedDeviceState.ACTIVE
                    Lifecycle.State.DESTROYED -> ProjectedDeviceState.DESTROYED
                }
            val deviceStateListenerArgumentCaptor = argumentCaptor<IProjectedDeviceStateListener>()
            verify(mockProjectedService)
                .registerProjectedDeviceStateListener(deviceStateListenerArgumentCaptor.capture())
            deviceStateListenerArgumentCaptor.firstValue.onProjectedDeviceStateChanged(
                projectedDeviceState,
                /* data= */ null,
            )
            field = value
        }

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val virtualDeviceManager =
        context.getSystemService(Context.VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager
    private val mockProjectedService: IProjectedService =
        mock<IProjectedService> {
            on { addWindowFlags(any()) } doAnswer
                { invocation ->
                    val flag = invocation.arguments[0] as Int
                    projectedLayoutParamFlags = projectedLayoutParamFlags or flag
                }
            on { clearWindowFlags(any()) } doAnswer
                { invocation ->
                    val flag = invocation.arguments[0] as Int
                    projectedLayoutParamFlags = projectedLayoutParamFlags and flag.inv()
                }
        }
    private val mockProjectedServiceStub =
        mock<IProjectedService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockProjectedService)
        }
    private val mockEngagementModeService = mock<IEngagementModeService>()
    private val mockEngagementModeServiceStub =
        mock<IEngagementModeService.Stub> {
            on { queryLocalInterface(any()) }.thenReturn(mockEngagementModeService)
        }

    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            override fun evaluate() {
                throwIllegalStateExceptionWhenCreatingControllers = false
                isDeviceConnected = true
                capabilities = setOf(Capability.CAPABILITY_VISUAL_UI)
                base?.evaluate()
            }
        }

    /**
     * Emits a [ProjectedInputEvent] with the given [ProjectedInputAction] to
     * [androidx.xr.projected.ProjectedActivityCompat.projectedInputEvents].
     */
    public fun sendProjectedInputEvent(projectedInputAction: ProjectedInputAction) {
        val inputEventListenerCaptor =
            ArgumentCaptor.forClass(IProjectedInputEventListener::class.java)
        verify(mockProjectedService)
            .registerProjectedInputEventListener(inputEventListenerCaptor.capture())
        inputEventListenerCaptor.firstValue.onProjectedInputEvent(
            ProjectedInputEvent().apply { action = projectedInputAction.code }
        )
    }

    /**
     * Launches a test [Activity] on the Projected device.
     *
     * @param block The block to execute on the test activity.
     */
    public fun launchTestProjectedDeviceActivity(block: (Activity) -> Unit) {
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

    // TODO: b/476403759 - Replace reflection with the shadow APIs when they are available.
    @SuppressLint("BanUncheckedReflection")
    private fun createVirtualDevice() {
        try {
            // 1. Get Class objects
            val virtualDeviceParamsBuilderClass =
                Class.forName("android.companion.virtual.VirtualDeviceParams\$Builder")
            val virtualDeviceParamsClass =
                Class.forName("android.companion.virtual.VirtualDeviceParams")

            // 2. Call constructor for the Builder
            val builderConstructor: Constructor<*> =
                virtualDeviceParamsBuilderClass.getDeclaredConstructor()
            var virtualDeviceParamsBuilderInstance: Any? = builderConstructor.newInstance()

            // 3. Call setName method
            val setNameMethod: Method =
                virtualDeviceParamsBuilderClass.getMethod("setName", String::class.java)
            virtualDeviceParamsBuilderInstance =
                setNameMethod.invoke(virtualDeviceParamsBuilderInstance, PROJECTED_DEVICE_NAME)

            // 4. Call build method
            val buildMethod: Method = virtualDeviceParamsBuilderClass.getMethod("build")
            val virtualDeviceParamsInstance: Any? =
                buildMethod.invoke(virtualDeviceParamsBuilderInstance)

            // 5. Call virtualDeviceManager.createVirtualDevice
            val createVirtualDeviceMethod: Method =
                virtualDeviceManager::class
                    .java
                    .getMethod(
                        "createVirtualDevice",
                        Int::class
                            .javaPrimitiveType, // or Int::class.java for boxed, but primitiveType
                        // is more exact
                        virtualDeviceParamsClass,
                    )

            val virtualDevice: Any? =
                createVirtualDeviceMethod.invoke(
                    virtualDeviceManager,
                    ASSOCIATION_ID,
                    virtualDeviceParamsInstance,
                )

            createVirtualDisplayForDevice(virtualDevice!!)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("Reflection failed: Class not found", e)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Reflection failed: Method not found", e)
        } catch (e: Exception) {
            // Catch other reflection-related exceptions like IllegalAccessException,
            // InvocationTargetException, InstantiationException
            throw RuntimeException("Reflection failed", e)
        }
    }

    // TODO: b/476403759 - Replace reflection with the shadow APIs when they are available.
    @SuppressLint("BanUncheckedReflection")
    private fun createVirtualDisplayForDevice(virtualDevice: Any): VirtualDisplay {
        val virtualDisplayConfig =
            VirtualDisplayConfig.Builder(
                    PROJECTED_DISPLAY_NAME,
                    DISPLAY_WIDTH,
                    DISPLAY_HEIGHT,
                    DISPLAY_DENSITY,
                )
                .build()

        try {
            val method: Method =
                virtualDevice::class
                    .java
                    .getMethod(
                        "createVirtualDisplay",
                        VirtualDisplayConfig::class.java,
                        Executor::class.java,
                        VirtualDisplay.Callback::class.java,
                    )
            // Note: The cast to VirtualDisplay might be unsafe if the method returns null
            // or a different type. Consider adding checks.
            return method.invoke(virtualDevice, virtualDisplayConfig, null, null) as VirtualDisplay
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Failed to find createVirtualDisplay method", e)
        } catch (e: Exception) {
            throw RuntimeException("Failed to call createVirtualDisplay method", e)
        }
    }

    private fun enableProjectedService() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(PROJECTED_SERVICE_COMPONENT_NAME)
            addIntentFilterForService(
                PROJECTED_SERVICE_COMPONENT_NAME,
                IntentFilter(PROJECTED_ACTION_BIND),
            )
            installPackage(PROJECTED_PACKAGE_INFO)
        }

        shadowOf(context).apply {
            setComponentNameAndServiceForBindServiceForIntent(
                Intent().apply {
                    action = PROJECTED_ACTION_BIND
                    component = PROJECTED_SERVICE_COMPONENT_NAME
                },
                PROJECTED_SERVICE_COMPONENT_NAME,
                mockProjectedServiceStub,
            )
            setBindServiceCallsOnServiceConnectedDirectly(true)
        }
    }

    private fun disableProjectedService() {
        shadowOf(context.packageManager).apply {
            clearIntentFilterForService(PROJECTED_SERVICE_COMPONENT_NAME)
        }
    }

    private fun enableEngagementModeService() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(ENGAGEMENT_MODE_SERVICE_COMPONENT_NAME)
            addIntentFilterForService(
                ENGAGEMENT_MODE_SERVICE_COMPONENT_NAME,
                IntentFilter(ENGAGEMENT_MODE_ACTION_BIND),
            )
            installPackage(ENGAGEMENT_MODE_PACKAGE_INFO)
        }
        shadowOf(context).apply {
            setComponentNameAndServiceForBindServiceForIntent(
                Intent().apply {
                    action = ENGAGEMENT_MODE_ACTION_BIND
                    `package` = ENGAGEMENT_MODE_SYSTEM_PACKAGE_NAME
                },
                ENGAGEMENT_MODE_SERVICE_COMPONENT_NAME,
                mockEngagementModeServiceStub,
            )
            setBindServiceCallsOnServiceConnectedDirectly(true)
        }
    }

    private fun disableEngagementModeService() {
        shadowOf(context.packageManager).apply {
            clearIntentFilterForService(ENGAGEMENT_MODE_SERVICE_COMPONENT_NAME)
        }
    }

    private class TestProjectedDeviceActivity : Activity() {

        private lateinit var virtualDeviceManager: VirtualDeviceManager

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            virtualDeviceManager = getSystemService(VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager
            ProjectedTestRule().createVirtualDevice()
        }

        override fun getDeviceId(): Int {
            return virtualDeviceManager.virtualDevices.first().deviceId
        }
    }

    private companion object {
        private const val PROJECTED_DEVICE_NAME = "ProjectionDevice"
        private const val PROJECTED_DISPLAY_NAME = "ProjectionDisplay"
        private const val ASSOCIATION_ID = 1
        private const val DISPLAY_WIDTH = 10
        private const val DISPLAY_HEIGHT = 10
        private const val DISPLAY_DENSITY = 10

        private const val PROJECTED_ACTION_BIND = "androidx.xr.projected.ACTION_BIND"
        private const val ENGAGEMENT_MODE_ACTION_BIND: String =
            "androidx.xr.projected.ACTION_ENGAGEMENT_BIND"
        private const val PROJECTED_SERVICE_PACKAGE_NAME = "com.system.service"
        private const val ENGAGEMENT_MODE_SYSTEM_PACKAGE_NAME = "com.system.service1"
        private const val PROJECTED_SERVICE_CLASS_NAME = "com.system.service.ProjectedService"
        private const val ENGAGEMENT_MODE_SYSTEM_CLASS_NAME =
            "com.system.service.EngagementModeService"
        private val PROJECTED_SERVICE_COMPONENT_NAME: ComponentName =
            ComponentName(PROJECTED_SERVICE_PACKAGE_NAME, PROJECTED_SERVICE_CLASS_NAME)
        private val ENGAGEMENT_MODE_SERVICE_COMPONENT_NAME =
            ComponentName(ENGAGEMENT_MODE_SYSTEM_PACKAGE_NAME, ENGAGEMENT_MODE_SYSTEM_CLASS_NAME)
        private val PROJECTED_SERVICE_INFO =
            ServiceInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                name = PROJECTED_SERVICE_CLASS_NAME
            }
        private val ENGAGEMENT_MODE_SERVICE_INFO =
            ServiceInfo().apply {
                packageName = ENGAGEMENT_MODE_SYSTEM_PACKAGE_NAME
                name = ENGAGEMENT_MODE_SYSTEM_CLASS_NAME
            }
        private val PROJECTED_PACKAGE_INFO =
            PackageInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                services = arrayOf(PROJECTED_SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }

        private val ENGAGEMENT_MODE_PACKAGE_INFO =
            PackageInfo().apply {
                packageName = ENGAGEMENT_MODE_SYSTEM_PACKAGE_NAME
                services = arrayOf(ENGAGEMENT_MODE_SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
    }
}
