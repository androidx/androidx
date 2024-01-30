/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeVariableType
import androidx.room.compiler.processing.javac.kotlin.KmBaseTypeContainer
import com.google.auto.common.MoreTypes.asIntersection
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeVariable

internal class JavacTypeVariableType(
    env: JavacProcessingEnv,
    override val typeMirror: TypeVariable,
    nullability: XNullability?,
    override val kotlinType: KmBaseTypeContainer?
) : JavacType(env, typeMirror, nullability), XTypeVariableType {
    constructor(
        env: JavacProcessingEnv,
        typeMirror: TypeVariable
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = null,
        kotlinType = null
    )

    constructor(
        env: JavacProcessingEnv,
        typeMirror: TypeVariable,
        kotlinType: KmBaseTypeContainer
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = kotlinType.nullability,
        kotlinType = kotlinType
    )

    constructor(
        env: JavacProcessingEnv,
        typeMirror: TypeVariable,
        nullability: XNullability
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = nullability,
        kotlinType = null
    )

    override val equalityItems by lazy {
        arrayOf(typeMirror)
    }

    override val typeArguments: List<XType>
        get() = emptyList()

    override val upperBounds: List<XType>
        get() {
            return if (typeMirror.upperBound.kind == TypeKind.INTERSECTION) {
                asIntersection(typeMirror.upperBound).bounds.mapIndexed { i, bound ->
                    env.wrap(
                        typeMirror = bound,
                        kotlinType = kotlinType?.upperBounds?.getOrNull(i),
                        elementNullability = maybeNullability
                    )
                }
            } else {
                listOf(
                    env.wrap(
                        typeMirror = typeMirror.upperBound,
                        // If this isn't an intersection type then there is only 1 upper bound
                        kotlinType = kotlinType?.upperBounds?.singleOrNull(),
                        elementNullability = maybeNullability
                    )
                )
            }
        }

    override fun copyWithNullability(nullability: XNullability): JavacTypeVariableType {
        return JavacTypeVariableType(
            env = env,
            typeMirror = typeMirror,
            kotlinType = kotlinType,
            nullability = nullability
        )
    }
}
