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
import android.app.Application
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.core.app.ApplicationProvider
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.Executor
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
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
public class ProjectedTestRule : TestRule {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val virtualDeviceManager =
        context.getSystemService(Context.VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager

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

    override fun apply(base: Statement?, description: Description?): Statement =
        object : Statement() {
            override fun evaluate() {
                isDeviceConnected = true
                base?.evaluate()
            }
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

    private companion object {
        private const val PROJECTED_DEVICE_NAME = "ProjectionDevice"
        private const val PROJECTED_DISPLAY_NAME = "ProjectionDisplay"
        private const val ASSOCIATION_ID = 1
        private const val DISPLAY_WIDTH = 10
        private const val DISPLAY_HEIGHT = 10
        private const val DISPLAY_DENSITY = 10
    }
}
