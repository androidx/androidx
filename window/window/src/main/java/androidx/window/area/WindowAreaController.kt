/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.window.core.SpecificationComputer
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.area.WindowAreaComponent
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executor
import kotlin.jvm.Throws

/**
 * An interface to provide the information and behavior around moving windows between
 * displays or display areas on a device.
 */
@ExperimentalWindowApi
interface WindowAreaController {

    /*
    Marked with RestrictTo as we iterate and define the
    Kotlin API we want to provide.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun rearDisplayStatus(): Flow<WindowAreaStatus>

    /*
    Marked with RestrictTo as we iterate and define the
    Kotlin API we want to provide.
     */
    @Throws(UnsupportedOperationException::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun rearDisplayMode(
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    )

    public companion object {
        internal val REAR_DISPLAY_ERROR =
            UnsupportedOperationException("Rear Display mode cannot be enabled currently")

        private val TAG = WindowAreaController::class.simpleName

        private var decorator: WindowAreaControllerDecorator = EmptyDecorator

        /**
         * Provides an instance of [WindowAreaController].
         */
        @JvmName("getOrCreate")
        @JvmStatic
        public fun getOrCreate(): WindowAreaController {
            var windowAreaComponentExtensions: WindowAreaComponent?
            try {
                windowAreaComponentExtensions = WindowExtensionsProvider
                    .getWindowExtensions()
                    .windowAreaComponent
            } catch (t: Throwable) {
                if (BuildConfig.verificationMode == SpecificationComputer.VerificationMode.STRICT) {
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
        public fun overrideDecorator(overridingDecorator: WindowAreaControllerDecorator) {
            decorator = overridingDecorator
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun reset() {
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
public interface WindowAreaControllerDecorator {
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
