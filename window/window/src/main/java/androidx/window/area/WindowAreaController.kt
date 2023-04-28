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
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.window.area.WindowAreaInfo.Type.Companion.TYPE_REAR_FACING
import androidx.window.core.BuildConfig
import androidx.window.core.VerificationMode
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.area.WindowAreaComponent
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow

/**
 * An interface to provide the information and behavior around moving windows between
 * displays or display areas on a device.
 *
 */
interface WindowAreaController {

    /**
     * [Flow] of the list of current [WindowAreaInfo]s that are currently available to be interacted
     * with.
     */
    val windowAreaInfos: Flow<List<WindowAreaInfo>>

    /**
     * Starts a transfer session where the calling [Activity] is moved to the window area identified
     * by the [token]. Updates on the session are provided through the [WindowAreaSessionCallback].
     * Attempting to start a transfer session when the [WindowAreaInfo] does not return
     * [WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE] will result in
     * [WindowAreaSessionCallback.onSessionEnded] containing an [IllegalStateException]
     *
     * Only the top visible application can request to start a transfer session.
     *
     * The calling [Activity] will likely go through a configuration change since the window area
     * it will be transferred to is usually different from the current area the [Activity] is in.
     * The callback is retained during the lifetime of the session. If an [Activity] is captured in
     * the callback and it does not handle the configuration change then it will be leaked. Consider
     * using an [androidx.lifecycle.ViewModel] since that is meant to outlive the [Activity]
     * lifecycle. If the [Activity] does override configuration changes, it is safe to have the
     * [Activity] handle the WindowAreaSessionCallback. This guarantees that the calling [Activity]
     * will continue to receive [WindowAreaSessionCallback.onSessionEnded] and keep a handle to the
     * [WindowAreaSession] provided through [WindowAreaSessionCallback.onSessionStarted].
     *
     * The [windowAreaSessionCallback] provided will receive a call to
     * [WindowAreaSessionCallback.onSessionStarted] after the [Activity] has been transferred to the
     * window area. The transfer session will stay active until the session provided through
     * [WindowAreaSessionCallback.onSessionStarted] is closed. Depending on the
     * [WindowAreaInfo.Type] there may be other triggers that end the session, such as if a device
     * state change makes the window area unavailable. One example of this is if the [Activity] is
     * currently transferred to the [TYPE_REAR_FACING] window area of a foldable device, the session
     * will be ended when the device is closed. When this occurs,
     * [WindowAreaSessionCallback.onSessionEnded] is called.
     *
     * @param token [Binder] token identifying the window area to be transferred to.
     * @param activity Base Activity making the call to [transferActivityToWindowArea].
     * @param executor Executor used to provide updates to [windowAreaSessionCallback].
     * @param windowAreaSessionCallback to be notified when the rear display session is started and
     * ended.
     *
     * @see windowAreaInfos
     */
    fun transferActivityToWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        // TODO(272064992) investigate how to make this safer from leaks
        windowAreaSessionCallback: WindowAreaSessionCallback
    )

    /**
     * Starts a presentation session on the [WindowAreaInfo] identified by the [token] and sends
     * updates through the [WindowAreaPresentationSessionCallback].
     *
     * If a presentation session is attempted to be started without it being available,
     * [WindowAreaPresentationSessionCallback.onSessionEnded] will be called immediately with an
     * [IllegalStateException].
     *
     * Only the top visible application can request to start a presentation session.
     *
     * The presentation session will stay active until the presentation provided through
     * [WindowAreaPresentationSessionCallback.onSessionStarted] is closed. The [WindowAreaInfo.Type]
     * may provide different triggers to close the session such as if the calling application
     * is no longer in the foreground, or there is a device state change that makes the window area
     * unavailable to be presented on. One example scenario is if a [TYPE_REAR_FACING] window area
     * is being presented to on a foldable device that is open and has 2 screens. If the device is
     * closed and the internal display is turned off, the session would be ended and
     * [WindowAreaPresentationSessionCallback.onSessionEnded] is called to notify that the session
     * has been ended. The session may end prematurely if the device gets to a critical thermal
     * level, or if power saver mode is enabled.
     *
     * @param token [Binder] token to identify which [WindowAreaInfo] is to be presented on
     * @param activity An [Activity] that will present content on the Rear Display.
     * @param executor Executor used to provide updates to [windowAreaPresentationSessionCallback].
     * @param windowAreaPresentationSessionCallback to be notified of updates to the lifecycle of
     * the currently enabled rear display presentation.
     * @see windowAreaInfos
     */
    fun presentContentOnWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback
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
            var vendorApiLevel: Int = -1
            try {
                val windowExtensions = WindowExtensionsProvider.getWindowExtensions()
                vendorApiLevel = windowExtensions.vendorApiLevel
                windowAreaComponentExtensions = windowExtensions.windowAreaComponent
            } catch (t: Throwable) {
                if (BuildConfig.verificationMode == VerificationMode.LOG) {
                    Log.d(TAG, "Failed to load WindowExtensions")
                }
                windowAreaComponentExtensions = null
            }
            val controller =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    windowAreaComponentExtensions == null) {
                    EmptyWindowAreaControllerImpl()
                } else {
                    WindowAreaControllerImpl(windowAreaComponentExtensions, vendorApiLevel)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface WindowAreaControllerDecorator {
    /**
     * Returns an instance of [WindowAreaController] associated to the [Activity]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun decorate(controller: WindowAreaController): WindowAreaController
}

private object EmptyDecorator : WindowAreaControllerDecorator {
    override fun decorate(controller: WindowAreaController): WindowAreaController {
        return controller
    }
}
