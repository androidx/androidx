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

import androidx.ink.nativeloader.cinterop.ToolTypeFilterNodeNative_create
import androidx.ink.nativeloader.cinterop.ToolTypeFilterNodeNative_getMouseEnabled
import androidx.ink.nativeloader.cinterop.ToolTypeFilterNodeNative_getStylusEnabled
import androidx.ink.nativeloader.cinterop.ToolTypeFilterNodeNative_getTouchEnabled
import androidx.ink.nativeloader.cinterop.ToolTypeFilterNodeNative_getUnknownEnabled
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual internal object ToolTypeFilterNodeNative {
    actual fun create(
        mouseEnabled: Boolean,
        touchEnabled: Boolean,
        stylusEnabled: Boolean,
        unknownEnabled: Boolean,
    ): Long =
        ToolTypeFilterNodeNative_create(
            jni_env_pass_through = null,
            mouseEnabled,
            touchEnabled,
            stylusEnabled,
            unknownEnabled,
            throwForNonOkStatusCallback,
        )

    actual fun getMouseEnabled(nativePointer: Long): Boolean =
        ToolTypeFilterNodeNative_getMouseEnabled(nativePointer)

    actual fun getTouchEnabled(nativePointer: Long): Boolean =
        ToolTypeFilterNodeNative_getTouchEnabled(nativePointer)

    actual fun getStylusEnabled(nativePointer: Long): Boolean =
        ToolTypeFilterNodeNative_getStylusEnabled(nativePointer)

    actual fun getUnknownEnabled(nativePointer: Long): Boolean =
        ToolTypeFilterNodeNative_getUnknownEnabled(nativePointer)
}
