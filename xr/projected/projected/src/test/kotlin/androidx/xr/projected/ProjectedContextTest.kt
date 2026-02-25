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

import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedContext.PROJECTED_DEVICE_NAME
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowVirtualDeviceManager
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalProjectedApi::class)
class ProjectedContextTest {

    @get:Rule() val projectedTestRule = ProjectedTestRule()

    val context: ContextWrapper = ApplicationProvider.getApplicationContext()
    val virtualDeviceManager =
        context.getSystemService(Context.VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager

    val projectedDeviceContext: Context
        get() = LocalContextWrapper(context, virtualDeviceManager.virtualDevices.first().deviceId)

    @Test
    fun createProjectedDeviceContext_hasVirtualDevice_returnsContext() {
        assertThat(ProjectedContext.createProjectedDeviceContext(context)).isNotNull()
    }

    @Test
    fun createProjectedDeviceContext_noVirtualDevice_throwsIllegalStateException() {
        projectedTestRule.isDeviceConnected = false

        assertThrows(IllegalStateException::class.java) {
            assertThat(ProjectedContext.createProjectedDeviceContext(context)).isNull()
        }
    }

    @Test
    fun createHostDeviceContext_returnsContext() {
        assertThat(ProjectedContext.createHostDeviceContext(context).deviceId)
            .isEqualTo(Context.DEVICE_ID_DEFAULT)
    }

    @Test
    fun getProjectedDeviceName_projectedDeviceContext_returnsName() {
        assertThat(ProjectedContext.getProjectedDeviceName(projectedDeviceContext))
            .isEqualTo(PROJECTED_DEVICE_NAME)
    }

    @Test
    fun getProjectedDeviceName_anotherContext_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectedContext.getProjectedDeviceName(context)
        }
    }

    @Test
    fun isProjectedDeviceContext_returnsTrue() {
        assertThat(ProjectedContext.isProjectedDeviceContext(projectedDeviceContext)).isTrue()
    }

    @Test
    fun isProjectedDeviceContext_returnsFalse() {
        assertThat(ProjectedContext.isProjectedDeviceContext(context)).isFalse()
    }

    @Test
    fun createProjectedActivityOptions_projectedDisplayAvailable_projectedDeviceContext_returnsActivityOptionsWithLaunchDisplayId() {
        val activityOptions =
            ProjectedContext.createProjectedActivityOptions(projectedDeviceContext)

        assertThat(activityOptions.launchDisplayId).isEqualTo(1)
    }

    @Test
    fun createProjectedActivityOptions_projectedDisplayAvailable_anotherContext_returnsActivityOptionsWithLaunchDisplayId() {
        val activityOptions = ProjectedContext.createProjectedActivityOptions(context)

        assertThat(activityOptions.launchDisplayId).isEqualTo(1)
    }

    @Test
    fun createProjectedActivityOptions_projectedDisplayUnavailable_throwsIllegalStateException() {
        ShadowDisplayManager.removeDisplay(
            virtualDeviceManager.virtualDevices.first().displayIds[0]
        )

        assertThrows(IllegalStateException::class.java) {
            ProjectedContext.createProjectedActivityOptions(context)
        }
    }

    @Test
    fun createProjectedActivityOptions_projectedDisplayDoesNotBelongToProjectedDevice_throwsIllegalStateException() {
        ShadowDisplayManager.removeDisplay(
            virtualDeviceManager.virtualDevices.first().displayIds[0]
        )

        // Create a display with the Projected display name that belongs to another device.
        ShadowDisplayManager.addDisplay("", ProjectedContext.PROJECTED_DISPLAY_NAME)

        assertThrows(IllegalStateException::class.java) {
            ProjectedContext.createProjectedActivityOptions(context)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun isProjectedDeviceConnected_projectedDeviceCreated_isTrue() = runBlocking {
        assertThat(ProjectedContext.isProjectedDeviceConnected(context, coroutineContext).first())
            .isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun isProjectedDeviceConnected_displayAddedLater_isTrue() = runBlocking {
        val virtualDevice = createVirtualDevice()
        val flow = ProjectedContext.isProjectedDeviceConnected(context, coroutineContext)
        createVirtualDisplayForDevice(virtualDevice)

        assertThat(flow.first()).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun isProjectedDeviceConnected_displayRemoved_isFalse() = runBlocking {
        val flow = ProjectedContext.isProjectedDeviceConnected(context, coroutineContext)
        assertThat(flow.first()).isTrue()

        ShadowDisplayManager.removeDisplay(
            virtualDeviceManager.virtualDevices.first().displayIds[0]
        )

        assertThat(flow.first()).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun isProjectedDeviceConnected_projectedDeviceNotCreated_isFalse() = runBlocking {
        projectedTestRule.isDeviceConnected = false

        assertThat(ProjectedContext.isProjectedDeviceConnected(context, coroutineContext).first())
            .isFalse()
    }

    // TODO: b/476403759 - Replace reflection with the shadow APIs when they are available.
    private fun createVirtualDevice(): Any? {
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
        val virtualDevice =
            ReflectionHelpers.callInstanceMethod<Any?>(
                virtualDeviceManager,
                "createVirtualDevice",
                ClassParameter(Int::class.javaPrimitiveType, 1),
                ClassParameter(virtualDeviceParamsClass, virtualDeviceParamsBuilder),
            )

        return virtualDevice
    }

    class LocalContextWrapper(context: Context, private val deviceId: Int) :
        ContextWrapper(context) {
        override fun getDeviceId() = deviceId
    }

    private fun createVirtualDisplayForDevice(virtualDevice: Any?): VirtualDisplay =
        Shadow.extract<ShadowVirtualDeviceManager.ShadowVirtualDevice>(virtualDevice)
            .createVirtualDisplay(
                VirtualDisplayConfig.Builder(
                        ProjectedContext.PROJECTED_DISPLAY_NAME,
                        /* width = */ 10,
                        /* height = */ 10,
                        /* densityDpi = */ 10,
                    )
                    .build(),
                /* executor= */ null,
                /* callback= */ null,
            )
}
