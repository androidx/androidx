/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.brush.behavior

import androidx.ink.nativeloader.cinterop.InterpolationNodeNative_create
import androidx.ink.nativeloader.cinterop.InterpolationNodeNative_getInterpolationInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object InterpolationNodeNative {
    actual fun create(interpolation: Int): Long =
        InterpolationNodeNative_create(
            jni_env_pass_through = null,
            interpolation,
            throwForNonOkStatusCallback,
        )

    actual fun getInterpolationInt(nativePointer: Long): Int =
        InterpolationNodeNative_getInterpolationInt(nativePointer)
}
