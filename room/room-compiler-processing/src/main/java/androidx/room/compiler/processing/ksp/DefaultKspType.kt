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

import androidx.room.compiler.processing.tryBox
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

internal class DefaultKspType(
    env: KspProcessingEnv,
    ksType: KSType,
    originalKSAnnotations: Sequence<KSAnnotation> = ksType.annotations,
    scope: KSTypeVarianceResolverScope? = null,
    typeAlias: KSType? = null,
) : KspType(env, ksType, originalKSAnnotations, scope, typeAlias) {

    override fun resolveJTypeName(): JTypeName {
        // Always box these unless for inline value classes. For primitives, typeName might return
        // the primitive type but if we wanted it to be a primitive, we would've resolved it to
        // [KspPrimitiveType]. Inline value classes with primitive values won't be resolved to
        // [KspPrimitiveType] because we need boxed name for Kotlin and unboxed name for Java.
        return if (ksType.declaration.isValueClass()) {
            // Don't box inline value classes, e.g. the type name for `UInt` should be `int`,
            // not `Integer`, if used directly.
            ksType.asJTypeName(env.resolver)
        } else {
            ksType.asJTypeName(env.resolver).tryBox()
        }
    }

    override fun resolveKTypeName(): KTypeName {
        return ksType.asKTypeName(env.resolver)
    }

    override fun boxed(): DefaultKspType {
        return this
    }

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?
    ) = DefaultKspType(env, ksType, originalKSAnnotations, scope, typeAlias)
}
