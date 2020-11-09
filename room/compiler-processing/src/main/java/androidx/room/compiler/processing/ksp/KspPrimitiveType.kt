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

import com.google.devtools.ksp.symbol.KSType
import com.squareup.javapoet.TypeName

/**
 * This tries to mimic primitive types in Kotlin.
 *
 * Primitiveness of a type cannot always be driven from itself (e.g. its nullability).
 * For instance, a kotlin.Int might be non-null but still be non primitive if it is derived from a
 * generic type argument or is part of type parameters.
 */
internal class KspPrimitiveType(
    env: KspProcessingEnv,
    ksType: KSType
) : KspType(env, ksType) {
    override val typeName: TypeName by lazy {
        // TODO once we generate type names in java realm, this custom conversion won't be necessary
        // for now, temporarily, we only do conversion here in place
        return@lazy checkNotNull(
            KspTypeMapper.getPrimitiveJavaTypeName(
                ksType.declaration.qualifiedName!!.asString()
            )
        ) {
            "Internal error. Should've found a java primitive for " +
                "${ksType.declaration.qualifiedName}"
        }
    }
}
