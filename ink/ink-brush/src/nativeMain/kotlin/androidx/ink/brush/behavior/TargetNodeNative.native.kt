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

import androidx.ink.nativeloader.cinterop.TargetNodeNative_create
import androidx.ink.nativeloader.cinterop.TargetNodeNative_getModifierRangeEnd
import androidx.ink.nativeloader.cinterop.TargetNodeNative_getModifierRangeStart
import androidx.ink.nativeloader.cinterop.TargetNodeNative_getTargetInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object TargetNodeNative {
    actual fun create(
        target: Int,
        targetModifierRangeStart: Float,
        targetModifierRangeEnd: Float,
    ): Long =
        TargetNodeNative_create(
            jni_env_pass_through = null,
            target,
            targetModifierRangeStart,
            targetModifierRangeEnd,
            throwForNonOkStatusCallback,
        )

    actual fun getTargetInt(nativePointer: Long): Int = TargetNodeNative_getTargetInt(nativePointer)

    actual fun getModifierRangeStart(nativePointer: Long): Float =
        TargetNodeNative_getModifierRangeStart(nativePointer)

    actual fun getModifierRangeEnd(nativePointer: Long): Float =
        TargetNodeNative_getModifierRangeEnd(nativePointer)
}
