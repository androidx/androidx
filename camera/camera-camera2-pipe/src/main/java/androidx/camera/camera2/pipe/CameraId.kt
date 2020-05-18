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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.camera.camera2.pipe

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class CameraId(val value: String) {
    companion object {
        inline fun fromCamera2Id(value: String): CameraId = CameraId(value)
        inline fun fromCamera1Id(value: Int): CameraId = CameraId("$value")
    }

    /**
     * Attempt to parse an camera1 id from a camera2 id.
     *
     * @return The parsed Camera1 id, or null if the value cannot be parsed as a Camera1 id.
     */
    inline fun toCamera1Id(): Int? = value.toIntOrNull()
}
