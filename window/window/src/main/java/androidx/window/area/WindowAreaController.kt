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
import androidx.core.util.Consumer
import androidx.window.area.WindowArea.Type.Companion.TYPE_REAR_FACING
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.core.BuildConfig
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.ExtensionsUtil
import androidx.window.core.VerificationMode
import java.util.concurrent.Executor

/**
 * An interface to provide the information and behavior around moving windows between displays or
 * display areas on a device.
 */
public abstract class WindowAreaController
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor() {

    /**
     * Registers a listener that is interested in the current list of [WindowArea] available to be
     * interacted with.
     *
     * The [listener] will receive an initial value on registration, as soon as it becomes
     * available.
     *
     * @param executor to handle sending listener updates.
     * @param listener to receive updates to the list of [WindowArea].
     * @see WindowAreaController.transferToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    public abstract fun addWindowAreasListener(
        executor: Executor,
        listener: Consumer<List<WindowArea>>,
    )

    /**
     * Removes a listener of available [WindowArea] records. If the listener is not present then
     * this method is a no-op.
     *
     * @param listener to remove from receiving status updates.
     * @see WindowAreaController.transferToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    public abstract fun removeWindowAreasListener(listener: Consumer<List<WindowArea>>)

    /**
     * Moves the calling [Activity] and the global state of the device to the [WindowArea] provided.
     * This is a long-lasting and sticky operation that will outlive the application that requests
     * this operation. Status updates can be received from the value received by registering a
     * listener through [addWindowAreasListener] and then querying for the
     * [WindowAreaCapability.Operation.OPERATION_TRANSFER_TO_AREA] operation.
     *
     * Attempting to move the device to a window area when the [WindowArea] does not return
     * [WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE] will result in an
     * [IllegalStateException].
     *
     * Only the top visible application can request the device ot move to a [WindowArea]. If this
     * operation is requested when the application is not the top level process, a
     * [SecurityException] will be thrown.
     *
     * Passing a null [WindowAreaToken] returns back to the default window area (usually going to be
     * the [android.view.Display.DEFAULT_DISPLAY]). Depending on the [WindowArea.Type] there may be
     * other triggers that end the session, such as if a device state change makes the window area
     * =unavailable. One example of this is if the device is currently moved to the
     * [TYPE_REAR_FACING] window area of a foldable device, the device will be moved back to the
     * default window area, and the status of the operation will no longer be
     * [WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE].
     *
     * @param windowAreaToken [WindowAreaToken] window area token that identifies the [WindowArea]
     *   to move to.
     * @param activity Base Activity making the call to [transferToWindowArea].
     * @throws IllegalStateException if this method is called when the provided [WindowArea] does
     *   not have a [WINDOW_AREA_STATUS_AVAILABLE] status for [OPERATION_TRANSFER_TO_AREA].
     * @throws SecurityException if this method is called from a process that is not the top-level
     *   process.
     * @throws IllegalArgumentException if this method is called with a [WindowArea] with a
     *   [WindowArea.Type] that is unrecognized.
     * @see addWindowAreasListener
     */
    public abstract fun transferToWindowArea(windowAreaToken: WindowAreaToken?, activity: Activity)

    /**
     * Starts a presentation session on the [WindowArea] identified by the [windowAreaToken] and
     * sends updates through the [WindowAreaPresentationSessionCallback].
     *
     * If a presentation session is attempted to be started without it being available,
     * [WindowAreaPresentationSessionCallback.onSessionEnded] will be called immediately with an
     * [IllegalStateException].
     *
     * Only the top visible application can request to start a presentation session.
     *
     * The presentation session will stay active until the presentation provided through
     * [WindowAreaPresentationSessionCallback.onSessionStarted] is closed. The [WindowArea.Type] may
     * provide different triggers to close the session such as if the calling application is no
     * longer in the foreground, or there is a device state change that makes the window area
     * unavailable to be presented on. One example scenario is if a [TYPE_REAR_FACING] window area
     * is being presented to on a foldable device that is open and has 2 screens. If the device is
     * closed and the internal display is turned off, the session would be ended and
     * [WindowAreaPresentationSessionCallback.onSessionEnded] is called to notify that the session
     * has been ended. The session may end prematurely if the device gets to a critical thermal
     * level, or if power saver mode is enabled.
     *
     * @param windowAreaToken identifier for which [WindowArea] is to be presented on
     * @param activity An [Activity] that will present content on the Rear Display.
     * @param executor Executor used to provide updates to [windowAreaPresentationSessionCallback].
     * @param windowAreaPresentationSessionCallback to be notified of updates to the lifecycle of
     *   the currently enabled rear display presentation.
     * @see addWindowAreasListener
     */
    @ExperimentalWindowApi
    public abstract fun presentContentOnWindowArea(
        windowAreaToken: WindowAreaToken,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
    )

    /**
     * Returns the current active [WindowAreaSessionPresenter] if one is currently active in the
     * [WindowArea] identified by the provided [WindowAreaToken]. Returns null if there is no active
     * presentation session for the provided [windowAreaToken].
     */
    @ExperimentalWindowApi
    public abstract fun getActivePresentationSession(
        windowAreaToken: WindowAreaToken
    ): WindowAreaSessionPresenter?

    public companion object {

        private val TAG = WindowAreaController::class.simpleName

        private var decorator: WindowAreaControllerDecorator = EmptyDecorator
        private val windowAreaController: WindowAreaController by lazy {
            val windowAreaComponentExtensions =
                try {
                    this::class.java.classLoader?.let {
                        SafeWindowAreaComponentProvider(it).windowAreaComponent
                    }
                } catch (t: Throwable) {
                    if (BuildConfig.verificationMode == VerificationMode.LOG) {
                        Log.d(TAG, "Failed to load WindowExtensions: ${t.message}")
                    }
                    null
                }

            val deviceSupported =
                Build.VERSION.SDK_INT > Build.VERSION_CODES.Q &&
                    windowAreaComponentExtensions != null &&
                    ExtensionsUtil.safeVendorApiLevel >= 3

            if (deviceSupported) {
                WindowAreaControllerImpl(windowAreaComponent = windowAreaComponentExtensions)
            } else {
                EmptyWindowAreaControllerImpl()
            }
        }

        /** Provides an instance of [WindowAreaController]. */
        @JvmName("getOrCreate")
        @JvmStatic
        public fun getOrCreate(): WindowAreaController {
            return decorator.decorate(windowAreaController)
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

/** Decorator that allows us to provide different functionality in our window-testing artifact. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface WindowAreaControllerDecorator {
    /** Returns an instance of [WindowAreaController] associated to the [Activity] */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun decorate(controller: WindowAreaController): WindowAreaController
}

private object EmptyDecorator : WindowAreaControllerDecorator {
    override fun decorate(controller: WindowAreaController): WindowAreaController {
        return controller
    }
}
