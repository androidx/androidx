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

package androidx.camera.common

/**
 * A type-safe identifier for a camera device.
 *
 * This value class wraps a [String] representation of a camera ID. Using [CameraId] instead of raw
 * strings or integers helps prevent type confusion and ensures that APIs receiving or returning
 * camera identifiers are self-documenting and type-safe.
 *
 * [CameraId] instances are typically created using the factory methods in the companion object:
 * [fromCamera2Id] for Camera2 string identifiers, or [fromCamera1Id] for legacy Camera1 integer
 * identifiers.
 *
 * @property value The underlying string representation of the camera identifier. Must not be empty.
 */
@Suppress("ValueClassDefinition")
@JvmInline
public value class CameraId(public val value: String) {
    init {
        require(value.isNotEmpty()) { "CameraId cannot be empty!" }
    }

    public companion object {
        /**
         * Creates a [CameraId] from a Camera2 camera identifier string.
         *
         * Camera2 identifiers are typically obtained from
         * `android.hardware.camera2.CameraManager.getCameraIdList()`.
         *
         * @param value The Camera2 identifier string.
         * @return A [CameraId] representing the given identifier.
         * @throws IllegalArgumentException if [value] is empty.
         */
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public fun fromCamera2Id(value: String): CameraId = CameraId(value)

        /**
         * Creates a [CameraId] from a legacy Camera1 camera identifier integer.
         *
         * Camera1 identifiers are typically zero-based indices used with
         * `android.hardware.Camera.open(int)`.
         *
         * @param value The Camera1 integer identifier.
         * @return A [CameraId] representing the given identifier as a string.
         */
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public fun fromCamera1Id(value: Int): CameraId = CameraId("$value")
    }

    /**
     * Returns a string representation of the [CameraId] for debugging purposes, in the format
     * "CameraId-value".
     */
    override fun toString(): String = "CameraId-$value"
}
