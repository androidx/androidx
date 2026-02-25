/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.samples

import android.hardware.HardwareBuffer
import androidx.annotation.Sampled
import androidx.xr.arcore.playservices.ArCorePerceptionManager
import androidx.xr.arcore.playservices.ArCoreRuntime
import androidx.xr.arcore.playservices.CameraState
import androidx.xr.arcore.playservices.UnsupportedArCoreCompatApi
import androidx.xr.arcore.playservices.cameraState
import androidx.xr.runtime.Session

/**
 * @param session the [Session] to get the ARCore session from
 * @return the underlying [com.google.ar.core.Session] obtained by the [ArCoreRuntime]
 */
@Sampled
@OptIn(UnsupportedArCoreCompatApi::class)
fun getARCoreSession(session: Session): com.google.ar.core.Session? {
    // This code assumes the ARCore for Play Services runtime is being used.
    val runtime = session.runtimes.first() as? ArCoreRuntime
    return runtime?.lifecycleManager?.session()
}

/**
 * @param session the [Session] to get the ARCore frame from
 * @return the current [com.google.ar.core.Frame] obtained by the [ArCorePerceptionManager]
 */
@Sampled
@OptIn(UnsupportedArCoreCompatApi::class)
fun getARCoreFrame(session: Session): com.google.ar.core.Frame? {
    // This code assumes the ARCore for Play Services runtime is being used.
    val runtime = session.runtimes.first() as? ArCoreRuntime
    return runtime?.perceptionManager?.lastFrame()
}

/**
 * @param session the [Session] to get the hardware buffer from
 * @return the [HardwareBuffer] obtained by the [CameraState]
 */
@Sampled
fun getARCoreHardwareBuffer(session: Session): HardwareBuffer? {
    val coreState = session.state.value
    val cameraState = coreState.cameraState
    // The CameraState object is not supported by all runtimes;
    // if it's not supported, `cameraState` will return null.
    return cameraState?.hardwareBuffer
}
