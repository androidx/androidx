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

package androidx.camera.camera2.pipe

/**
 * Enum to know the status of 3A operation in case the method returns before the desired
 * operation is complete. The reason could be that the operation was talking a lot longer and an
 * enforced frame or time limit was reached.
 */
enum class Status3A {
    OK,
    FRAME_LIMIT_REACHED,
    TIME_LIMIT_REACHED
}

/**
 * Return type for a 3A method.
 *
 * @param frameNumber the latest [FrameNumber] at which the method succeeded or was aborted.
 * @param status [Status3A] of the 3A operation at the time of return.
 */
data class Result3A(val frameNumber: FrameNumber, val status: Status3A)