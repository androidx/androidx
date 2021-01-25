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

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.tryBox
import com.google.devtools.ksp.symbol.KSType
import com.squareup.javapoet.TypeName

internal class DefaultKspType(
    env: KspProcessingEnv,
    ksType: KSType
) : KspType(env, ksType) {
    override val typeName: TypeName by lazy {
        // always box these. For primitives, typeName might return the primitive type but if we
        // wanted it to be a primitive, we would've resolved it to [KspPrimitiveType].
        ksType.typeName(env.resolver).tryBox()
    }

    override fun boxed(): DefaultKspType {
        return this
    }

    override fun copyWithNullability(nullability: XNullability): KspType {
        return DefaultKspType(
            env = env,
            ksType = ksType.withNullability(nullability)
        )
    }
}