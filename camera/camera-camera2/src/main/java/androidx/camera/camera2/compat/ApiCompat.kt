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

package androidx.camera.camera2.compat

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.common.UnsafeWrapper
import androidx.camera.common.unwrapAs
import androidx.camera.core.impl.CameraCaptureResult

@RequiresApi(Build.VERSION_CODES.N)
internal object Api24Compat {
    @JvmStatic
    fun onCaptureBufferLost(
        callback: CameraCaptureSession.CaptureCallback,
        session: CameraCaptureSession,
        request: CaptureRequest,
        surface: Surface,
        frameNumber: Long,
    ) {
        callback.onCaptureBufferLost(session, request, surface, frameNumber)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
internal object Api29Compat {
    @JvmStatic
    fun getActivePhysicalCameraId(result: CameraCaptureResult): String? {
        val wrapper = result as? UnsafeWrapper
        val physicalId =
            wrapper
                ?.unwrapAs<FrameMetadata>()
                ?.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
                ?: wrapper
                    ?.unwrapAs<CaptureResult>()
                    ?.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
                ?: result.captureResult?.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
        return physicalId?.ifEmpty { null }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
internal object Api30Compat {
    @JvmStatic
    fun getDisplaySize(context: Context, display: Display): Size {
        val windowContext =
            context
                .createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        val windowManager = windowContext.getSystemService(WindowManager::class.java)
        val bounds =
            checkNotNull(windowManager) { "WindowManager is not available." }
                .maximumWindowMetrics
                .bounds
        return Size(bounds.width(), bounds.height())
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal object Api34Compat {
    @JvmStatic
    fun onReadoutStarted(
        callback: CameraCaptureSession.CaptureCallback,
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long,
    ) {
        callback.onReadoutStarted(session, request, timestamp, frameNumber)
    }

    @JvmStatic
    fun setSettingsOverrideZoom(parameters: MutableMap<CaptureRequest.Key<*>, Any>) {
        parameters[CaptureRequest.CONTROL_SETTINGS_OVERRIDE] =
            CaptureRequest.CONTROL_SETTINGS_OVERRIDE_ZOOM
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal object Api35Compat {
    @JvmStatic
    fun setFlashStrengthLevel(parameters: MutableMap<CaptureRequest.Key<*>, Any>, level: Int) {
        parameters[CaptureRequest.FLASH_STRENGTH_LEVEL] = level
    }
}
