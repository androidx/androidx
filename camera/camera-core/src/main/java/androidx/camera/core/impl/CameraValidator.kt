/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core.impl

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger

/** An interface for validating the camera state against system-advertised features. */
public interface CameraValidator {

    /**
     * Validates the initial set of cameras upon startup.
     *
     * @throws CameraIdListIncorrectException if a camera required by system features is missing.
     */
    @Throws(CameraIdListIncorrectException::class)
    public fun validateOnFirstInit(cameraRepository: CameraRepository)

    /**
     * Checks if a proposed camera removal would result in a degraded state.
     *
     * This method determines if removing a set of cameras would violate the device's hardware
     * contract as defined by system features (e.g., `PackageManager.FEATURE_CAMERA`). A change is
     * considered **invalid** if it removes a **required** camera that is **currently available**,
     * thereby making the camera state "worse".
     *
     * **Examples (Assuming device requires a BACK and FRONT camera):**
     * 1. **Valid:** Removing a non-required `EXTERNAL` camera from `[BACK, FRONT, EXTERNAL]` is
     *    allowed.
     * 2. **Invalid:** Removing the required `BACK` camera from `[BACK, FRONT]` is blocked.
     * 3. **Valid (Degraded State):** If the initial state is `[BACK, EXTERNAL]` (missing the
     *    required `FRONT`), removing the non-required `EXTERNAL` camera is still allowed as the
     *    situation does not worsen.
     *
     * The validation logic is constrained by the `availableCamerasSelector` provided when this
     * validator was created.
     *
     * @param currentCameras The complete set of `CameraInternal` objects available *before* the
     *   proposed removal.
     * @param removedCameras The set of `CameraIdentifier`s for the cameras that are being removed.
     * @return `true` if the change is invalid and should be aborted, `false` otherwise.
     */
    public fun isChangeInvalid(
        currentCameras: Set<CameraInternal>,
        removedCameras: Set<CameraIdentifier>,
    ): Boolean

    /** The exception for an incorrect camera id list. */
    public class CameraIdListIncorrectException(
        message: String?,
        public val availableCameraCount: Int,
        cause: Throwable?,
    ) : Exception(message, cause)

    /** Companion object to hold the factory method. */
    public companion object {
        /**
         * Creates a new instance of the default CameraValidator.
         *
         * @param context The application context.
         * @param availableCamerasSelector The selector that filters which cameras to validate.
         * @return A new [CameraValidator] instance.
         */
        @JvmStatic
        public // Makes this callable as a static method from Java
        fun create(context: Context, availableCamerasSelector: CameraSelector?): CameraValidator {
            return CameraValidatorImpl(context, availableCamerasSelector)
        }
    }
}

/**
 * The default implementation of [CameraValidator].
 *
 * This validator is configured with a context and an optional camera selector upon creation.
 */
public class CameraValidatorImpl(
    private val context: Context,
    private val availableCamerasSelector: CameraSelector?,
) : CameraValidator {

    private val isVirtualDevice = isVirtualDevice(context)
    private val validationCriteria = getValidationCriteria()

    override fun validateOnFirstInit(cameraRepository: CameraRepository) {
        if (isVirtualDevice) {
            Logger.d(
                TAG,
                "Virtual device with " +
                    "${cameraRepository.cameras.size} cameras. Skipping validation.",
            )
            return
        }

        Logger.d(TAG, "Verifying camera lens facing on " + Build.DEVICE)
        var exception: RuntimeException? = null

        if (validationCriteria.checkBack) {
            try {
                CameraSelector.DEFAULT_BACK_CAMERA.select(cameraRepository.cameras)
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Camera LENS_FACING_BACK verification failed", e)
                exception = e
            }
        }

        if (validationCriteria.checkFront) {
            try {
                CameraSelector.DEFAULT_FRONT_CAMERA.select(cameraRepository.cameras)
            } catch (e: RuntimeException) {
                Logger.w(TAG, "Camera LENS_FACING_FRONT verification failed", e)
                if (exception == null) exception = e
            }
        }

        if (exception != null) {
            throw CameraValidator.CameraIdListIncorrectException(
                "Expected camera missing from device.",
                cameraRepository.cameras.size,
                exception,
            )
        }
    }

    override fun isChangeInvalid(
        currentCameras: Set<CameraInternal>,
        removedCameras: Set<CameraIdentifier>,
    ): Boolean {
        if (isVirtualDevice || (!validationCriteria.checkBack && !validationCriteria.checkFront)) {
            return false
        }

        val hadBack = hasCamera(currentCameras, CameraSelector.DEFAULT_BACK_CAMERA)
        val hadFront = hasCamera(currentCameras, CameraSelector.DEFAULT_FRONT_CAMERA)

        val removedCameraIds = removedCameras.map { it.internalId }.toSet()
        val newProposedCameras =
            currentCameras.filter { it.cameraInfoInternal.cameraId !in removedCameraIds }.toSet()

        val willHaveBack = hasCamera(newProposedCameras, CameraSelector.DEFAULT_BACK_CAMERA)
        val willHaveFront = hasCamera(newProposedCameras, CameraSelector.DEFAULT_FRONT_CAMERA)

        val backCameraIsLost = validationCriteria.checkBack && hadBack && !willHaveBack
        val frontCameraIsLost = validationCriteria.checkFront && hadFront && !willHaveFront

        return backCameraIsLost || frontCameraIsLost
    }

    private fun hasCamera(cameras: Set<CameraInternal>, selector: CameraSelector): Boolean {
        return try {
            selector.select(LinkedHashSet(cameras))
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private data class ValidationCriteria(val checkBack: Boolean, val checkFront: Boolean)

    private fun getValidationCriteria(): ValidationCriteria {
        val pm = context.packageManager
        val lensFacing = this.availableCamerasSelector?.lensFacing
        val needsBackCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        val needsFrontCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

        val checkBack =
            needsBackCamera && (lensFacing == null || lensFacing == CameraSelector.LENS_FACING_BACK)
        val checkFront =
            needsFrontCamera &&
                (lensFacing == null || lensFacing == CameraSelector.LENS_FACING_FRONT)
        return ValidationCriteria(checkBack, checkFront)
    }

    private fun isVirtualDevice(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            Api34Impl.getDeviceId(context) != Context.DEVICE_ID_DEFAULT
    }

    @RequiresApi(34)
    private object Api34Impl {
        fun getDeviceId(context: Context): Int {
            return context.getDeviceId()
        }
    }

    public companion object {
        private const val TAG = "CameraValidator"
    }
}
