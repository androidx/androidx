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

package androidx.build.clang

import java.io.Serializable

/** Serializable wrapper for [NativeTarget] to be used as Gradle task input/output. */
data class SerializableNativeTarget(val name: String) : Serializable {

    constructor(target: NativeTarget) : this(target.name)

    init {
        // Check name is valid
        NativeTarget.fromName(name)
    }

    val asNativeTarget: NativeTarget
        get() = NativeTarget.fromName(name)

    override fun toString() = name

    companion object {
        private const val serialVersionUID: Long = 119394285023L
    }
}
