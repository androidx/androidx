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
 * A strongly typed identifier for a camera frame.
 *
 * A frame number is a unique, monotonically increasing identifier assigned to each frame produced
 * by a camera device. Wrapping this in a value class prevents type confusion with other `Long`
 * identifiers (such as timestamps, request IDs, or camera IDs) and enforces type safety at compile
 * time.
 *
 * As a [JvmInline] value class, it has zero runtime overhead when compiled, as the compiler
 * replaces instances of this class with the underlying [Long] value where possible.
 *
 * ### Example
 *
 * ```
 * val frameNumber = CameraFrameNumber(42L)
 * println(frameNumber.value) // Prints: 42
 * println(frameNumber)       // Prints: Frame-42
 * ```
 *
 * @property value The non-negative `Long` representing the frame identifier.
 * @throws IllegalArgumentException If [value] is negative.
 * @see CaptureResultWrapper.frameNumber
 */
@Suppress("ValueClassDefinition")
@JvmInline
public value class CameraFrameNumber(public val value: Long) {
    init {
        require(value >= 0) { "CameraFrameNumber cannot be negative!" }
    }

    /** Returns a string representation of the frame number in the format "Frame-{value}". */
    override fun toString(): String = "Frame-$value"
}
