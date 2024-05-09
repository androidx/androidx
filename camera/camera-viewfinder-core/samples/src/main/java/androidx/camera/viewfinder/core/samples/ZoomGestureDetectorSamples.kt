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

package androidx.camera.viewfinder.core.samples

import android.content.Context
import android.view.MotionEvent
import androidx.annotation.Sampled
import androidx.camera.core.Camera
import androidx.camera.viewfinder.core.ZoomGestureDetector
import androidx.camera.viewfinder.core.ZoomGestureDetector.ZoomEvent
import kotlin.math.max
import kotlin.math.min

// The application context.
private lateinit var context: Context
// The camera instance returned by ProcessCameraProvider.bindToLifecycle.
private lateinit var camera: Camera

@Sampled
fun onTouchEventSample(event: MotionEvent): Boolean {
    val zoomGestureDetector =
        ZoomGestureDetector(context) { zoomEvent ->
            when (zoomEvent) {
                is ZoomEvent.Move -> {
                    val zoomState = camera.cameraInfo.zoomState.value!!
                    val ratio = zoomState.zoomRatio * zoomEvent.incrementalScaleFactor
                    val minRatio = zoomState.minZoomRatio
                    val maxRatio = zoomState.maxZoomRatio
                    val clampedRatio = min(max(ratio, minRatio), maxRatio)
                    camera.cameraControl.setZoomRatio(clampedRatio)
                }
                is ZoomEvent.Begin -> {
                    // Handle the begin event. For example, determine whether this gesture
                    // should be processed further.
                }
                is ZoomEvent.End -> {
                    // Handle the end event. For example, show a UI indicator.
                }
            }
            true
        }

    return zoomGestureDetector.onTouchEvent(event)
}
