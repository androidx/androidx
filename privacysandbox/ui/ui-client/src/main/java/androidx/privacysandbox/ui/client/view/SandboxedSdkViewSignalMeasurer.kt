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

package androidx.privacysandbox.ui.client.view

import androidx.privacysandbox.ui.client.GeometryMeasurer
import androidx.privacysandbox.ui.core.SandboxedSdkViewUiInfo
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapterSignalOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Class for calculating signals related to the presentation of a [SandboxedSdkView]. */
internal class SandboxedSdkViewSignalMeasurer(
    val view: SandboxedSdkView,
    private val session: SandboxedUiAdapter.Session,
) {
    private var geometryMeasurer = GeometryMeasurer.getInstance()

    // TODO(b/340466873): Delegate obstruction measurement to SandboxedSdkUi when SandboxedSdkView
    // is part of SSU
    private var isMeasuringObstructions =
        session.signalOptions.contains(SandboxedUiAdapterSignalOptions.OBSTRUCTIONS)
    private var scope = CoroutineScope(Dispatchers.Default)
    private var childView = view.getChildAt(0)
    private var isMeasuring = true

    init {
        scope.launch {
            var geometryFlow = geometryMeasurer.registerView(childView, isMeasuringObstructions)
            geometryFlow.collect { geometryData ->
                val sandboxedSdkViewUiInfo =
                    SandboxedSdkViewUiInfo(
                        geometryData.widthPx,
                        geometryData.heightPx,
                        geometryData.onScreenGeometry,
                        view.alpha,
                        geometryData.obstructions,
                    )
                session.notifyUiChanged(SandboxedSdkViewUiInfo.toBundle(sandboxedSdkViewUiInfo))
            }
        }
    }

    /**
     * Requests for the associated [GeometryMeasurer] to emit a new set of geometry signals. The
     * [GeometryMeasurer] is responsible for throttling these requests so that data is not emitted
     * too frequently.
     *
     * When [onLayoutEventOccurred] is true, this indicates that onLayout has been called on [view].
     * This is used as a signal to recompute the view's parents when performing the obstructions
     * calculation, if required.
     */
    fun requestUpdatedSignals(onLayoutEventOccurred: Boolean = false) {
        geometryMeasurer.requestUpdatedSignals(onLayoutEventOccurred)
    }

    /**
     * Called when [view] is attached to the window again. This may happen when using pooling
     * containers, where the UI session is retained but geometry measurement stops when the view
     * gets temporarily detached from the window.
     */
    fun resumeMeasuringIfNecessary() {
        if (isMeasuring) {
            return
        }
        scope.launch {
            geometryMeasurer.registerView(childView, isMeasuringObstructions)
            isMeasuring = true
        }
    }

    /**
     * Stops consuming emitted geometry data for the associated view, and tells the
     * [GeometryMeasurer] to stop measuring geometry data for this view.
     *
     * This may happen when the UI session hosting the [view] stops, or when the [view] is detached
     * from the window but the UI session remains active. The latter case may happen when using
     * pooling containers.
     */
    fun stopMeasuring() {
        if (!isMeasuring) {
            return
        }
        scope.launch {
            geometryMeasurer.unregisterView(childView)
            scope.cancel()
            isMeasuring = false
        }
    }
}
