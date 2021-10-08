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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.javac.kotlin.KmType
import javax.lang.model.type.DeclaredType

/**
 * Declared types are different from non declared types in java (e.g. primitives, or wildcard
 * types). Even thought XProcessing does not distinguish between these these, in the java
 * implementation, it is handy to have a separate type for explicit typeMirror information.
 */
internal class JavacDeclaredType private constructor(
    env: JavacProcessingEnv,
    override val typeMirror: DeclaredType,
    override val nullability: XNullability,
    override val kotlinType: KmType?
) : JavacType(
    env, typeMirror
) {
    constructor(
        env: JavacProcessingEnv,
        typeMirror: DeclaredType,
        kotlinType: KmType
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = kotlinType.nullability,
        kotlinType = kotlinType
    )

    constructor(
        env: JavacProcessingEnv,
        typeMirror: DeclaredType,
        nullability: XNullability
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = nullability,
        kotlinType = null
    )

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(typeMirror)
    }

    override val typeArguments: List<JavacType> by lazy {
        typeMirror.typeArguments.mapIndexed { index, typeMirror ->
            env.wrap<JavacType>(
                typeMirror = typeMirror,
                kotlinType = kotlinType?.typeArguments?.getOrNull(index),
                elementNullability = XNullability.UNKNOWN
            )
        }
    }

    override fun copyWithNullability(nullability: XNullability): JavacDeclaredType {
        return JavacDeclaredType(
            env = env,
            typeMirror = typeMirror,
            kotlinType = kotlinType,
            nullability = nullability
        )
    }
}
