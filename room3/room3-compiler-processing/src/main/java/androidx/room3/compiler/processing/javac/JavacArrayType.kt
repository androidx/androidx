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

package androidx.room3.compiler.processing.javac

import androidx.room3.compiler.codegen.JArrayTypeName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XArrayType
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeArgument
import androidx.room3.compiler.processing.javac.kotlin.KmTypeContainer
import javax.lang.model.type.ArrayType

internal class JavacArrayType(
    env: JavacProcessingEnv,
    override val typeMirror: ArrayType,
    nullability: XNullability? = null,
    knownComponentNullability: XNullability? = null,
    override val kotlinType: KmTypeContainer? = null,
) : JavacType(env, typeMirror, nullability), XArrayType {
    private val componentNullability: XNullability? =
        knownComponentNullability ?: kotlinType?.typeArguments?.firstOrNull()?.nullability

    override val equalityItems: Array<out Any?> by lazy { arrayOf(typeMirror) }

    private val xTypeName: XTypeName by lazy {
        XTypeName(
            java = JArrayTypeName.get(typeMirror),
            kotlin = XTypeName.UNAVAILABLE_KTYPE_NAME,
            nullability = componentNullability ?: XNullability.UNKNOWN,
        )
    }

    override fun asTypeName() = xTypeName

    override val typeArguments: List<XTypeArgument>
        get() = emptyList()

    override val componentType: XType by lazy {
        val componentType = typeMirror.componentType
        val componentTypeNullability =
            componentNullability
                ?: if (componentType.kind.isPrimitive) {
                    XNullability.NONNULL
                } else {
                    XNullability.UNKNOWN
                }
        env.wrap<JavacType>(
            typeMirror = componentType,
            kotlinType = kotlinType?.typeArguments?.firstOrNull(),
            elementNullability = componentTypeNullability,
        )
    }

    override fun copyWithNullability(nullability: XNullability): JavacType {
        return JavacArrayType(
            env = env,
            typeMirror = typeMirror,
            nullability = nullability,
            knownComponentNullability = componentNullability,
            kotlinType = kotlinType,
        )
    }
}
