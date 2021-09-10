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

package androidx.room.compiler.processing

import com.squareup.javapoet.TypeVariableName
import kotlin.contracts.contract

/**
 * Represents a type information for a method.
 *
 * It is not an XType as it does not represent a class or primitive.
 */
interface XMethodType {
    /**
     * The return type of the method
     */
    val returnType: XType

    /**
     * Parameter types of the method.
     */
    val parameterTypes: List<XType>

    /**
     * Returns the names of [TypeVariableName]s for this executable.
     */
    val typeVariableNames: List<TypeVariableName>
}

/**
 * Returns `true` if this method type represents a suspend function
 */
fun XMethodType.isSuspendFunction(): Boolean {
    contract {
        returns(true) implies (this@isSuspendFunction is XSuspendMethodType)
    }
    return this is XSuspendMethodType
}
