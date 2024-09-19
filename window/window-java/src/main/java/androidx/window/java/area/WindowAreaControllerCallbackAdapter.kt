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
import android.os.Binder
import androidx.core.util.Consumer
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.core.ExperimentalWindowApi
import androidx.window.java.core.CallbackToFlowAdapter
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow

/** An adapter for [WindowAreaController] to provide callback APIs. */
@ExperimentalWindowApi
class WindowAreaControllerCallbackAdapter
private constructor(
    private val controller: WindowAreaController,
    private val callbackToFlowAdapter: CallbackToFlowAdapter
) : WindowAreaController() {

    constructor(controller: WindowAreaController) : this(controller, CallbackToFlowAdapter())

    /**
     * Registers a listener that is interested in the current list of [WindowAreaInfo] available to
     * be interacted with.
     *
     * The [listener] will receive an initial value on registration, as soon as it becomes
     * available.
     *
     * @param executor to handle sending listener updates.
     * @param listener to receive updates to the list of [WindowAreaInfo].
     * @see WindowAreaController.transferActivityToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    fun addWindowAreaInfoListListener(
        executor: Executor,
        listener: Consumer<List<WindowAreaInfo>>
    ) {
        callbackToFlowAdapter.connect(executor, listener, controller.windowAreaInfos)
    }

    /**
     * Removes a listener of available [WindowAreaInfo] records. If the listener is not present then
     * this method is a no-op.
     *
     * @param listener to remove from receiving status updates.
     * @see WindowAreaController.transferActivityToWindowArea
     * @see WindowAreaController.presentContentOnWindowArea
     */
    fun removeWindowAreaInfoListListener(listener: Consumer<List<WindowAreaInfo>>) {
        callbackToFlowAdapter.disconnect(listener)
    }

    override val windowAreaInfos: Flow<List<WindowAreaInfo>>
        get() = controller.windowAreaInfos

    override fun transferActivityToWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaSessionCallback: WindowAreaSessionCallback
    ) =
        controller.transferActivityToWindowArea(
            token,
            activity,
            executor,
            windowAreaSessionCallback
        )

    override fun presentContentOnWindowArea(
        token: Binder,
        activity: Activity,
        executor: Executor,
        windowAreaPresentationSessionCallback: WindowAreaPresentationSessionCallback
    ) =
        controller.presentContentOnWindowArea(
            token,
            activity,
            executor,
            windowAreaPresentationSessionCallback
        )
}
