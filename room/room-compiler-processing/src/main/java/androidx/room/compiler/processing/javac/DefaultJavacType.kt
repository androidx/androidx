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
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.javac.kotlin.KmType
import javax.lang.model.type.TypeMirror

/**
 * Catch-all class for XType implementation when we don't need/discover a sub-type
 */
internal class DefaultJavacType private constructor(
    env: JavacProcessingEnv,
    typeMirror: TypeMirror,
    override val nullability: XNullability,
    override val kotlinType: KmType?
) : JavacType(
    env, typeMirror
) {
    constructor(
        env: JavacProcessingEnv,
        typeMirror: TypeMirror,
        kotlinType: KmType
    ) : this(
        env = env,
        typeMirror = typeMirror,
        nullability = kotlinType.nullability,
        kotlinType = kotlinType
    )

    constructor(
        env: JavacProcessingEnv,
        typeMirror: TypeMirror,
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
        /**
         * This is always empty because if the type mirror is declared, we wrap it in a
         * JavacDeclaredType.
         */
        get() = emptyList()

    override fun copyWithNullability(nullability: XNullability): JavacType {
        return DefaultJavacType(
            env = env,
            typeMirror = typeMirror,
            kotlinType = kotlinType,
            nullability = nullability
        )
    }
}