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

package androidx.xr.runtime.testing

import android.companion.virtual.VirtualDeviceManager
import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.SpatialApiVersion
import androidx.xr.runtime.SpatialApiVersionHelper
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.testing.internal.FakeSpatialApiVersionProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProvider
import androidx.xr.runtime.testing.internal.FakeXrDeviceCapabilityProviderFactory
import androidx.xr.runtime.toInternalDepthEstimationMode
import androidx.xr.runtime.toInternalEyeTrackingMode
import androidx.xr.runtime.toInternalGeospatialMode
import androidx.xr.runtime.toInternalHandTrackingMode
import androidx.xr.runtime.toInternalRenderingMode
import org.junit.rules.ExternalResource
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowVirtualDeviceManager
import org.robolectric.shadows.ShadowVirtualDeviceManager.ShadowVirtualDevice
import org.robolectric.shadows.VirtualDeviceParamsBuilder

/** JUnit Rule containing properties that affect the results of [XrDevice] capability APIs. */
public class XrDeviceTestRule : ExternalResource() {
    internal var capabilityProvider: FakeXrDeviceCapabilityProvider? = null
        set(value) {
            field = value
            attemptToRunCapabilityProviderTasks()
        }

    private val capabilityProviderTaskList: MutableList<() -> Unit> = mutableListOf()

    private fun attemptToRunCapabilityProviderTasks() {
        if (capabilityProvider != null) {
            capabilityProviderTaskList.forEach { task -> task.invoke() }
            capabilityProviderTaskList.clear()
        }
    }

    internal var spatialApiVersionProvider: FakeSpatialApiVersionProvider? = null

    /**
     * The result of [XrDevice.getPreferredDisplayBlendMode].
     *
     * Tests can set this property to control the value returned by
     * [XrDevice.getPreferredDisplayBlendMode] during the test execution.
     */
    public var preferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.ALPHA_BLEND
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.preferredDisplayBlendMode = value.toInternal()
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The set of [HandTrackingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isHandTrackingModeSupported] during the test execution.
     */
    public var supportedHandTrackingModes: Set<HandTrackingMode> = setOf(HandTrackingMode.DISABLED)
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.let { capabilityProvider ->
                    capabilityProvider.supportedHandTrackingModes.clear()
                    value.forEach { mode ->
                        capabilityProvider.supportedHandTrackingModes.add(
                            mode.toInternalHandTrackingMode()
                        )
                    }
                }
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The set of [EyeTrackingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isEyeTrackingModeSupported] during the test execution.
     */
    public var supportedEyeTrackingModes: Set<EyeTrackingMode> = setOf(EyeTrackingMode.DISABLED)
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.let { capabilityProvider ->
                    capabilityProvider.supportedEyeTrackingModes.clear()
                    value.forEach { mode ->
                        capabilityProvider.supportedEyeTrackingModes.add(
                            mode.toInternalEyeTrackingMode()
                        )
                    }
                }
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The set of [DepthEstimationMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isDepthEstimationModeSupported] during the test execution.
     */
    public var supportedDepthEstimationModes: Set<DepthEstimationMode> =
        setOf(DepthEstimationMode.DISABLED)
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.let { capabilityProvider ->
                    capabilityProvider.supportedDepthEstimationModes.clear()
                    value.forEach { mode ->
                        capabilityProvider.supportedDepthEstimationModes.add(
                            mode.toInternalDepthEstimationMode()
                        )
                    }
                }
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The set of [GeospatialMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isGeospatialModeSupported] during the test execution.
     */
    public var supportedGeospatialModes: Set<GeospatialMode> = setOf(GeospatialMode.DISABLED)
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.let { capabilityProvider ->
                    capabilityProvider.supportedGeospatialModes.clear()
                    value.forEach { mode ->
                        capabilityProvider.supportedGeospatialModes.add(
                            mode.toInternalGeospatialMode()
                        )
                    }
                }
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The set of [RenderingMode] instances supported by the [XrDevice].
     *
     * Tests can modify this set to control the value returned by
     * [XrDevice.isRenderingModeSupported] during the test execution.
     */
    public var supportedRenderingModes: Set<RenderingMode> = setOf()
        set(value) {
            field = value
            capabilityProviderTaskList.add {
                capabilityProvider?.let { capabilityProvider ->
                    capabilityProvider.supportedRenderingModes.clear()
                    value.forEach { mode ->
                        capabilityProvider.supportedRenderingModes.add(
                            mode.toInternalRenderingMode()
                        )
                    }
                }
            }
            attemptToRunCapabilityProviderTasks()
        }

    /**
     * The value to be returned by [SpatialApiVersionHelper.spatialApiVersion].
     *
     * Tests can set this property to control the value returned by
     * [SpatialApiVersionHelper.spatialApiVersion] during the test execution. By default the value
     * is set to the latest stable API level.
     */
    @SpatialApiVersion
    public var spatialApiVersion: Int = SpatialApiVersions.UNKNOWN
        set(@SpatialApiVersion value) {
            spatialApiVersionProvider?.spatialApiVersion = value
            field = value
        }

    /**
     * This property can be used to control whether the Projected service is available or not. By
     * default, the Projected service is available.
     */
    public var isProjectedServiceAvailable: Boolean = true
        set(value) {
            shadowOf(context.packageManager).setSystemFeature(XR_PROJECTED_SYSTEM_FEATURE, value)
            if (value) {
                enableProjectedService()
            } else {
                disableProjectedService()
            }
            field = value
        }

