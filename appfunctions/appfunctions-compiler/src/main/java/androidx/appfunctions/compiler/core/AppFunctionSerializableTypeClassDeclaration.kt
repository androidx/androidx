/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

/** Wrapper for [KSClassDeclaration] to represent an [AppFunctionSerializableType] */
class AppFunctionSerializableTypeClassDeclaration(
    private val classDeclaration: KSClassDeclaration
) {
    /**
     * Returns the JVM qualified name which takes into account multi-layer class declarations.
     *
     * For example, `com.example.InnerClass$OuterClass`.
     */
    val jvmQualifiedName: String by lazy { classDeclaration.getJvmQualifiedName() }
    /**
     * Returns the JVM class name which takes into account multi-layer class declarations. For
     * example, `InnerClass$OuterClass`.
     */
    val jvmClassName: String by lazy { classDeclaration.getJvmClassName() }

    /** The super types of this class. */
    val superTypes: Sequence<KSTypeReference> by lazy { classDeclaration.superTypes }

    /** All the [KSDeclaration]s within this class container */
    val declarations: Sequence<KSDeclaration> by lazy { classDeclaration.declarations }

    /** The modifiers of the class. */
    val modifiers: Set<Modifier> by lazy { classDeclaration.modifiers }

    /** The primary constructor of the class if available. */
    val primaryConstructor: KSFunctionDeclaration? by lazy { classDeclaration.primaryConstructor }

    /** The [KSNode] to which the processing error is attributed. */
    val attributeNode: KSNode by lazy { classDeclaration }

    /** The list of [KSTypeParameter] of the class */
    val typeParameters: List<KSTypeParameter> by lazy { classDeclaration.typeParameters }

    /** The original [ClassName] of the class. */
    val originalClassName: ClassName by lazy { classDeclaration.toClassName() }

    /**
     * The [TypeName] of the class.
     *
     * This will be parameterized if [typeParameters] is not empty.
     */
    val typeName: TypeName by lazy {
        if (typeParameters.isEmpty()) {
            originalClassName
        } else {
            originalClassName.parameterizedBy(
                typeParameters.map(KSTypeParameter::toTypeVariableName)
            )
        }
    }

    /** The docString of the class. */
    val docString: String by lazy { classDeclaration.docString.orEmpty() }
}
