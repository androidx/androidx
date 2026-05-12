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

package androidx.ink.brush

import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_createFromOrderedNodes
import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_free
import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_getDeveloperComment
import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_getNodeCount
import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_getNodeTypeInt
import androidx.ink.nativeloader.cinterop.BrushBehaviorNative_newCopyOfNode
import androidx.ink.nativeloader.throwForNonOkStatusCallback
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual internal object BrushBehaviorNative {
    actual fun createFromOrderedNodes(
        orderdNodeNativePointers: LongArray,
        developerComment: String,
    ): Long =
        orderdNodeNativePointers.usePinned { pinned ->
            BrushBehaviorNative_createFromOrderedNodes(
                jni_env_pass_through = null,
                if (orderdNodeNativePointers.isEmpty()) null else pinned.addressOf(0),
                orderdNodeNativePointers.size,
                developerComment,
                throwForNonOkStatusCallback,
            )
        }

    actual fun free(nativePointer: Long) = BrushBehaviorNative_free(nativePointer)

    actual fun getNodeCount(nativePointer: Long): Int =
        BrushBehaviorNative_getNodeCount(nativePointer)

    actual fun getNodeTypeInt(nativePointer: Long, index: Int): Int =
        BrushBehaviorNative_getNodeTypeInt(nativePointer, index)

    actual fun getDeveloperComment(nativePointer: Long): String =
        BrushBehaviorNative_getDeveloperComment(nativePointer)?.toKString() ?: ""

    actual fun newCopyOfNode(nativePointer: Long, index: Int): Long =
        BrushBehaviorNative_newCopyOfNode(nativePointer, index)
}
