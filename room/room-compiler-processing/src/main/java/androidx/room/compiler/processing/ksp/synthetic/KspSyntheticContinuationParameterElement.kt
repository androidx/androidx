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
import androidx.room.compiler.processing.ksp.KSTypeVarianceResolverScope
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.ksp.KspMethodElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import androidx.room.compiler.processing.ksp.requireContinuationClass
import androidx.room.compiler.processing.ksp.returnTypeAsMemberOf
import androidx.room.compiler.processing.ksp.swapResolvedType
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance

/**
 * XProcessing adds an additional argument to each suspend function for the continuation because
 * this is what KAPT generates and Room needs it as long as it generates java code.
 */
internal class KspSyntheticContinuationParameterElement(
    val env: KspProcessingEnv,
    override val enclosingElement: KspMethodElement
) : XExecutableParameterElement,
    XEquality,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = null, // does not matter, this is synthetic and has no annotations.
        filter = NO_USE_SITE
    ) {
    override fun isContinuationParam() = true

    override fun isReceiverParam() = false

    override fun isKotlinPropertyParam() = false

    override val name: String by lazy {
        // KAPT uses `$completion` but it doesn't check for conflicts, we do. Be aware that before
        // Kotlin 1.8.0 the param was named 'continuation'.
        var candidate = PARAM_NAME
        var suffix = 0
        while (
            enclosingElement.declaration.parameters.any { it.name?.asString() == candidate }
        ) {
            candidate = PARAM_NAME + "_" + suffix
            suffix ++
        }
        candidate
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(PARAM_NAME, enclosingElement)
    }

    override val hasDefaultValue: Boolean
        get() = false

    override val type: KspType by lazy {
        asMemberOf(enclosingElement.enclosingElement.type?.ksType)
    }

    override val fallbackLocationText: String
        get() = "return type of ${enclosingElement.fallbackLocationText}"

    // Not applicable
    override val docComment: String? get() = null

    override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    override fun asMemberOf(other: XType): KspType {
        if (enclosingElement.enclosingElement.type?.isSameType(other) != false) {
            return type
        }
        check(other is KspType)
        return asMemberOf(other.ksType)
    }

    private fun asMemberOf(ksType: KSType?): KspType {
        val continuation = env.resolver.requireContinuationClass()
        val asMember = enclosingElement.declaration.returnTypeAsMemberOf(
            ksType = ksType
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
        ).copyWithScope(
            KSTypeVarianceResolverScope.MethodParameter(
                kspExecutableElement = enclosingElement,
                parameterIndex = enclosingElement.parameters.size - 1,
                annotated = enclosingElement.declaration,
                container = ksType?.declaration
            )
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

    companion object {
        const val PARAM_NAME = "\$completion"
    }
}
