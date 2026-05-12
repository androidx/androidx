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

import androidx.ink.nativeloader.cinterop.IntegralNodeNative_create
import androidx.ink.nativeloader.cinterop.IntegralNodeNative_getIntegrateOverInt
import androidx.ink.nativeloader.cinterop.IntegralNodeNative_getOutOfRangeBehaviorInt
import androidx.ink.nativeloader.cinterop.IntegralNodeNative_getValueRangeEnd
import androidx.ink.nativeloader.cinterop.IntegralNodeNative_getValueRangeStart
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object IntegralNodeNative {
    actual fun create(
        integrateOver: Int,
        integralValueRangeStart: Float,
        integralValueRangeEnd: Float,
        integralOutOfRangeBehavior: Int,
    ): Long =
        IntegralNodeNative_create(
            jni_env_pass_through = null,
            integrateOver,
            integralValueRangeStart,
            integralValueRangeEnd,
            integralOutOfRangeBehavior,
            throwForNonOkStatusCallback,
        )

    actual fun getIntegrateOverInt(nativePointer: Long): Int =
        IntegralNodeNative_getIntegrateOverInt(nativePointer)

    actual fun getValueRangeStart(nativePointer: Long): Float =
        IntegralNodeNative_getValueRangeStart(nativePointer)

    actual fun getValueRangeEnd(nativePointer: Long): Float =
        IntegralNodeNative_getValueRangeEnd(nativePointer)

    actual fun getOutOfRangeBehaviorInt(nativePointer: Long): Int =
        IntegralNodeNative_getOutOfRangeBehaviorInt(nativePointer)
}
