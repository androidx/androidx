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
 * Strongly typed identifier for a camera frame.
 *
 * Use this class to safely represent and pass frame numbers, preventing type confusion with other
 * long identifiers.
 *
 * @param value The non-negative long representing the frame identifier.
 */
@Suppress("ValueClassDefinition")
@JvmInline
public value class CameraFrameNumber(public val value: Long) {
    init {
        require(value >= 0) { "CameraFrameNumber cannot be negative!" }
    }

    override fun toString(): String = "Frame-$value"
}
