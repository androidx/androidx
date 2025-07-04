/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.codegen

/** Represents a name (e.g. method name) which may differ in Java and Kotlin. */
class XName internal constructor(internal val java: String, internal val kotlin: String) {
    companion object {
        @JvmStatic fun of(name: String) = of(java = name, kotlin = name)

        @JvmStatic fun of(java: String, kotlin: String) = XName(java, kotlin)
    }
}
