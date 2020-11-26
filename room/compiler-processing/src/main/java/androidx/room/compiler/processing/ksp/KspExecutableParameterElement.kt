/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.METHOD_PARAMETER
import com.google.devtools.ksp.symbol.KSValueParameter

internal class KspExecutableParameterElement(
    val env: KspProcessingEnv,
    val method: KspExecutableElement,
    val parameter: KSValueParameter
) : XExecutableParameterElement,
    XEquality,
    XAnnotated by KspAnnotated.create(env, parameter, METHOD_PARAMETER) {

    override val equalityItems: Array<out Any?>
        get() = arrayOf(method, parameter)

    override val name: String
        get() = parameter.name?.asString() ?: "_no_param_name"

    override val type: KspType by lazy {
        parameter.typeAsMemberOf(
            resolver = env.resolver,
            functionDeclaration = method.declaration,
            ksType = method.containing.declaration.asStarProjectedType()
        ).let {
            env.wrap(
                originatingReference = parameter.type,
                ksType = it
            )
        }
    }

    override fun asMemberOf(other: XDeclaredType): KspType {
        if (method.containing.type.isSameType(other)) {
            return type
        }
        check(other is KspType)
        return parameter.typeAsMemberOf(
            resolver = env.resolver,
            functionDeclaration = method.declaration,
            ksType = other.ksType
        ).let {
            env.wrap(
                originatingReference = parameter.type,
                ksType = it
            )
        }
    }

    override fun kindName(): String {
        return "function parameter"
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }
}