    /**
     * Tests can set this property to control the lifecycle state of the [XrDevice]. By default, the
     * value is set to [Lifecycle.State.INITIALIZED].
     */
    public var lifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(value) {
            field = value
            capabilityProviderTaskList.add { capabilityProvider?.lifecycleState = value }
            attemptToRunCapabilityProviderTasks()
        }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val createdDevices = mutableListOf<CreatedDevice>()

    /**
     * Creates a simulated device that is connected to the host device and returns a [TestXrDevice]
     * for it.
     *
     * Use [TestXrDevice.context] to get the [XrDevice] of the simulated device:
     * ```
     * val testDevice = xrDeviceTestRule.createTestXrDevice()
     * val device = XrDevice.getCurrentDevice(testDevice.context)
     * ```
     *
     * Each call creates a new device.
     *
     * The simulated device is backed by a virtual device and a virtual display, which are created
     * through Robolectric's [VirtualDeviceManager] shadow. They are also visible to other Android
     * APIs for the rest of the test, such as [VirtualDeviceManager.getVirtualDevices] and
     * [DisplayManager.getDisplays]. The rule closes the virtual device and releases its display
     * when the test finishes.
     *
     * This method requires API 34 for usage of [VirtualDeviceManager].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public fun createTestXrDevice(): TestXrDevice {
        val virtualDeviceManager = context.getSystemService(VirtualDeviceManager::class.java)
        val deviceName = "$DEVICE_NAME${createdDevices.size + 1}"
        val device =
            Shadow.extract<ShadowVirtualDevice>(
                Shadow.extract<ShadowVirtualDeviceManager>(virtualDeviceManager)
                    .createVirtualDevice(
                        ASSOCIATION_ID,
                        VirtualDeviceParamsBuilder.newBuilder().setName(deviceName).build(),
                    )
            )
        val display =
            device.createVirtualDisplay(
                VirtualDisplayConfig.Builder(
                        DISPLAY_NAME,
                        DISPLAY_WIDTH,
                        DISPLAY_HEIGHT,
                        DISPLAY_DENSITY,
                    )
                    .build(),
                /* executor= */ null,
                /* callback= */ null,
            )
        createdDevices.add(CreatedDevice(device, display))
        val deviceId = virtualDeviceManager.virtualDevices.single { it.name == deviceName }.deviceId
        return TestXrDevice(context.createDeviceContext(deviceId), display.display.displayId)
    }

    private fun closeCreatedDevices() {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        createdDevices.forEach { createdDevice ->
            // ShadowDisplayManager.removeDisplay() removes the display from DisplayManager
            // without updating VirtualDisplay, which would cause release() to throw.
            if (displayManager.getDisplay(createdDevice.display.display.displayId) != null) {
                createdDevice.display.release()
            }
            createdDevice.device.close()
        }
        createdDevices.clear()
    }

    private class CreatedDevice(val device: ShadowVirtualDevice, val display: VirtualDisplay)

    internal fun DisplayBlendMode.toInternal() =
        when (this) {
            DisplayBlendMode.ALPHA_BLEND ->
                androidx.xr.runtime.interfaces.DisplayBlendMode.ALPHA_BLEND
            DisplayBlendMode.ADDITIVE -> androidx.xr.runtime.interfaces.DisplayBlendMode.ADDITIVE
            else -> androidx.xr.runtime.interfaces.DisplayBlendMode.NO_DISPLAY
        }

    init {
        // Force SpatialApiVersionHelper to load FakeSpatialApiVersionProvider
        SpatialApiVersionHelper.spatialApiVersion
    }

    override fun before() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = this

        FakeSpatialApiVersionProvider.xrDeviceTestRule = this
        FakeSpatialApiVersionProvider.instance?.registerProvider()
        spatialApiVersion =
            spatialApiVersionProvider?.spatialApiVersion ?: SpatialApiVersions.UNKNOWN
        isProjectedServiceAvailable = true
        lifecycleState = Lifecycle.State.INITIALIZED
    }

    override fun after() {
        FakeXrDeviceCapabilityProviderFactory.xrDeviceTestRule = null
        FakeSpatialApiVersionProvider.xrDeviceTestRule = null
        closeCreatedDevices()
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
    }

    private fun disableProjectedService() {
        shadowOf(context.packageManager).apply {
            clearIntentFilterForService(PROJECTED_SERVICE_COMPONENT_NAME)
        }
    }

    private companion object {
        // The names of the virtual devices and displays that simulate connected devices.
        private const val DEVICE_NAME = "TestXrDevice"
        private const val DISPLAY_NAME = "TestXrDeviceDisplay"
        private const val ASSOCIATION_ID = 1
        private const val DISPLAY_WIDTH = 640
        private const val DISPLAY_HEIGHT = 480
        private const val DISPLAY_DENSITY = 160
        private const val XR_PROJECTED_SYSTEM_FEATURE = "com.google.android.feature.XR_PROJECTED"
        private const val PROJECTED_ACTION_BIND = "androidx.xr.projected.ACTION_BIND"
        private const val PROJECTED_SERVICE_PACKAGE_NAME = "com.system.service"
        private const val PROJECTED_SERVICE_CLASS_NAME = "com.system.service.ProjectedService"
        private val PROJECTED_SERVICE_COMPONENT_NAME: ComponentName =
            ComponentName(PROJECTED_SERVICE_PACKAGE_NAME, PROJECTED_SERVICE_CLASS_NAME)
        private val PROJECTED_SERVICE_INFO =
            ServiceInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                name = PROJECTED_SERVICE_CLASS_NAME
            }
        private val PROJECTED_PACKAGE_INFO =
            PackageInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                services = arrayOf(PROJECTED_SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
    }
}
