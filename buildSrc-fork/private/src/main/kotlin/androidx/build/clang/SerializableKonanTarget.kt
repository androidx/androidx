/*
 * Copyright 2023 The Android Open Source Project
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
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * We cannot use KonanTarget as Gradle input/output due to
 * https://youtrack.jetbrains.com/issue/KT-61657. Hence, we have this value class which represents
 * it as a string.
 */
@JvmInline
value class SerializableKonanTarget(val name: String) : Serializable {
    init {
        check(KonanTarget.predefinedTargets.contains(name)) { "Invalid KonanTarget name: $name" }
    }

    val asKonanTarget
        get(): KonanTarget {
            return KonanTarget.predefinedTargets[name]
                ?: error("No KonanTarget found with name $name")
        }

    override fun toString() = name

    constructor(konanTarget: KonanTarget) : this(konanTarget.name)
}
