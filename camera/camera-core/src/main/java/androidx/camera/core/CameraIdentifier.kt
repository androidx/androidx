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
package androidx.camera.core

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.Identifier
import androidx.core.util.Preconditions

/**
 * An opaque, stable identifier for a camera device recognized by CameraX.
 *
 * This identifier is designed to be unique and stable for a specific camera device across
 * application sessions and device connect/disconnect events, provided the underlying system can
 * consistently identify the camera. It serves as a reliable key for uniquely identifying cameras
 * within CameraX APIs.
 *
 * Applications should not attempt to parse the string representation of this identifier or assume
 * any particular format for its internal value, as it may change across CameraX versions or device
 * implementations.
 *
 * Instances of `CameraIdentifier` are vended by CameraX (e.g., via
 * [CameraInfo.getCameraIdentifier]). Applications should not instantiate this class directly.
 *
 * To re-select a camera for binding, use the [CameraSelector.of] factory method. This class also
 * includes convenience methods like [isOf] to check if this identifier corresponds to a given
 * [Camera] instance.
 *
 * **Selecting a Specific Camera**
 *
 * The primary way to use a `CameraIdentifier` is to re-select that specific camera for binding.
 * This is done by creating a [CameraSelector] with the [CameraSelector.of] static factory method.
 *
 * ```kotlin
 * val cameraProvider = ...
 * val specificId = ... // From a listener or saved preferences
 *
 * // Create a selector that will only match the specific camera
 * val selector = CameraSelector.of(specificId)
 *
 * try {
 * // Bind use cases to this exact camera
 * val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
 * } catch (e: IllegalArgumentException) {
 * // This occurs if the camera with the specific ID is no longer available.
 * }
 * ```
 *
 * **Obtaining CameraInfo from an Identifier**
 *
 * To get the [CameraInfo] for a specific `CameraIdentifier`, you must use a [CameraSelector] to
 * query the [CameraProvider]. This ensures that the camera is currently available.
 *
 * ```kotlin
 * val cameraProvider = ...
 * val identifier = ...
 *
 * try {
 * val info = cameraProvider.getCameraInfo(CameraSelector.of(identifier))
 * // Use the CameraInfo object...
 * } catch (e: IllegalArgumentException) {
 * // This exception is thrown if the camera corresponding to the
 * // identifier is not currently available.
 * }
 * ```
 *
 * @see CameraInfo.getCameraIdentifier
 * @see CameraSelector.of
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CameraIdentifier
private constructor(
    /**
     * The ordered list of camera IDs that this identifier represents.
     *
     * The meaning of this list depends on the context:
     * 1. **For a single camera**, this list will contain exactly one ID from the CameraManager.
     *    This is the most common case and applies to both individual physical cameras and single
     *    logical cameras that are being selected.
     * 2. **For concurrent camera mode**, this list contains the IDs of two or more top-level
     *    cameras (which must all be present in CameraManager's list) that will be opened and used
     *    simultaneously.
     *
     * This list does **not** represent a single logical camera and its constituent physical
     * sensors. Selecting a single logical camera is done using a list with its single ID.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val cameraIds: List<String>,

    /** The compatibility identifier, if one exists. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val compatibilityId: Identifier?,
) {
    init {
        Preconditions.checkArgument(cameraIds.isNotEmpty(), "Camera ID set cannot be empty.")
    }

    /**
     * The single internal ID for this camera.
     *
     * @return The internal string that uniquely identifies this camera.
     * @throws IllegalStateException if this identifier represents a multi-camera (i.e., contains
     *   more than one ID).
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val internalId: String
        get() {
            Preconditions.checkState(
                cameraIds.size == 1,
                "getInternalId() is only available for single-camera identifiers.",
            )
            return cameraIds.first()
        }

    /**
     * Checks if this identifier corresponds to the given [Camera] instance.
     *
     * This is a convenience method that simplifies checking if a `CameraIdentifier` matches a
     * currently bound `Camera` object.
     *
     * **Usage Example:**
     *
     * ```kotlin
     * // Instead of:
     * // if (identifier.equals(myCamera.cameraInfo.cameraIdentifier)) { ... }
     *
     * // You can write:
     * if (identifier.isOf(myCamera)) { ... }
     * ```
     */
    public fun isOf(camera: Camera): Boolean {
        Preconditions.checkNotNull(camera)
        return this == camera.cameraInfo.cameraIdentifier
    }

    /**
     * Checks if this identifier corresponds to the given [CameraInfo] instance.
     *
     * This is a convenience method for checking against a `CameraInfo` object, which might be
     * retrieved from [CameraProvider.availableCameraInfos].
     */
    public fun isOf(cameraInfo: CameraInfo): Boolean {
        Preconditions.checkNotNull(cameraInfo)
        return this == cameraInfo.cameraIdentifier
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraIdentifier) return false

        if (cameraIds != other.cameraIds) return false
        if (compatibilityId != other.compatibilityId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cameraIds.hashCode()
        result = 31 * result + (compatibilityId?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "CameraIdentifier{" +
            "cameraIds=${cameraIds.joinToString(",")}" +
            (compatibilityId?.let { ", compatId=$it" } ?: "") +
            "}"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        /**
         * Creates a new [CameraIdentifier] from an ordered list of camera IDs and an optional
         * compatibility ID.
         *
         * @param cameraIds An ordered list of one or more camera ID strings.
         * @param compatibilityId An optional identifier for special configurations. Defaults to
         *   null.
         */
        @VisibleForTesting
        @JvmOverloads
        @JvmStatic
        public fun create(
            cameraIds: List<String>,
            compatibilityId: Identifier? = null,
        ): CameraIdentifier {
            return CameraIdentifier(cameraIds, compatibilityId)
        }

        /**
         * Creates a new [CameraIdentifier] for a logical camera with a primary and an optional
         * secondary camera.
         *
         * This is a convenience method that preserves the primary-first order.
         *
         * @param primaryCameraId The ID of the primary camera.
         * @param secondaryCameraId The ID of the secondary camera. Defaults to null.
         * @param compatibilityId An optional identifier for special configurations like extensions.
         *   Defaults to null.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            primaryCameraId: String,
            secondaryCameraId: String? = null,
            compatibilityId: Identifier? = null,
        ): CameraIdentifier {
            val cameraIds = mutableListOf(primaryCameraId)
            secondaryCameraId?.let { cameraIds.add(it) }
            return create(cameraIds, compatibilityId)
        }

        /**
         * Creates a composite [CameraIdentifier] from one or two [AdapterCameraInfo] objects.
         *
         * This factory method is specifically designed to create the unique key for a
         * `androidx.camera.core.internal.CameraUseCaseAdapter`. It combines the camera IDs and the
         * compatibility ID from the primary camera's configuration into a single, stable
         * identifier. This is crucial for the `androidx.camera.lifecycle.LifecycleCameraRepository`
         * to correctly look up existing camera instances.
         *
         * @param primaryInfo The non-null info for the primary camera.
         * @param secondaryInfo The optional info for the secondary camera.
         * @return A new [CameraIdentifier] instance representing the adapter's unique identity.
         */
        @JvmStatic
        public fun fromAdapterInfos(
            primaryInfo: AdapterCameraInfo,
            secondaryInfo: AdapterCameraInfo?,
        ): CameraIdentifier {
            val secondaryId = secondaryInfo?.cameraId
            val compatId = primaryInfo.cameraConfig.compatibilityId
            return create(primaryInfo.cameraId, secondaryId, compatId)
        }
    }
}
