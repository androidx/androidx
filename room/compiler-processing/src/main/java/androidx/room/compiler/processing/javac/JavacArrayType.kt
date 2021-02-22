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

import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.javac.kotlin.KmType
import javax.lang.model.type.ArrayType

internal class JavacArrayType private constructor(
    env: JavacProcessingEnv,
    override val typeMirror: ArrayType,
    override val nullability: XNullability,
    private val knownComponentNullability: XNullability?,
    override val kotlinType: KmType?
) : JavacType(
    env,
    typeMirror
),
    XArrayType {
    constructor(
        env: JavacProcessingEnv,
        typeMirror: ArrayType,
        kotlinType: KmType
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = kotlinType.nullability,
        knownComponentNullability = kotlinType.typeArguments.firstOrNull()?.nullability,
        kotlinType = kotlinType
    )

    constructor(
        env: JavacProcessingEnv,
        typeMirror: ArrayType,
        nullability: XNullability,
        knownComponentNullability: XNullability?
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = nullability,
        knownComponentNullability = knownComponentNullability,
        kotlinType = null
    )

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(typeMirror)
    }

    override val typeArguments: List<XType>
        get() = emptyList()

    override val componentType: XType by lazy {
        val componentType = typeMirror.componentType
        val componentTypeNullability =
            knownComponentNullability ?: if (componentType.kind.isPrimitive) {
                XNullability.NONNULL
            } else {
                XNullability.UNKNOWN
            }
        env.wrap<JavacType>(
            typeMirror = componentType,
            kotlinType = kotlinType?.typeArguments?.firstOrNull(),
            elementNullability = componentTypeNullability
        )
    }

    override fun copyWithNullability(nullability: XNullability): JavacType {
        return JavacArrayType(
            env = env,
            typeMirror = typeMirror,
            nullability = nullability,
            knownComponentNullability = knownComponentNullability,
            kotlinType = kotlinType
        )
    }
}