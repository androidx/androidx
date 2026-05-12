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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

@UsedByNative
actual internal object BrushBehaviorNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    actual external fun createFromOrderedNodes(
        orderdNodeNativePointers: LongArray,
        developerComment: String,
    ): Long

    @UsedByNative actual external fun free(nativePointer: Long)

    @UsedByNative actual external fun getNodeCount(nativePointer: Long): Int

    @UsedByNative actual external fun getNodeTypeInt(nativePointer: Long, index: Int): Int

    @UsedByNative actual external fun getDeveloperComment(nativePointer: Long): String

    @UsedByNative actual external fun newCopyOfNode(nativePointer: Long, index: Int): Long
}
