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

import androidx.room.compiler.processing.XDeclaredType
import javax.lang.model.type.DeclaredType

internal class JavacDeclaredType(
    env: JavacProcessingEnv,
    override val typeMirror: DeclaredType
) : JavacType(
    env, typeMirror
), XDeclaredType {
    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(typeMirror)
    }

    override val typeArguments: List<JavacType> by lazy {
        env.wrapTypes<JavacType>(typeMirror.typeArguments)
    }
}
