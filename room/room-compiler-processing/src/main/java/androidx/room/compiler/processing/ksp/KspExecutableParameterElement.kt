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
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_METHOD_PARAMETER
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

internal class KspExecutableParameterElement(
    env: KspProcessingEnv,
    override val enclosingMethodElement: KspExecutableElement,
    val parameter: KSValueParameter
) : KspElement(env, parameter),
    XExecutableParameterElement,
    XAnnotated by KspAnnotated.create(env, parameter, NO_USE_SITE_OR_METHOD_PARAMETER) {

    override val equalityItems: Array<out Any?>
        get() = arrayOf(enclosingMethodElement, parameter)

    override val name: String
        get() = parameter.name?.asString() ?: "_no_param_name"

    override val hasDefaultValue: Boolean
        get() = parameter.hasDefault

    override val type: KspType by lazy {
        parameter.typeAsMemberOf(
            functionDeclaration = enclosingMethodElement.declaration,
            ksType = enclosingMethodElement.containing.type?.ksType
        ).let {
            env.wrap(
                originatingReference = parameter.type,
                ksType = it
            )
        }
    }

    override val fallbackLocationText: String
        get() = "$name in ${enclosingMethodElement.fallbackLocationText}"

    override fun asMemberOf(other: XType): KspType {
        if (enclosingMethodElement.containing.type?.isSameType(other) != false) {
            return type
        }
        check(other is KspType)
        return parameter.typeAsMemberOf(
            functionDeclaration = enclosingMethodElement.declaration,
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

    companion object {
        fun create(
            env: KspProcessingEnv,
            parameter: KSValueParameter
        ): KspExecutableParameterElement {
            val parent = checkNotNull(parameter.parent) {
                "Expected value parameter '$parameter' to contain a parent node."
            }
            if (parent !is KSFunctionDeclaration) {
                error(
                    "Don't know how to create a parameter element whose parent is a " +
                        "'${parent::class}'"
                )
            }
            return KspExecutableParameterElement(
                env = env,
                enclosingMethodElement = KspExecutableElement.create(env, parent),
                parameter = parameter
            )
        }
    }
}
