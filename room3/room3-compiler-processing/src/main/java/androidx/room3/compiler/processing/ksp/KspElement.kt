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

package androidx.room3.compiler.processing.ksp

import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XEquality
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.visitor.KSValidateVisitor
import java.util.Locale

internal abstract class KspElement(
    internal val env: KspProcessingEnv,
    open val declaration: KSAnnotated,
) : XElement, XEquality {
    override fun kindName(): String {
        return when (declaration) {
            is KSClassDeclaration ->
                (declaration as KSClassDeclaration).classKind.name.lowercase(Locale.US)
            is KSPropertyDeclaration -> "property"
            is KSFunctionDeclaration -> "function"
            else -> declaration::class.simpleName ?: "unknown"
        }
    }

    final override val equalityItems: Array<out Any?> by lazy { arrayOf(declaration) }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return declaration.toString()
    }

    override val docComment: String? by lazy { (declaration as? KSDeclaration)?.docString }

    override fun validate(): Boolean {
        return declaration.validateWithAnnotations()
    }
}

internal fun KSNode.validateWithAnnotations(): Boolean = accept(KSP_VALIDATE_VISITOR, null)

// Default KSValidateVisitor only checks resolvable types and misses non-constant annotation
// arguments as well as annotations on property accessors and value parameters.
private val KSP_VALIDATE_VISITOR =
    object : KSValidateVisitor({ _, _ -> true }) {
        // KSValidateVisitor already checks property and @field: annotations via visitDeclaration,
        // but skips getter and setter accessors (@get:, @set:, @setparam:). Once XProcessing opts
        // into KSP's KSBackingField feature, property.backingField will also need to be visited
        // here.
        override fun visitPropertyDeclaration(
            property: KSPropertyDeclaration,
            data: KSNode?,
        ): Boolean {
            return super.visitPropertyDeclaration(property, data) &&
                property.getter?.let { visitAnnotated(it, property) } != false &&
                property.setter?.let {
                    visitAnnotated(it, property) && visitAnnotated(it.parameter, it)
                } != false
        }

        // KSValidateVisitor only checks parameter types, so parameter annotations would be skipped.
        override fun visitValueParameter(
            valueParameter: KSValueParameter,
            data: KSNode?,
        ): Boolean {
            return super.visitValueParameter(valueParameter, data) &&
                visitAnnotated(valueParameter, data)
        }

        // KSValidateVisitor ignores null argument values, which KSP produces for non-constant
        // arguments. Note that for array arguments, KSP preserves null items for Java sources,
        // whereas Kotlin's Analysis API silently drops non-constant array items upstream.
        override fun visitValueArgument(
            valueArgument: KSValueArgument,
            data: KSNode?,
        ): Boolean {
            return (valueArgument.origin == Origin.SYNTHETIC ||
                hasNoNullValues(valueArgument.value)) &&
                super.visitValueArgument(valueArgument, data)
        }

        private fun hasNoNullValues(value: Any?): Boolean =
            value != null && (value !is List<*> || value.all { hasNoNullValues(it) })
    }
