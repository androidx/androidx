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

import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import com.squareup.javapoet.TypeVariableName

/**
 * @see KspSyntheticPropertyMethodElement
 */
internal sealed class KspSyntheticPropertyMethodType(
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

    companion object {
        fun create(
            element: KspSyntheticPropertyMethodElement,
            container: XType?
        ): XMethodType {
            return when (element) {
                is KspSyntheticPropertyMethodElement.Getter ->
                    Getter(
                        origin = element,
                        containingType = container
                    )
                is KspSyntheticPropertyMethodElement.Setter ->
                    Setter(
                        origin = element,
                        containingType = container
                    )
            }
        }
    }

    private class Getter(
        origin: KspSyntheticPropertyMethodElement.Getter,
        containingType: XType?
    ) : KspSyntheticPropertyMethodType(
        origin = origin,
        containing = containingType
    ) {
        override val returnType: XType by lazy {
            if (containingType == null) {
                origin.field.type
            } else {
                origin.field.asMemberOf(containingType)
            }
        }
    }

    private class Setter(
        origin: KspSyntheticPropertyMethodElement.Setter,
        containingType: XType?
    ) : KspSyntheticPropertyMethodType(
        origin = origin,
        containing = containingType
    ) {
        override val returnType: XType
            // setters always return Unit, no need to get it as type of
            get() = origin.returnType
    }
}
