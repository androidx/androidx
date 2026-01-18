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
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.area.WindowAreaToken
import androidx.window.core.ExperimentalWindowApi
import androidx.window.java.core.CallbackToFlowAdapter
import java.util.concurrent.Executor

/** An adapter for [WindowAreaController] to provide callback APIs. */
public class WindowAreaControllerCallbackAdapter
private constructor(
    private val controller: WindowAreaController,
    private val callbackToFlowAdapter: CallbackToFlowAdapter,
) {

    public constructor(controller: WindowAreaController) : this(controller, CallbackToFlowAdapter())

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

    public fun transferToWindowArea(windowAreaToken: WindowAreaToken?, activity: Activity): Unit =
        controller.transferToWindowArea(windowAreaToken, activity)

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

    @ExperimentalWindowApi
    public fun getActivePresentationSession(
        windowAreaToken: WindowAreaToken
    ): WindowAreaSessionPresenter = controller.getActivePresentationSession(windowAreaToken)
}
