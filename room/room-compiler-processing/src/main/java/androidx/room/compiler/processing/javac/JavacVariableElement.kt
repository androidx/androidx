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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.javac.kotlin.KmTypeContainer
import com.google.auto.common.MoreTypes
import javax.lang.model.element.VariableElement

internal abstract class JavacVariableElement(
    env: JavacProcessingEnv,
    override val element: VariableElement
) : JavacElement(env, element), XVariableElement {

    abstract val kotlinType: KmTypeContainer?

    override val type: JavacType by lazy {
        env.wrap(
            typeMirror = element.asType(),
            kotlinType = kotlinType,
            elementNullability = element.nullability
        )
    }

    override fun asMemberOf(other: XType): JavacType {
        return if (closestMemberContainer.type?.isSameType(other) == true) {
            type
        } else {
            check(other is JavacDeclaredType)
            val asMember = MoreTypes.asMemberOf(env.typeUtils, other.typeMirror, element)
            env.wrap(
                typeMirror = asMember,
                kotlinType = kotlinType,
                elementNullability = element.nullability
            )
        }
    }
}
