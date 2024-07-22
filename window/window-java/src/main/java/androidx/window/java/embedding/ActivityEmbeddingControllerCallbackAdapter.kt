/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.EmbeddedActivityWindowInfo
import androidx.window.java.core.CallbackToFlowAdapter
import java.util.concurrent.Executor

/**
 * An adapted interface for [ActivityEmbeddingController] that provides callback shaped APIs to
 * report the latest [EmbeddedActivityWindowInfo].
 *
 * It should only be used if [ActivityEmbeddingController.embeddedActivityWindowInfo] is not
 * available. For example, an app is written in Java and cannot use Flow APIs.
 *
 * @param controller an [ActivityEmbeddingController] that can be obtained by
 *   [ActivityEmbeddingController.getInstance].
 * @constructor creates a callback adapter of
 *   [ActivityEmbeddingController.embeddedActivityWindowInfo] flow API.
 */
class ActivityEmbeddingControllerCallbackAdapter(
    private val controller: ActivityEmbeddingController
) {
    private val callbackToFlowAdapter = CallbackToFlowAdapter()

    /**
     * Registers a listener for updates of [EmbeddedActivityWindowInfo] of [activity].
     *
     * The [listener] will immediately be invoked with the latest value upon registration if the
     * [activity] is currently embedded as [EmbeddedActivityWindowInfo.isEmbedded] is `true`.
     *
     * When the [activity] is embedded, the [listener] will be invoked when
     * [EmbeddedActivityWindowInfo] is changed. When the [activity] is not embedded, the [listener]
     * will not be triggered unless the [activity] is becoming non-embedded from embedded.
     *
     * Note that this API is only supported on the device with
     * [WindowSdkExtensions.extensionVersion] equal to or larger than 6. If
     * [WindowSdkExtensions.extensionVersion] is less than 6, this [listener] will not be invoked.
     *
     * @param activity the [Activity] that is interested in getting the embedded window info.
     * @param executor the [Executor] to dispatch the [EmbeddedActivityWindowInfo] change.
     * @param listener the [Consumer] that will be invoked on the [executor] when there is an update
     *   to [EmbeddedActivityWindowInfo].
     */
    @RequiresWindowSdkExtension(6)
    fun addEmbeddedActivityWindowInfoListener(
        activity: Activity,
        executor: Executor,
        listener: Consumer<EmbeddedActivityWindowInfo>
    ) {
        callbackToFlowAdapter.connect(
            executor,
            listener,
            controller.embeddedActivityWindowInfo(activity)
        )
    }

    /**
     * Unregisters a listener that was previously registered via
     * [addEmbeddedActivityWindowInfoListener].
     *
     * It's no-op if the [listener] has not been registered.
     *
     * @param listener the previously registered [Consumer] to unregister.
     */
    @RequiresWindowSdkExtension(6)
    fun removeEmbeddedActivityWindowInfoListener(listener: Consumer<EmbeddedActivityWindowInfo>) {
        callbackToFlowAdapter.disconnect(listener)
    }
}
