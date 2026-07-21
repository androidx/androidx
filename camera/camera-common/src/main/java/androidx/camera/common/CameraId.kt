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
 * Strongly typed identifier for a camera device.
 *
 * Use this class to safely represent and pass camera IDs, preventing type confusion with other
 * string identifiers.
 *
 * @param value The non-blank string representing the camera identifier.
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
         * @param value The Camera2 identifier.
         * @return A [CameraId] wrapper.
         */
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public fun fromCamera2Id(value: String): CameraId = CameraId(value)

        /**
         * Creates a [CameraId] from a Camera1 camera identifier integer.
         *
         * @param value The Camera1 integer identifier.
         * @return A [CameraId] wrapper.
         */
        @Suppress("MissingJvmstatic", "ValueClassUsageWithoutJvmName")
        public fun fromCamera1Id(value: Int): CameraId = CameraId("$value")
    }

    override fun toString(): String = "CameraId-$value"
}
