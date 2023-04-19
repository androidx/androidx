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
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_METHOD_PARAMETER
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter

internal class KspExecutableParameterElement(
    env: KspProcessingEnv,
    override val enclosingElement: KspExecutableElement,
    val parameter: KSValueParameter,
    val parameterIndex: Int
) : KspElement(env, parameter),
    XExecutableParameterElement,
    XAnnotated by KspAnnotated.create(env, parameter, NO_USE_SITE_OR_METHOD_PARAMETER) {
    override fun isContinuationParam() = false

    override fun isReceiverParam() = false

    override fun isKotlinPropertyParam() = false

    override val name: String
        get() = parameter.name?.asString() ?: "_no_param_name"

    override val hasDefaultValue: Boolean
        get() = parameter.hasDefault

    override val type: KspType by lazy {
        asMemberOf(enclosingElement.enclosingElement.type?.ksType)
    }

    override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    override val fallbackLocationText: String
        get() = "$name in ${enclosingElement.fallbackLocationText}"

    override fun asMemberOf(other: XType): KspType {
        if (closestMemberContainer.type?.isSameType(other) != false) {
            return type
        }
        check(other is KspType)
        return asMemberOf(other.ksType)
    }

    private fun asMemberOf(ksType: KSType?): KspType {
        return env.wrap(
            originatingReference = parameter.type,
            ksType = parameter.typeAsMemberOf(
                functionDeclaration = enclosingElement.declaration,
                ksType = ksType
            )
        ).copyWithScope(
            KSTypeVarianceResolverScope.MethodParameter(
                kspExecutableElement = enclosingElement,
                parameterIndex = parameterIndex,
                annotated = parameter.type,
                container = ksType?.declaration
            )
        )
    }

    override fun kindName(): String {
        return "function parameter"
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            parameter: KSValueParameter
        ): XExecutableParameterElement {
            val parent = checkNotNull(parameter.parent) {
                "Expected value parameter '$parameter' to contain a parent node."
            }
            return when (parent) {
                is KSFunctionDeclaration -> {
                    val parameterIndex = parent.parameters.indexOf(parameter)
                    check(parameterIndex > -1) {
                        "Cannot find $parameter in $parent"
                    }
                    KspExecutableParameterElement(
                        env = env,
                        enclosingElement = KspExecutableElement.create(env, parent),
                        parameter = parameter,
                        parameterIndex = parameterIndex,
                    )
                }
                is KSPropertySetter -> KspSyntheticPropertyMethodElement.create(
                    env, parent
                ).parameters.single()
                else -> error(
                    "Don't know how to create a parameter element whose parent is a " +
                        "'${parent::class}'"
                )
            }
        }
    }
}
