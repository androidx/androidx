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

package androidx.window.java.embedding

import android.app.Activity
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitInfo
import androidx.window.java.core.CallbackToFlowAdapter
import java.util.concurrent.Executor

/**
 * An adapted interface for [SplitController] that provides callback shaped APIs to report
 * the latest split information with [SplitInfo] list.
 *
 * It should only be used if [SplitController.splitInfoList] is not available. For example, an app
 * is written in Java and cannot use Flow APIs.
 *
 * @param controller A [SplitController] that can be obtained by [SplitController.getInstance]
 */
@ExperimentalWindowApi
class SplitControllerCallbackAdapter private constructor(
    private val controller: SplitController,
    private val callbackToFlowAdapter: CallbackToFlowAdapter
) {

    constructor(controller: SplitController) : this(controller, CallbackToFlowAdapter())

    /**
     * Registers a listener for updates about the active split state(s) that this
     * activity is part of. An activity can be in zero, one or more active splits.
     * More than one active split is possible if an activity created multiple
     * containers to side, stacked on top of each other. Or it can be in two
     * different splits at the same time - in a secondary container for one (it was
     * launched to the side) and in the primary for another (it launched another
     * activity to the side). The reported splits in the list are ordered from
     * bottom to top by their z-order, more recent splits appearing later.
     * Guaranteed to be called at least once to report the most recent state.
     *
     * @param activity only split that this [Activity] is part of will be reported.
     * @param executor when there is an update to the active split state(s), the [consumer] will be
     * invoked on this [Executor].
     * @param consumer [Consumer] that will be invoked on the [executor] when there is an update to
     * the active split state(s).
     */
    fun addSplitListener(
        activity: Activity,
        executor: Executor,
        consumer: Consumer<List<SplitInfo>>
    ) {
        callbackToFlowAdapter.connect(executor, consumer, controller.splitInfoList(activity))
    }

    /**
     * Unregisters a listener that was previously registered via [addSplitListener].
     *
     * @param consumer the previously registered [Consumer] to unregister.
     */
    fun removeSplitListener(consumer: Consumer<List<SplitInfo>>) {
        callbackToFlowAdapter.disconnect(consumer)
    }
}
