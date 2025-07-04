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
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.projected.ProjectedContext.PROJECTED_DEVICE_NAME
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@RunWith(AndroidJUnit4::class)
class ProjectedContextTest {

    val context: ContextWrapper = ApplicationProvider.getApplicationContext()
    val virtualDeviceManager =
        context.getSystemService(Context.VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager

    val projectedDeviceContext: Context
        get() = LocalContextWrapper(context, virtualDeviceManager.virtualDevices.first().deviceId)

    @Test
    fun createProjectedDeviceContext_hasVirtualDevice_returnsContext() {
        createVirtualDevice()

        assertThat(ProjectedContext.createProjectedDeviceContext(context)).isNotNull()
    }

    @Test
    fun createProjectedDeviceContext_noVirtualDevice_throwsIllegalStateException() {
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
        createVirtualDevice()

        assertThat(ProjectedContext.getProjectedDeviceName(projectedDeviceContext))
            .isEqualTo(PROJECTED_DEVICE_NAME)
    }

    @Test
    fun getProjectedDeviceName_anotherContext_throwsIllegalArgumentException() {
        createVirtualDevice()

        assertThrows(IllegalArgumentException::class.java) {
            ProjectedContext.getProjectedDeviceName(context)
        }
    }

    @Test
    fun isProjectedDeviceContext_returnsTrue() {
        createVirtualDevice()

        assertThat(ProjectedContext.isProjectedDeviceContext(projectedDeviceContext)).isTrue()
    }

    @Test
    fun isProjectedDeviceContext_returnsFalse() {
        createVirtualDevice()

        assertThat(ProjectedContext.isProjectedDeviceContext(context)).isFalse()
    }

    @Test
    fun addProjectedFlags_returnsIntentWithAddedFlags() {
        val intent = Intent().setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        val expectedFlags = intent.flags or ProjectedContext.REQUIRED_LAUNCH_FLAGS

        ProjectedContext.addProjectedFlags(intent)

        assertThat(intent.flags).isEqualTo(expectedFlags)
    }

    @Test
    @Ignore // Bring back this test once a new Robolectric version is available
    fun createProjectedActivityOptions_projectedDeviceContext_returnsActivityOptionsWithLaunchDisplayId() {
        createVirtualDevice()

        val activityOptions = ProjectedContext.createProjectedActivityOptions(context)

        assertThat(activityOptions.launchDisplayId).isEqualTo(DISPLAY_ID)
    }

    @Test
    fun createProjectedActivityOptions_anotherContext_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectedContext.createProjectedActivityOptions(context)
        }
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

    companion object {
        private const val DISPLAY_ID = 5
    }

    class LocalContextWrapper(context: Context, private val deviceId: Int) :
        ContextWrapper(context) {
        override fun getDeviceId() = deviceId
    }
}
