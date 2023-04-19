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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KSTypeVarianceResolverScope
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.squareup.javapoet.TypeVariableName

/**
 * @see KspSyntheticPropertyMethodElement
 */
internal sealed class KspSyntheticPropertyMethodType(
    val env: KspProcessingEnv,
    val origin: KspSyntheticPropertyMethodElement,
    val containing: XType?
) : XMethodType {

    override val parameterTypes: List<XType> by lazy {
        if (containing == null) {
            origin.parameters.map {
                it.type
            }
        } else {
            origin.parameters.map {
                it.asMemberOf(containing)
            }
        }
    }

    override val typeVariableNames: List<TypeVariableName>
        get() = emptyList()

    override val thrownTypes: List<XType>
        // The thrown types are the same as on the origin since those can't change
        get() = origin.thrownTypes

    override fun isSameType(other: XExecutableType): Boolean {
        return env.isSameType(this, other)
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            element: KspSyntheticPropertyMethodElement,
            container: XType?
        ): XMethodType {
            return when (element.accessor) {
                is KSPropertyGetter ->
                    Getter(
                        env = env,
                        origin = element,
                        containingType = container
                    )
                is KSPropertySetter ->
                    Setter(
                        env = env,
                        origin = element,
                        containingType = container
                    )
                else -> error("Unexpected accessor type for $element (${element.accessor})")
            }
        }
    }

    private class Getter(
        env: KspProcessingEnv,
        origin: KspSyntheticPropertyMethodElement,
        containingType: XType?
    ) : KspSyntheticPropertyMethodType(
        env = env,
        origin = origin,
        containing = containingType
    ) {
        override val returnType: XType by lazy {
            if (containingType == null) {
                origin.field.type
            } else {
                origin.field.asMemberOf(containingType)
            }.copyWithScope(
                KSTypeVarianceResolverScope.PropertyGetterMethodReturnType(
                    getterMethod = origin as KspSyntheticPropertyMethodElement.Getter
                )
            )
        }
    }

    private class Setter(
        env: KspProcessingEnv,
        origin: KspSyntheticPropertyMethodElement,
        containingType: XType?
    ) : KspSyntheticPropertyMethodType(
        env = env,
        origin = origin,
        containing = containingType
    ) {
        override val returnType: XType
            // setters always return Unit, no need to get it as type of
            get() = origin.returnType
    }
}