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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.xr.projected.platform.IProjectedService

/**
 * Class providing window functionality on the Projected device.
 *
 * This class provides a way to connect to and communicate with the ProjectedWindow.
 *
 * Use [create] to create an instance of this class. Use [close] to clear the instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProjectedWindow
private constructor(private val connection: ProjectedServiceConnection) : AutoCloseable {

    private lateinit var projectedService: IProjectedService

    /**
     * Convenience function to set the flag bits as as per the
     * [android.view.WindowManager.LayoutParams] flags. Flags will be cleared automatically after
     * the app stops.
     *
     * Supported flags:
     * - [android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
     *
     * If an unsupported flag is passed, this method does nothing.
     */
    public fun addFlags(flags: Int) {
        projectedService.addWindowFlags(flags)
    }

    /**
     * Convenience function to clear the flag bits as as per the
     * [android.view.WindowManager.LayoutParams] flags.
     *
     * Supported flags:
     * - [android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
     *
     * If an unsupported flag is passed, this method does nothing.
     */
    public fun clearFlags(flags: Int) {
        projectedService.clearWindowFlags(flags)
    }

    /**
     * Returns `true` if the currently connected projected device is capable of displaying content
     * to the user.
     *
     * This method does not provide any more details on what type of display it is.
     */
    public fun isDisplayCapable(): Boolean = projectedService.isDisplayCapable()

    /**
     * Disconnects from the service providing features for Projected devices. Methods from the
     * [ProjectedWindow] shouldn't be called after this.
     *
     * This method should be called in [android.app.Activity.onDestroy].
     */
    override fun close() {
        connection.disconnect()
    }

    private suspend fun initialize() {
        projectedService = connection.connect()
    }

    public companion object {
        /**
         * Connects to the service providing features for Projected devices and returns the
         * [ProjectedWindow] when the connection is established.
         *
         * @param activity The [Activity] running on a Projected device.
         * @throws IllegalStateException if the projected service is not found or binding is not
         *   permitted
         * @throws IllegalArgumentException if provided Activity is not running on a Projected
         *   device.
         */
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JvmStatic
        public suspend fun create(activity: Activity): ProjectedWindow {
            require(
                ProjectedContext.isProjectedDeviceContext(activity),
                { "Provided Activity is not running on a Projected device." },
            )

            return ProjectedWindow(ProjectedServiceConnection(activity)).apply { initialize() }
        }
    }
}
