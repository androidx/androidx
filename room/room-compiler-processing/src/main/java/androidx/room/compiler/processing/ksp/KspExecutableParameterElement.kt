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
import androidx.room.compiler.processing.isArray
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_METHOD_PARAMETER
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertySetter
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

    override fun isVarArgs() = parameter.isVararg

    override val name: String
        get() = parameter.name?.asString() ?: "_no_param_name"

    override val jvmName: String
        get() = name.sanitizeAsJavaParameterName(parameterIndex)

    override val hasDefaultValue: Boolean
        get() = parameter.hasDefault

    override val type: KspType by lazy {
        createAsMemberOf(closestMemberContainer.type)
    }

    override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    override val fallbackLocationText: String
        get() = "$name in ${enclosingElement.fallbackLocationText}"

    override fun asMemberOf(other: XType): KspType {
        return if (closestMemberContainer.type?.isSameType(other) != false) {
            type
        } else {
            createAsMemberOf(other)
        }
    }

    private fun createAsMemberOf(container: XType?): KspType {
        check(container is KspType?)
        val resolvedType = parameter.type.resolve()
        val type = env.wrap(
            originalAnnotations = parameter.type.annotations,
            ksType = parameter.typeAsMemberOf(
                functionDeclaration = enclosingElement.declaration,
                ksType = container?.ksType,
                resolved = resolvedType
            ),
            allowPrimitives = !resolvedType.isTypeParameter()
        ).copyWithScope(
            KSTypeVarianceResolverScope.MethodParameter(
                kspExecutableElement = enclosingElement,
                parameterIndex = parameterIndex,
                annotated = parameter.type,
                container = container?.ksType?.declaration,
                asMemberOf = container,
            )
        )
        // In KSP2 the varargs have the component type instead of the array type. We make it always
        // return the array type in XProcessing.
        return if (isVarArgs() && !type.isArray()) {
            env.getArrayType(type)
        } else {
            type
        }
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
                    env, parent, isSyntheticStatic = false
                ).parameters.single()
                else -> error(
                    "Don't know how to create a parameter element whose parent is a " +
                        "'${parent::class}'"
                )
            }
        }
    }
}
