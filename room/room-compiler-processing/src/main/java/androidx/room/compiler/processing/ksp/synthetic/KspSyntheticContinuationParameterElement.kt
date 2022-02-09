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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.ksp.KspJvmTypeResolutionScope
import androidx.room.compiler.processing.ksp.KspMethodElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import androidx.room.compiler.processing.ksp.requireContinuationClass
import androidx.room.compiler.processing.ksp.returnTypeAsMemberOf
import androidx.room.compiler.processing.ksp.swapResolvedType
import com.google.devtools.ksp.symbol.Variance

/**
 * XProcessing adds an additional argument to each suspend function for the continuation because
 * this is what KAPT generates and Room needs it as long as it generates java code.
 */
internal class KspSyntheticContinuationParameterElement(
    private val env: KspProcessingEnv,
    override val enclosingElement: KspMethodElement
) : XExecutableParameterElement,
    XEquality,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = null, // does not matter, this is synthetic and has no annotations.
        filter = NO_USE_SITE
    ) {

    override val name: String by lazy {
        // KAPT uses `continuation` but it doesn't check for conflicts, we do.
        var candidate = "continuation"
        var suffix = 0
        while (
            enclosingElement.declaration.parameters.any { it.name?.asString() == candidate }
        ) {
            candidate = "continuation_$suffix"
            suffix ++
        }
        candidate
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf("continuation", enclosingElement)
    }

    override val hasDefaultValue: Boolean
        get() = false

    private val jvmTypeResolutionScope by lazy {
        KspJvmTypeResolutionScope.MethodParameter(
            kspExecutableElement = enclosingElement,
            parameterIndex = enclosingElement.parameters.size - 1,
            annotated = enclosingElement.declaration
        )
    }

    override val type: XType by lazy {
        val continuation = env.resolver.requireContinuationClass()
        val asMember = enclosingElement.declaration.returnTypeAsMemberOf(
            ksType = enclosingElement.containing.type?.ksType
        )
        val returnTypeRef = checkNotNull(enclosingElement.declaration.returnType) {
            "cannot find return type reference for $this"
        }
        val returnTypeAsTypeArgument = env.resolver.getTypeArgument(
            returnTypeRef.swapResolvedType(asMember),
            // even though this will be CONTRAVARIANT when resolved to the JVM type, in Kotlin, it
            // is still INVARIANT. (see [KSTypeVarianceResolver]
            Variance.INVARIANT
        )
        val contType = continuation.asType(
            listOf(
                returnTypeAsTypeArgument
            )
        )
        env.wrap(
            ksType = contType,
            allowPrimitives = false
        ).withJvmTypeResolver(jvmTypeResolutionScope)
    }

    override val fallbackLocationText: String
        get() = "return type of ${enclosingElement.fallbackLocationText}"

    // Not applicable
    override val docComment: String? get() = null

    override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    override fun asMemberOf(other: XType): KspType {
        check(other is KspType)
        val continuation = env.resolver.requireContinuationClass()
        val asMember = enclosingElement.declaration.returnTypeAsMemberOf(
            ksType = other.ksType
        )
        val returnTypeRef = checkNotNull(enclosingElement.declaration.returnType) {
            "cannot find return type reference for $this"
        }
        val returnTypeAsTypeArgument = env.resolver.getTypeArgument(
            returnTypeRef.swapResolvedType(asMember),
            // even though this will be CONTRAVARIANT when resolved to the JVM type, in Kotlin, it
            // is still INVARIANT. (see [KSTypeVarianceResolver]
            Variance.INVARIANT
        )
        val contType = continuation.asType(listOf(returnTypeAsTypeArgument))
        return env.wrap(
            ksType = contType,
            allowPrimitives = false
        ).withJvmTypeResolver(
            jvmTypeResolutionScope
        )
    }

    override fun kindName(): String {
        return "synthetic continuation parameter"
    }

    override fun validate(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }
}
