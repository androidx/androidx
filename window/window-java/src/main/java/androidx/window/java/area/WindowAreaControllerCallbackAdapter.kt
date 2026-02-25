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

package androidx.window.java.area

import android.app.Activity
import androidx.core.util.Consumer
import androidx.window.area.WindowArea
import androidx.window.area.WindowArea.Type.Companion.TYPE_REAR_FACING
import androidx.window.area.WindowAreaCapability.Operation.Companion.OPERATION_TRANSFER_TO_AREA
import androidx.window.area.WindowAreaCapability.Status.Companion.WINDOW_AREA_STATUS_AVAILABLE
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.area.WindowAreaToken
import androidx.window.core.ExperimentalWindowApi
import java.util.concurrent.Executor

/** An adapter for [WindowAreaController] to provide callback APIs. */
public class WindowAreaControllerCallbackAdapter
public constructor(private val controller: WindowAreaController) {

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
    public fun addWindowAreasListener(executor: Executor, listener: Consumer<List<WindowArea>>) {
        controller.addWindowAreasListener(executor, listener)
    }

    /**
     * Removes a listener of available [WindowArea] records. If the listener is not present then
     * this method is a no-op.
     *
     * @param listener to remove from receiving status updates.
     * @see WindowAreaController.transferToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    public fun removeWindowAreasListener(listener: Consumer<List<WindowArea>>) {
        controller.removeWindowAreasListener(listener)
    }

    /**
     * Moves the calling [Activity] and the global state of the device to the [WindowArea] provided.
     * This is a long-lasting and sticky operation that will outlive the application that requests
     * this operation. Status updates can be received by registering a listener through
     * [addWindowAreasListener] and then querying for the [OPERATION_TRANSFER_TO_AREA] operation.
     *
     * Attempting to move the device to a window area when the [WindowArea] does not return
     * [WINDOW_AREA_STATUS_AVAILABLE] will result in an [IllegalStateException].
     *
     * Only the top visible application can request the device to move to a [WindowArea]. If this
     * operation is requested when the application is not the top level process, a
     * [SecurityException] will be thrown.
     *
     * Passing a null [WindowAreaToken] returns back to the default window area (usually going to be
     * the [android.view.Display.DEFAULT_DISPLAY]). Depending on the [WindowArea.Type] there may be
     * other triggers that end the session, such as if a device state change makes the window area
     * =unavailable. One example of this is if the device is currently moved to the
     * [TYPE_REAR_FACING] window area of a foldable device, the device will be moved back to the
     * default window area, and the status of the operation will no longer be
     * [WINDOW_AREA_STATUS_ACTIVE].
     *
     * @param windowAreaToken [WindowAreaToken] window area token that identifies the [WindowArea]
     *   to move to.
     * @param activity Base Activity to transfer to the [transferToWindowArea].
     * @throws IllegalStateException if this method is called when the provided [WindowArea] does
     *   not have a [WINDOW_AREA_STATUS_AVAILABLE] status for [OPERATION_TRANSFER_TO_AREA].
     * @throws SecurityException if this method is called from a process that is not the top-level
     *   process.
     * @throws IllegalArgumentException if this method is called for an unrecognized
     *   [WindowArea.Type]
     * @see addWindowAreasListener
     * @see WindowAreaController
     */
    public fun transferToWindowArea(windowAreaToken: WindowAreaToken?, activity: Activity): Unit =
        controller.transferToWindowArea(windowAreaToken, activity)

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
     * unavailable. One example scenario is if a [TYPE_REAR_FACING] window area is being presented
     * to on a foldable device that is open and has 2 screens. If the device is closed and the
     * internal display is turned off, the session would be ended and
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
     * @see WindowAreaController
     */
    @ExperimentalWindowApi
    public fun presentContentOnWindowArea(
        windowAreaToken: WindowAreaToken,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback,
    ): Unit =
        controller.presentContentOnWindowArea(
            windowAreaToken,
            activity,
            executor,
            windowAreaPresentationSessionCallback,
        )

    /**
     * Returns the current active [WindowAreaSessionPresenter] if one is currently active in the
     * [WindowArea] identified by the provided [WindowAreaToken]. Returns null if there is no active
     * presentation session for the provided [windowAreaToken].
     */
    @ExperimentalWindowApi
    public fun getActivePresentationSession(
        windowAreaToken: WindowAreaToken
    ): WindowAreaSessionPresenter? = controller.getActivePresentationSession(windowAreaToken)
}
