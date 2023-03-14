/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.area

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.window.core.BuildConfig
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.VerificationMode
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.area.WindowAreaComponent
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide information about available window areas on the device and an option
 * to use the rear display area of a foldable device, exclusively or concurrently with the internal
 * display.
 */
@ExperimentalWindowApi
interface WindowAreaController {

    /**
     * Provides information about the current state of the window area of the rear display on the
     * device, if or when it is available. Rear Display mode can be invoked if the current status is
     * [WindowAreaStatus.AVAILABLE].
     */
    fun rearDisplayStatus(): Flow<WindowAreaStatus>

    /**
     * Starts Rear Display Mode and moves the provided activity to the rear side of the device in
     * order to face the same direction as the primary device camera(s). When a rear display
     * mode is started, the system will turn on the rear display of the device to show the content
     * there, and can disable the internal display. The provided [Activity] is likely to get a
     * configuration change or being relaunched due to the difference in the internal and rear
     * display sizes on the device.
     * <p>Only the top visible application can request and use this mode. The system can dismiss the
     * mode if the user changes the device state.
     * <p>This method can only be called if the feature is supported on the device and is reported
     * as available in the current state through [rearDisplayStatus], otherwise it will
     * throw an [Exception].
     */
    fun rearDisplayMode(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    )

    public companion object {
        private val TAG = WindowAreaController::class.simpleName

        private var decorator: WindowAreaControllerDecorator = EmptyDecorator

        /**
         * Provides an instance of [WindowAreaController].
         */
        @JvmName("getOrCreate")
        @JvmStatic
        fun getOrCreate(): WindowAreaController {
            var windowAreaComponentExtensions: WindowAreaComponent?
            try {
                // TODO(b/267972002): Introduce reflection guard for WindowAreaComponent
                windowAreaComponentExtensions = WindowExtensionsProvider
                    .getWindowExtensions()
                    .windowAreaComponent
            } catch (t: Throwable) {
                if (BuildConfig.verificationMode == VerificationMode.STRICT) {
                    Log.d(TAG, "Failed to load WindowExtensions")
                }
                windowAreaComponentExtensions = null
            }
            val controller =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                    windowAreaComponentExtensions == null) {
                    EmptyWindowAreaControllerImpl()
                } else {
                    WindowAreaControllerImpl(windowAreaComponentExtensions)
                }
            return decorator.decorate(controller)
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun overrideDecorator(overridingDecorator: WindowAreaControllerDecorator) {
            decorator = overridingDecorator
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun reset() {
            decorator = EmptyDecorator
        }
    }
}

/**
 * Decorator that allows us to provide different functionality
 * in our window-testing artifact.
 */
@ExperimentalWindowApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface WindowAreaControllerDecorator {
    /**
     * Returns an instance of [WindowAreaController] associated to the [Activity]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun decorate(controller: WindowAreaController): WindowAreaController
}

@ExperimentalWindowApi
private object EmptyDecorator : WindowAreaControllerDecorator {
    override fun decorate(controller: WindowAreaController): WindowAreaController {
        return controller
    }
}
