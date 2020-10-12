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

import com.squareup.javapoet.TypeName
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSTypeParameter

/**
 * The typeName for type arguments requires the type parameter, hence we have a special type
 * for them when we produce them.
 */
internal class KspTypeArgumentType(
    env: KspProcessingEnv,
    val typeParam: KSTypeParameter,
    val typeArg: KSTypeArgument
) : KspType(
    env = env,
    ksType = typeArg.requireType()
) {
    override val typeName: TypeName by lazy {
        typeArg.typeName(typeParam)
    }
}
