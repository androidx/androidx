/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Origin

/**
 * JvmType of a Kotlin type depends on where the type is declared.
 *
 * This resolver is used to resolve that type when necessary (computing TypeName).
 * See [KSTypeVarianceResolver] for details.
 */
internal class KspJvmTypeResolver(
    val scope: KspJvmTypeResolutionScope,
    val delegate: KspType
) {
    internal fun resolveJvmType(
        env: KspProcessingEnv
    ): KspType {
        if (!scope.needsWildcardResolution) {
            return delegate
        }
        val wildcardMode = if (scope.suppressesWildcards) {
            KSTypeVarianceResolver.WildcardMode.SUPPRESSED
        } else {
            KSTypeVarianceResolver.WildcardMode.PREFERRED
        }

        // use the jvm type of the declaration so that it also gets its jvm wildcards resolved.
        val declarationJvmType = (scope.findDeclarationType() as? KspType)?.jvmWildcardTypeOrSelf

        return env.resolveWildcards(
            ksType = delegate.ksType,
            wildcardMode = wildcardMode,
            declarationType = declarationJvmType?.ksType
        ).let {
            env.wrap(
                ksType = it,
                allowPrimitives = delegate.typeName.isPrimitive
            )
        }
    }
}

/**
 * Provides KSType resolution scope for parameter types.
 */
internal sealed class KspJvmTypeResolutionScope(
    private val annotated: KSAnnotated,
    private val container: KSDeclaration?
) {
    /**
     * Checks whether we need wildcard resolution at all. It is only necessary if either the method
     * parameter is in kotlin or the containing class, which inherited the method, is in kotlin.
     */
    val needsWildcardResolution: Boolean by lazy {
        annotated.isInKotlinCode() || container?.isInKotlinCode() == true
    }

    /**
     * Checks if the wildcards are suppressed by checking the hierarchy of the declaration to see if
     * it has the @JvmSuppressWildcards annotation.
     */
    val suppressesWildcards by lazy {
        // suppress wildcards depend on the declaration site.
        annotated.hasSuppressWildcardsAnnotationInHierarchy()
    }

    private fun KSAnnotated.isInKotlinCode(): Boolean {
        // find the true origin by skipping synthetics.
        var current: KSNode? = this
        while (current != null) {
            val origin = current.origin
            if (origin != Origin.SYNTHETIC) {
                return origin == Origin.KOTLIN || origin == Origin.KOTLIN_LIB
            }
            current = current.parent
        }
        return false
    }

    /**
     * Finds the XType from the declaration if and only if this method is inherited from another
     * class / interface.
     */
    abstract fun findDeclarationType(): XType?

    internal class MethodParameter(
        private val kspExecutableElement: KspExecutableElement,
        private val parameterIndex: Int,
        annotated: KSAnnotated,
    ) : KspJvmTypeResolutionScope(
        annotated = annotated,
        container = kspExecutableElement.containing.declaration
    ) {
        override fun findDeclarationType(): XType? {
            val declarationMethodType = if (kspExecutableElement is KspMethodElement) {
                kspExecutableElement.declarationMethodType
            } else {
                null
            }
            return declarationMethodType?.parameterTypes?.getOrNull(parameterIndex)
        }
    }

    internal class PropertySetterParameter(
        val declaration: KspSyntheticPropertyMethodElement
    ) : KspJvmTypeResolutionScope(
        annotated = declaration.accessor,
        container = declaration.field.containing.declaration
    ) {
        override fun findDeclarationType(): XType? {
            // We return the declaration from the setter, not the field because the setter parameter
            // will have a different type in jvm (due to jvm wildcard resolution)
            return declaration.field.declarationField
                ?.syntheticSetter?.parameters?.firstOrNull()?.type
        }
    }
}
