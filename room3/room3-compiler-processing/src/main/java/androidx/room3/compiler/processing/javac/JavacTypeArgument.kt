/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XEquality
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeArgument
import androidx.room3.compiler.processing.XVariance
import androidx.room3.compiler.processing.javac.kotlin.KmBaseTypeContainer
import androidx.room3.compiler.processing.safeTypeName
import com.google.auto.common.MoreTypes.asWildcard
import com.squareup.kotlinpoet.javapoet.JClassName
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal class JavacTypeArgument(
    val env: JavacProcessingEnv,
    val typeMirror: TypeMirror,
    kotlinType: KmBaseTypeContainer? = null,
    elementNullability: XNullability? = null,
) : XTypeArgument, XEquality {

    override val type: JavacType by lazy {
        when (variance) {
            XVariance.INVARIANT -> typeMirror
            XVariance.OUT -> asWildcard(typeMirror).extendsBound
            XVariance.IN -> asWildcard(typeMirror).superBound
            XVariance.STAR ->
                env.elementUtils.getTypeElement(JClassName.OBJECT.canonicalName()).asType()
        }.let { env.wrap(typeMirror = it, kotlinType, elementNullability) }
    }

    override val variance: XVariance by lazy {
        when (typeMirror.kind) {
            TypeKind.WILDCARD -> {
                val wildcard = asWildcard(typeMirror)
                when {
                    (wildcard.superBound != null) -> XVariance.IN
                    (wildcard.extendsBound != null) -> XVariance.OUT
                    else -> XVariance.STAR
                }
            }
            else -> XVariance.INVARIANT
        }
    }

    override fun asTypeName(): XTypeName {
        return XTypeName(
            typeMirror.safeTypeName(),
            XTypeName.UNAVAILABLE_KTYPE_NAME,
            type.maybeNullability ?: XNullability.UNKNOWN,
        )
    }

    override val equalityItems: Array<out Any?> by lazy { arrayOf(typeMirror) }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return typeMirror.toString()
    }

    companion object {
        fun create(
            env: JavacProcessingEnv,
            typeMirror: TypeMirror,
            kotlinType: KmBaseTypeContainer?,
            elementNullability: XNullability?,
        ) = JavacTypeArgument(env, typeMirror, kotlinType, elementNullability)

        fun create(env: JavacProcessingEnv, type: XType, variance: XVariance): JavacTypeArgument {
            check(type is JavacType)
            return JavacTypeArgument(
                env,
                typeMirror =
                    when (variance) {
                        XVariance.INVARIANT -> type.typeMirror
                        XVariance.IN -> env.typeUtils.getWildcardType(null, type.typeMirror)
                        XVariance.OUT -> env.typeUtils.getWildcardType(type.typeMirror, null)
                        XVariance.STAR -> env.typeUtils.getWildcardType(null, null)
                    },
                type.kotlinType,
                type.nullability,
            )
        }
    }
}
