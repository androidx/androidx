/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.os.Build
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.ink.authoring.InProgressStrokeId

/**
 * Aids [CanvasInProgressStrokesRenderHelperV29] in handing off rendering from front buffer (low
 * latency) rendering to HWUI ([android.view.View] hierarchy) rendering. Interoperates with
 * [androidx.graphics.lowlatency.GLFrontBufferedRenderer] and
 * [androidx.graphics.lowlatency.CanvasFrontBufferedRenderer].
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal interface FrontBufferToHwuiHandoff {

    @UiThread fun setup()

    @UiThread fun cleanup()

    /** Call from [InProgressStrokesRenderHelper.requestStrokeCohortHandoffToHwui]. */
    @UiThread fun requestCohortHandoff(handingOff: Map<InProgressStrokeId, FinishedStroke>)

    companion object {

        /**
         * Create and return a [FrontBufferToHwuiHandoff] instance that is appropriate to the API
         * level.
         *
         * @param mainView The [ViewGroup] that the [surfaceView] belongs to.
         * @param surfaceView The [surfaceView] that holds the front buffer render layer.
         * @param onCohortHandoff Calls
         *   [InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwui].
         * @param onCohortHandoffComplete Calls
         *   [InProgressStrokesRenderHelper.Callback.onStrokeCohortHandoffToHwuiComplete].
         */
        fun create(
            mainView: ViewGroup,
            surfaceView: SurfaceView,
            @UiThread onCohortHandoff: (Map<InProgressStrokeId, FinishedStroke>) -> Unit,
            @UiThread onCohortHandoffComplete: () -> Unit,
        ): FrontBufferToHwuiHandoff {
            // TODO: b/328087803 - Samsung API 34 devices do not seem to execute the buffer release
            //   callback that V34 relies on. So use V29 for those devices instead.
            return if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    Build.MANUFACTURER != "samsung"
            ) {
                FrontBufferToHwuiHandoffV34(
                    mainView,
                    surfaceView,
                    onCohortHandoff,
                    onCohortHandoffComplete
                )
            } else {
                FrontBufferToHwuiHandoffV29(
                    mainView,
                    surfaceView,
                    onCohortHandoff,
                    onCohortHandoffComplete
                )
            }
        }
    }
}
