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

import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin

/**
 * Provides KSType resolution scope for a type.
 */
internal sealed class KSTypeVarianceResolverScope(
    private val annotated: KSAnnotated,
    private val container: KSDeclaration?
) {
    /**
     * Checks whether we need wildcard resolution at all. It is only necessary if either the method
     * parameter is in kotlin or the containing class, which inherited the method, is in kotlin.
     */
    val needsWildcardResolution: Boolean by lazy {
        (annotated.isInKotlinCode() || container?.isInKotlinCode() == true) &&
            !annotated.hasSuppressWildcardsAnnotationInHierarchy()
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

    /** Finds the [KSType] from the declaration. */
    abstract fun declarationType(): KSType

    /** Returns `true` if this scope represents a val property or a return type. */
    abstract fun isValOrReturnType(): Boolean

    internal class MethodParameter(
        private val kspExecutableElement: KspExecutableElement,
        private val parameterIndex: Int,
        annotated: KSAnnotated,
        container: KSDeclaration?,
    ) : KSTypeVarianceResolverScope(annotated, container) {
        override fun declarationType() =
            (kspExecutableElement.parameters[parameterIndex].type as KspType).ksType

        override fun isValOrReturnType() = false
    }

    internal class PropertySetterParameterType(
        private val setterMethod: KspSyntheticPropertyMethodElement.Setter
    ) : KSTypeVarianceResolverScope(
        annotated = setterMethod.accessor,
        container = setterMethod.field.enclosingElement.declaration
    ) {
        override fun declarationType(): KSType {
            // We return the declaration from the setter, not the field because the setter parameter
            // will have a different type in jvm (due to jvm wildcard resolution)
            return (setterMethod.field.syntheticSetter!!.parameters.single().type as KspType).ksType
        }

        override fun isValOrReturnType() = false
    }

    internal class PropertyGetterMethodReturnType(
        private val getterMethod: KspSyntheticPropertyMethodElement.Getter
    ) : KSTypeVarianceResolverScope(
        annotated = getterMethod.accessor,
        container = getterMethod.field.enclosingElement.declaration
    ) {
        override fun declarationType(): KSType {
            // We return the declaration from the getter, not the field because the getter return
            // type will have a different type in jvm (due to jvm wildcard resolution)
            return (getterMethod.field.syntheticAccessors.first().returnType as KspType).ksType
        }

        override fun isValOrReturnType() = true
    }

    internal class PropertyType(val field: KspFieldElement) : KSTypeVarianceResolverScope(
        annotated = field.declaration,
        container = field.enclosingElement.declaration
    ) {
        override fun declarationType() = field.type.ksType

        override fun isValOrReturnType() = field.isFinal()
    }

    internal class MethodReturnType(val method: KspMethodElement) : KSTypeVarianceResolverScope(
        annotated = method.declaration,
        container = method.enclosingElement.declaration
    ) {
        override fun declarationType() = method.returnType.ksType

        override fun isValOrReturnType() = true
    }
}
