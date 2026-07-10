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

package androidx.camera.camera2.compat.workaround

import android.hardware.camera2.CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.quirk.UltraWideFlashCaptureUnderexposureQuirk
import androidx.camera.camera2.compat.quirk.UseTorchAsFlashQuirk
import androidx.camera.camera2.internal.IntrinsicZoomCalculator
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.warn
import dagger.Module
import dagger.Provides

/**
 * Workaround to use torch as flash.
 *
 * @see UseTorchAsFlashQuirk
 */
public interface UseTorchAsFlash {
    /**
     * Determines whether the torch should be used for still image capture instead of the regular
     * flash firing sequence.
     *
     * This is a workaround for devices that have issues with the standard flash sequence, such as
     * the [UseTorchAsFlashQuirk] sub-classes.
     *
     * @param frameMetadata A suspend function that provides a corresponding [FrameMetadata] when
     *   required. This can be used to dynamically decide whether to apply the workaround based on
     *   the current camera state, such as the active physical camera.
     * @return `true` if the torch should be used as flash, `false` otherwise.
     */
    public suspend fun shouldUseTorchAsFlash(frameMetadata: suspend () -> FrameMetadata?): Boolean

    /**
     * Returns true if AE precapture should be disabled.
     *
     * AE precap is not disabled for quirks (mainly new ones) where we have tested that AE precap
     * works. For older quirks which weren't tested for this, AE precap is disabled by default for
     * safety.
     */
    public fun shouldDisableAePrecapture(): Boolean

    @Module
    public abstract class Bindings {
        public companion object {
            @Provides
            public fun provideUseTorchAsFlash(
                cameraQuirks: CameraQuirks,
                cameraDevices: CameraDevices,
                intrinsicZoomCalculator: IntrinsicZoomCalculator,
            ): UseTorchAsFlash =
                if (cameraQuirks.quirks.contains(UseTorchAsFlashQuirk::class.java))
                    UseTorchAsFlashImpl(cameraQuirks, cameraDevices, intrinsicZoomCalculator)
                else NotUseTorchAsFlash
        }
    }
}

public class UseTorchAsFlashImpl(
    private val cameraQuirks: CameraQuirks,
    private val cameraDevices: CameraDevices,
    private val intrinsicZoomCalculator: IntrinsicZoomCalculator,
) : UseTorchAsFlash {
    private val hasUwCameraUnderexposedFlashCaptureQuirk by lazy {
        cameraQuirks.quirks.contains(UltraWideFlashCaptureUnderexposureQuirk::class.java)
    }

    override suspend fun shouldUseTorchAsFlash(
        frameMetadata: suspend () -> FrameMetadata?
    ): Boolean {
        debug {
            "shouldUseTorchAsFlash: hasUwCameraUnderexposedFlashCaptureQuirk =" +
                " $hasUwCameraUnderexposedFlashCaptureQuirk"
        }

        // If the quirk is not for the ultra-wide camera, it's for another torch-as-flash quirk, so
        // we apply the workaround unconditionally.
        if (!hasUwCameraUnderexposedFlashCaptureQuirk) {
            return true
        }

        // For the ultra-wide quirk, we only apply the workaround if the ultra-wide
        // lens is active.

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            warn {
                "shouldUseTorchAsFlash: API level is too low to know if it's ultra wide camera," +
                    " defaulting to workaround for safety."
            }
            return true
        }

        val frameMetadata = frameMetadata.invoke()

        if (frameMetadata == null) {
            warn {
                "shouldUseTorchAsFlash: frameMetadata is null, defaulting to workaround for safety."
            }
            return true
        }

        // isUltraWideCamera() returns null on failure, true is returned then to be on the safe side
        return frameMetadata.isUltraWideCamera() ?: true
    }

    @RequiresApi(29)
    private fun FrameMetadata.isUltraWideCamera(): Boolean? {
        val cameraId =
            get(LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
                ?: run {
                    warn {
                        "isUltraWideCamera: could not get active physical camera ID to identify" +
                            " if it's ultra wide camera."
                    }
                    return null
                }

        val cameraMetadata =
            cameraDevices.awaitCameraMetadata(CameraId(cameraId))
                ?: run {
                    warn { "isUltraWideCamera: failed to get CameraMetadata for $cameraId" }
                    return null
                }

        val intrinsicZoomRatio =
            intrinsicZoomCalculator.calculateIntrinsicZoomRatio(cameraMetadata)
                ?: run {
                    warn { "isUltraWideCamera: could not calculate intrinsic zoom ratio." }
                    return null
                }

        debug {
            "isUltraWideCamera: cameraId = $cameraId, intrinsicZoomRatio = $intrinsicZoomRatio"
        }

        return intrinsicZoomRatio < 1.0f
    }

    override fun shouldDisableAePrecapture(): Boolean {
        return !hasUwCameraUnderexposedFlashCaptureQuirk
    }
}

public object NotUseTorchAsFlash : UseTorchAsFlash {
    override suspend fun shouldUseTorchAsFlash(
        frameMetadata: suspend () -> FrameMetadata?
    ): Boolean = false

    override fun shouldDisableAePrecapture(): Boolean = false
}
