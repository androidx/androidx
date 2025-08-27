/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.composer.gapbuffer

import androidx.compose.runtime.JoinedKey

/** Information about groups and their keys. */
internal class KeyInfo
internal constructor(
    /** The group key. */
    val key: Int,

    /** The object key for the group */
    val objectKey: Any?,

    /** The location of the group. */
    val location: Int,

    /** The number of nodes in the group. If the group is a node this is always 1. */
    val nodes: Int,

    /** The index of the key info in the list returned by extractKeys */
    val index: Int,
)

internal val KeyInfo.joinedKey: Any
    get() = if (objectKey != null) JoinedKey(key, objectKey) else key
