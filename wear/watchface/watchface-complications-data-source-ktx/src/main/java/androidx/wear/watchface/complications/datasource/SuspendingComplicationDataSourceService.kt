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

package androidx.wear.watchface.complications.datasource

import androidx.annotation.UiThread
import androidx.wear.watchface.complications.data.ComplicationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Kotlin coroutine wrapper for [ComplicationDataSourceService].
 */
public abstract class SuspendingComplicationDataSourceService : ComplicationDataSourceService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    final override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        scope.launch {
            listener.onComplicationData(onComplicationRequest(request))
        }
    }

    /**
     * Computes the [ComplicationData] for the given [request].
     *
     * The [ComplicationData] returned from this method will be passed to the
     * [ComplicationDataSourceService.ComplicationRequestListener] provided to
     * [onComplicationRequest].
     * Return `null` to indicate that the previous complication data shouldn't be overwritten.
     *
     * @see ComplicationDataSourceService.onComplicationRequest
     * @see ComplicationDataSourceService.ComplicationRequestListener.onComplicationData
     */
    @UiThread
    abstract suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData?

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
