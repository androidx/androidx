/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.graphics.Rect
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.impl.DEFAULT_ZOOM_RATIO
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

open class FakeZoomCompat(
    override val minZoomRatio: Float = 0f,
    override val maxZoomRatio: Float = 0f,
    var croppedSensorArea: Rect = Rect(0, 0, 640, 480),
) : ZoomCompat {
    var zoomRatio = DEFAULT_ZOOM_RATIO
    var applyAsyncResult = CompletableDeferred(Unit) // already completed deferred

    override fun applyAsync(
        zoomRatio: Float,
        requestControl: UseCaseCameraRequestControl,
    ): Deferred<Unit> {
        return applyAsyncResult.also { result ->
            result.invokeOnCompletion { this.zoomRatio = zoomRatio }
        }
    }

    override fun resetAsync(requestControl: UseCaseCameraRequestControl): Deferred<Unit> {
        zoomRatio = DEFAULT_ZOOM_RATIO
        return CompletableDeferred(Unit)
    }

    override fun getCropSensorRegion() = croppedSensorArea
}
