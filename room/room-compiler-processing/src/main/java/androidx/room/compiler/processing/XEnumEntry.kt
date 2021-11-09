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

package androidx.room.compiler.processing

import kotlin.contracts.contract

/**
 * Represents a named entry within an enum class.
 */
interface XEnumEntry : XElement {
    /**
     * The name of this enum object.
     */
    val name: String

    /**
     * The parent enum type declaration that holds all entries for this enum type..
     */
    val enumTypeElement: XEnumTypeElement
}

fun XElement.isEnumEntry(): Boolean {
    contract {
        returns(true) implies (this@isEnumEntry is XEnumEntry)
    }
    return this is XEnumEntry
}