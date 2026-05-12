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

import androidx.ink.nativeloader.cinterop.DampingNodeNative_create
import androidx.ink.nativeloader.cinterop.DampingNodeNative_getDampingGap
import androidx.ink.nativeloader.cinterop.DampingNodeNative_getDampingSourceInt
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object DampingNodeNative {
    actual fun create(dampingSource: Int, dampingGap: Float): Long =
        DampingNodeNative_create(
            jni_env_pass_through = null,
            dampingSource,
            dampingGap,
            throwForNonOkStatusCallback,
        )

    actual fun getDampingSourceInt(nativePointer: Long): Int =
        DampingNodeNative_getDampingSourceInt(nativePointer)

    actual fun getDampingGap(nativePointer: Long): Float =
        DampingNodeNative_getDampingGap(nativePointer)
}
