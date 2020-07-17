/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import kotlin.contracts.contract

interface XElement {
    val name: String

    val packageName: String

    val enclosingElement: XElement?

    fun isPublic(): Boolean

    fun isProtected(): Boolean

    fun isAbstract(): Boolean

    fun isPrivate(): Boolean

    fun isStatic(): Boolean

    fun isTransient(): Boolean

    fun isFinal(): Boolean

    fun kindName(): String

    fun asTypeElement() = this as XTypeElement

    fun asDeclaredType(): XDeclaredType {
        return asTypeElement().type
    }
}

// we keep these as extension methods to be able to use contracts
fun XElement.isType(): Boolean {
    contract {
        returns(true) implies (this@isType is XTypeElement)
    }
    return this is XTypeElement
}
