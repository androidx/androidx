/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.wrapper

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.StreamType
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.impl.checkNOrHigher
import androidx.camera.camera2.pipe.impl.checkOOrHigher
import androidx.camera.camera2.pipe.impl.checkPOrHigher
import androidx.camera.camera2.pipe.wrapper.OutputConfigurationWrapper.Companion.SURFACE_GROUP_ID_NONE
import java.util.concurrent.Executor

/**
 * A data class that mirrors the fields in [android.hardware.camera2.params.SessionConfiguration] so
 * that a real instance can be constructed when creating a
 * [android.hardware.camera2.CameraCaptureSession] on newer versions of the OS.
 */
data class SessionConfigData(
    val sessionType: Int,
    val inputConfiguration: InputConfigData?,
    val outputConfigurations: List<OutputConfigurationWrapper>,
    val executor: Executor,
    val stateCallback: CameraCaptureSessionWrapper.StateCallback,

    val sessionTemplateId: Int,
    val sessionParameters: Map<*, Any>
) {
    companion object {
        /* NOTE: These must keep in sync with their SessionConfiguration values. */
        const val SESSION_TYPE_REGULAR = 0
        const val SESSION_TYPE_HIGH_SPEED = 1
    }
}

/**
 * A data class that mirrors the fields in [android.hardware.camera2.params.InputConfiguration] so
 * that a real instance can be constructed when creating a
 * [android.hardware.camera2.CameraCaptureSession] on newer versions of the OS.
 */
data class InputConfigData(
    val width: Int,
    val height: Int,
    val format: Int
)

/**
 * An interface for [OutputConfiguration] with minor modifications.
 *
 * The primary modifications to this class are to make it harder to accidentally changes things that
 * cannot be modified after the [android.hardware.camera2.CameraCaptureSession] has been created.
 *
 * [OutputConfiguration]'s are NOT immutable, and changing state of an [OutputConfiguration] may
 * require the CameraCaptureSession to be finalized or updated.
 */
interface OutputConfigurationWrapper : UnsafeWrapper<OutputConfiguration> {
    /**
     * This method will return null if the output configuration was created without a Surface,
     * and until addSurface is called for the first time.
     *
     * @see OutputConfiguration.getSurface
     */
    val surface: Surface?

    /**
     * This method returns the current list of surfaces for this [OutputConfiguration]. Since the
     * [OutputConfiguration] is stateful, this value may change as a result of calling addSurface
     * or removeSurface.
     *
     * @see OutputConfiguration.getSurfaces
     */
    val surfaces: List<Surface>

    /** @see OutputConfiguration.addSurface */
    fun addSurface(surface: Surface)

    /** @see OutputConfiguration.removeSurface */
    fun removeSurface(surface: Surface)

    /** @see OutputConfiguration.setPhysicalCameraId */
    val physicalCameraId: CameraId?

    /** @see OutputConfiguration.enableSurfaceSharing */
    val surfaceSharing: Boolean

    /** @see OutputConfiguration.getMaxSharedSurfaceCount */
    val maxSharedSurfaceCount: Int

    /** @see OutputConfiguration.getSurfaceGroupId */
    val surfaceGroupId: Int

    companion object {
        const val SURFACE_GROUP_ID_NONE = -1
    }
}

@RequiresApi(24)
@SuppressLint("UnsafeNewApiCall")
class AndroidOutputConfiguration(
    private val output: OutputConfiguration,
    override val surfaceSharing: Boolean,
    override val maxSharedSurfaceCount: Int,
    override val physicalCameraId: CameraId?
) : OutputConfigurationWrapper {

    @RequiresApi(24)
    companion object {
        /**
         * Create and validate an OutputConfiguration for Camera2.
         */
        fun create(
            surface: Surface?,
            streamType: StreamType = StreamType.SURFACE,
            size: Size? = null,
            surfaceSharing: Boolean = false,
            surfaceGroupId: Int = SURFACE_GROUP_ID_NONE,
            physicalCameraId: CameraId? = null
        ): OutputConfigurationWrapper {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)

            // Create the OutputConfiguration using the groupId via the constructor (if set)
            val configuration: OutputConfiguration
            if (surface != null) {
                configuration = if (surfaceGroupId != SURFACE_GROUP_ID_NONE) {
                    OutputConfiguration(surfaceGroupId, surface)
                } else {
                    OutputConfiguration(surface)
                }
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    throw IllegalStateException(
                        "Deferred OutputConfigurations are not supported on API " +
                            "${Build.VERSION.SDK_INT} (requires API ${Build.VERSION_CODES.O})"
                    )
                }

                check(size != null) {
                    "Size must defined when creating a deferred OutputConfiguration."
                }

                configuration = OutputConfiguration(
                    size,
                    when (streamType) {
                        StreamType.SURFACE_TEXTURE -> SurfaceTexture::class.java
                        StreamType.SURFACE_VIEW -> SurfaceHolder::class.java
                        StreamType.SURFACE -> throw IllegalArgumentException(
                            "StreamType.Surface is not supported for deferred OutputConfigurations"
                        )
                    }
                )
            }

            // Enable surface sharing, if set.
            if (surfaceSharing) {
                configuration.enableSurfaceSharingCompat()
            }

            // Pass along the physicalCameraId, if set.
            if (physicalCameraId != null) {
                configuration.setPhysicalCameraIdCompat(physicalCameraId)
            }

            // Create and return the Android
            return AndroidOutputConfiguration(
                configuration,
                surfaceSharing,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    configuration.maxSharedSurfaceCount
                } else {
                    1
                },
                physicalCameraId
            )
        }

        private fun OutputConfiguration.enableSurfaceSharingCompat() {
            checkNOrHigher("surfaceSharing")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.enableSurfaceSharing()
            }
        }

        private fun OutputConfiguration.setPhysicalCameraIdCompat(physicalCameraId: CameraId) {
            checkPOrHigher("physicalCameraId")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                this.setPhysicalCameraId(physicalCameraId.value)
            }
        }
    }

    override val surface: Surface? = output.surface
    override val surfaces: List<Surface>
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return output.surfaces
            }

            // On older versions of the OS, only one surface is allowed, and if an output
            // configuration is in a deferred state it may not have a surface when it's first
            // created.
            return listOfNotNull(output.surface)
        }

    override fun addSurface(surface: Surface) {
        checkOOrHigher("addSurface")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            output.addSurface(surface)
        }
    }

    override fun removeSurface(surface: Surface) {
        checkPOrHigher("removeSurface")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            output.removeSurface(surface)
        }
    }

    override val surfaceGroupId: Int
        get() = output.surfaceGroupId

    override fun unwrap(): OutputConfiguration = output

    override fun toString(): String = output.toString()
}
