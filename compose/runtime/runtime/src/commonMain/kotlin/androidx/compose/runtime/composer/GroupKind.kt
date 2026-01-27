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

package androidx.compose.runtime.composer

import kotlin.jvm.JvmInline

/** Group types used with [Composer.start] to differentiate between different types of groups */
@JvmInline
internal value class GroupKind private constructor(val value: Int) {
    inline val isNode
        get() = value != Group.value

    inline val isReusable
        get() = value != Node.value

    companion object {
        val Group = GroupKind(0)
        val Node = GroupKind(1)
        val ReusableNode = GroupKind(2)
    }
}
