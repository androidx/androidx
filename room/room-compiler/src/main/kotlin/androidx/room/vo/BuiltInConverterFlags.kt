/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.BuiltInTypeConverters

data class BuiltInConverterFlags(
    val enums: BuiltInTypeConverters.State,
    val uuid: BuiltInTypeConverters.State
) {

    /**
     * Returns the combination of `this` flags with the [next] flags.
     * Notice that order is important here as the [next] gets priority when it defines any flag.
     */
    fun withNext(next: BuiltInConverterFlags) = BuiltInConverterFlags(
        enums = enums + next.enums,
        uuid = uuid + next.uuid
    )

    companion object {
        val DEFAULT = BuiltInConverterFlags(
            enums = BuiltInTypeConverters.State.INHERITED,
            uuid = BuiltInTypeConverters.State.INHERITED
        )
    }
}

fun BuiltInTypeConverters.State.isEnabled() = this != BuiltInTypeConverters.State.DISABLED

private operator fun BuiltInTypeConverters.State.plus(
    other: BuiltInTypeConverters.State
): BuiltInTypeConverters.State {
    return when (other) {
        BuiltInTypeConverters.State.INHERITED -> this
        else -> other
    }
}