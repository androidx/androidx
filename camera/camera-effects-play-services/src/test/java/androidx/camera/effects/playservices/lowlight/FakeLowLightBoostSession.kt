/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.camera.effects.playservices.lowlight

import android.graphics.SurfaceTexture
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.SceneDetectorCallback
import java.util.concurrent.Executor

/** A fake [LowLightBoostSession] with a valid camera surface. */
@RequiresApi(Build.VERSION_CODES.R)
internal class FakeLowLightBoostSession : LowLightBoostSession {
    val cameraSurfaceTexture = SurfaceTexture(CAMERA_TEXTURE_NAME)
    private val cameraSurface = Surface(cameraSurfaceTexture)
    val cameraSurfaceForTest: Surface
        get() = cameraSurface

    var releaseCount = 0
    var throwOnCall = false
    var sceneDetectorCallback: SceneDetectorCallback? = null
    private var isEnabled = false

    override fun getCameraSurface(): Surface {
        throwIfNeeded()
        return cameraSurface
    }

    override fun processCaptureResult(result: TotalCaptureResult) {
        throwIfNeeded()
    }

    override fun enableLowLightBoost(enabled: Boolean) {
        throwIfNeeded()
        isEnabled = enabled
    }

    override fun isLowLightBoostEnabled(): Boolean = isEnabled

    override fun setSceneDetectorCallback(callback: SceneDetectorCallback?, executor: Executor?) {
        sceneDetectorCallback = callback
    }

    override fun release() {
        releaseCount++
        cameraSurface.release()
    }

    private fun throwIfNeeded() {
        if (throwOnCall) {
            throw IllegalStateException("Session is dead")
        }
    }

    private companion object {
        private const val CAMERA_TEXTURE_NAME = 2
    }
}
