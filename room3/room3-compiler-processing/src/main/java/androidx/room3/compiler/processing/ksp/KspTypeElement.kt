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

import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.processing.XAnnotated
import androidx.room3.compiler.processing.XConstructorElement
import androidx.room3.compiler.processing.XEnumTypeElement
import androidx.room3.compiler.processing.XFieldElement
import androidx.room3.compiler.processing.XHasModifiers
import androidx.room3.compiler.processing.XMemberContainer
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XNullability
import androidx.room3.compiler.processing.XPackageElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.XTypeParameterElement
import androidx.room3.compiler.processing.collectAllMethods
import androidx.room3.compiler.processing.collectFieldsIncludingPrivateSupers
import androidx.room3.compiler.processing.filterMethodsByConfig
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE
import androidx.room3.compiler.processing.ksp.synthetic.KspSyntheticConstructorElement
import androidx.room3.compiler.processing.tryBox
import androidx.room3.compiler.processing.util.MemoizedSequence
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin.JAVA
import com.google.devtools.ksp.symbol.Origin.JAVA_LIB
import com.google.devtools.ksp.symbol.Origin.KOTLIN
import com.google.devtools.ksp.symbol.Origin.KOTLIN_LIB
import com.squareup.javapoet.ClassName
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.KClassName

internal sealed class KspTypeElement(
    env: KspProcessingEnv,
    override val declaration: KSClassDeclaration,
) :
    KspElement(env, declaration),
    XTypeElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE),
    KspMemberContainer {

    override val name: String by lazy { declaration.simpleName.asString() }

    override val packageName: String by lazy { packageElement.qualifiedName }

    override val packageElement: XPackageElement by lazy {
        KspPackageElement(env, declaration.packageName.asString())
    }

    override val enclosingTypeElement: XTypeElement? by lazy {
        // if it is a file, don't return it
        declaration.findEnclosingMemberContainer(env) as? XTypeElement
    }

    override val enclosingElement: XMemberContainer?
        get() = enclosingTypeElement

    override val typeParameters: List<XTypeParameterElement> by lazy {
        declaration.typeParameters.map { KspTypeParameterElement(env, it) }
    }

    override val qualifiedName: String by lazy { asClassName().kotlin.canonicalName }

    override val type: KspType by lazy {
        env.wrap(ksType = declaration.asType(emptyList()), allowPrimitives = false)
    }

    override val superClass: XType? by lazy {
        val anyTypeElement = env.requireTypeElement(Any::class)
        if (isInterface()) {
            // interfaces don't have super classes (they do have super types)
            null
        } else if (this == anyTypeElement) {
            null
        } else {
            declaration.superTypes
                .firstOrNull {
                    val type = it.resolve()
                    val declaration = type.declaration.replaceTypeAliases()
                    declaration is KSClassDeclaration &&
                        (declaration.classKind == ClassKind.CLASS &&
                            // Filter out error class declarations, for consistency with KAPT these
                            // are exposed as super interfaces.
                            (isFromJava() || !type.isError))
                }
                ?.let { env.wrap(it).makeNonNullable() } ?: anyTypeElement.type
        }
    }

    override val superInterfaces by lazy {
        declaration.superTypes
            .filter {
                val type = it.resolve()
                val declaration = type.declaration.replaceTypeAliases()
                declaration is KSClassDeclaration &&
                    (declaration.classKind == ClassKind.INTERFACE ||
                        // Workaround https://github.com/google/ksp/issues/1443 by exposing
                        // error class declarations as super interfaces.
                        (isFromKotlin() && type.isError))
            }
            .mapTo(mutableListOf()) { env.wrap(it).makeNonNullable() }
    }

    @Deprecated(
        "Use asClassName().toJavaPoet() to be clear the name is for JavaPoet.",
        replaceWith =
            ReplaceWith("asClassName().toJavaPoet()", "androidx.room3.compiler.codegen.toJavaPoet"),
    )
    override val className: ClassName by lazy { xClassName.java }

    private val xClassName: XClassName by lazy {
        val java =
            declaration.asJTypeName(env.resolver).tryBox().also { typeName ->
                check(typeName is JClassName) {
                    "Internal error. The type name for $declaration should be a class name but " +
                        "received ${typeName::class}"
                }
            } as JClassName
        val kotlin = declaration.asKTypeName(env.resolver) as KClassName
        XClassName(java, kotlin, XNullability.NONNULL)
    }

    override fun asClassName() = xClassName

    private val allMethods = MemoizedSequence { collectAllMethods(this) }

    private val allFieldsIncludingPrivateSupers = MemoizedSequence {
        collectFieldsIncludingPrivateSupers(this)
    }

    override fun getAllMethods(): Sequence<XMethodElement> = allMethods

    override fun getAllFieldsIncludingPrivateSupers() = allFieldsIncludingPrivateSupers

    @OptIn(KspExperimental::class)
    protected val _enclosedElements: List<KspElement> by lazy {
        env.resolver
            .getDeclarationsInSourceOrder(declaration)
            .map { env.wrapDeclaration(it) }
            .toList()
    }

    private val _constructors by lazy {
        if (isAnnotationClass()) {
            emptyList()
        } else {
            val constructors = _enclosedElements.filterIsInstance<KspConstructorElement>()
            buildList {
                addAll(constructors)
                constructors
                    .map { it.declaration }
                    .filter { it.hasOverloads() }
                    .forEach { addAll(enumerateSyntheticConstructors(it)) }

                // To match KAPT if all params in the primary constructor have default values then
                // synthesize a no-arg constructor if one is not already present.
                val hasNoArgConstructor = constructors.any { it.parameters.isEmpty() }
                if (!hasNoArgConstructor) {
                    declaration.primaryConstructor?.let {
                        if (!it.hasOverloads() && it.parameters.all { it.hasDefault }) {
                            add(
                                KspSyntheticConstructorElement(
                                    env = env,
                                    declaration = it,
                                    valueParameters = emptyList(),
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private val _declaredFields: List<XFieldElement> by lazy {
        if (isCompanionObject()) {
            emptyList()
        } else {
            (_enclosedElements + (companionObject?._enclosedElements ?: emptyList()))
                .filterIsInstance<KspFieldElement>()
                .filter { it.declaration.hasBackingField && it.name != "_hashCode" }
        }
    }

    override fun isNested(): Boolean {
        return declaration.findEnclosingMemberContainer(env) is XTypeElement
    }

    override fun isInterface(): Boolean {
        return declaration.classKind == ClassKind.INTERFACE
    }

    override fun isKotlinObject(): Boolean {
        return declaration.classKind == ClassKind.OBJECT
    }

    override fun isCompanionObject(): Boolean {
        return declaration.isCompanionObject
    }

    override fun isAnnotationClass(): Boolean {
        return declaration.classKind == ClassKind.ANNOTATION_CLASS
    }

    override fun isClass(): Boolean {
        return declaration.classKind == ClassKind.CLASS
    }

    override fun isDataClass(): Boolean {
        return Modifier.DATA in declaration.modifiers
    }

    override fun isValueClass(): Boolean = declaration.isValueClass()

    override fun isFunctionalInterface(): Boolean {
        return Modifier.FUN in declaration.modifiers
    }

    override fun isExpect(): Boolean {
        return Modifier.EXPECT in declaration.modifiers
    }

    override fun isFinal(): Boolean {
        // workaround for https://github.com/android/kotlin/issues/128
        return !isInterface() && !declaration.isOpen()
    }

    override fun isRecordClass(): Boolean {
        val recordType = env.findType("java.lang.Record") ?: return false
        // Need to also check super type since @JvmRecord is @Retention(SOURCE)
        return hasAnnotation(JvmRecord::class) ||
            superClass?.let { recordType.isAssignableFrom(it) } == true
    }

    override fun getDeclaredFields(): List<XFieldElement> {
        return _declaredFields
    }

    override fun findPrimaryConstructor(): XConstructorElement? {
        if (isAnnotationClass()) {
            return null
        }
        return declaration.primaryConstructor?.let {
            KspConstructorElement(env = env, declaration = it)
        }
    }

    private val _declaredMethods by lazy {
        buildList {
                _enclosedElements.forEach { element ->
                    when (element) {
                        is KspMethodElement -> add(element)
                        is KspFieldElement -> addAll(element.syntheticAccessors)
                    }
                }
                companionObject?._enclosedElements?.forEach { element ->
                    when (element) {
                        is KspMethodElement -> element.syntheticStaticMethod?.let { add(it) }
                        is KspFieldElement -> addAll(element.syntheticStaticAccessors)
                    }
                }
            }
            .let { declaredMethods ->
                if (isAnnotationClass() && isFromKotlin()) {
                    // TODO: KSP gives incorrect ordering for Kotlin annotation properties when
                    //  using Resolver#getDeclarationsInSourceOrder() so reorder based on
                    //  the constructor's parameters, which should have the correct order.
                    //  See https://github.com/google/ksp/issues/2617.
                    val orderByName =
                        declaration.primaryConstructor!!
                            .parameters
                            .mapIndexed { index, it -> it.name!!.asString() to index }
                            .toMap()
                    declaredMethods.sortedBy {
                        val key = it.propertyName
                        orderByName[key] ?: error("$key is not in $orderByName")
                    }
                } else {
                    declaredMethods
                }
            }
            .filterMethodsByConfig(env)
    }

    override fun getDeclaredMethods(): List<XMethodElement> {
        return _declaredMethods
    }

    override fun getConstructors(): List<XConstructorElement> {
        return _constructors
    }

    private fun enumerateSyntheticConstructors(
        declaration: KSFunctionDeclaration
    ): List<KspSyntheticConstructorElement> {
        val parameters = declaration.parameters
        val defaultParamsCount = parameters.count { it.hasDefault }
        if (defaultParamsCount < 1) {
            return emptyList()
        }
        val constructorEnumeration = mutableListOf<KspSyntheticConstructorElement>()
        for (defaultParameterToUseCount in 0..defaultParamsCount - 1) {
            val parameterEnumeration = mutableListOf<KSValueParameter>()
            var defaultParameterUsedCount = 0
            for (parameter in parameters) {
                if (parameter.hasDefault) {
                    if (defaultParameterUsedCount++ >= defaultParameterToUseCount) {
                        continue
                    }
                }
                parameterEnumeration.add(parameter)
            }
            constructorEnumeration.add(
                KspSyntheticConstructorElement(env, declaration, parameterEnumeration)
            )
        }
        val isPreCompiled = declaration.origin == KOTLIN_LIB || declaration.origin == JAVA_LIB
        return if (isPreCompiled) constructorEnumeration.reversed() else constructorEnumeration
    }

    override fun getSuperInterfaceElements() = superInterfaces.mapNotNull { it.typeElement }

    override val companionObject: KspTypeElement? by lazy {
        getEnclosedTypeElements().filterIsInstance<KspTypeElement>().firstOrNull {
            it.isCompanionObject()
        }
    }

    override fun getEnclosedTypeElements(): List<XTypeElement> {
        return _enclosedElements.filterIsInstance<KspTypeElement>()
    }

    override fun isFromJava(): Boolean {
        return when (declaration.origin) {
            JAVA,
            JAVA_LIB -> true
            else -> false
        }
    }

    override fun isFromKotlin(): Boolean {
        return when (declaration.origin) {
            KOTLIN,
            KOTLIN_LIB -> true
            else -> false
        }
    }

    private class DefaultKspTypeElement(env: KspProcessingEnv, declaration: KSClassDeclaration) :
        KspTypeElement(env, declaration)

    private class KspEnumTypeElement(env: KspProcessingEnv, declaration: KSClassDeclaration) :
        KspTypeElement(env, declaration), XEnumTypeElement {
        override val entries: Set<KspEnumEntry> by lazy {
            _enclosedElements.filterIsInstance<KspEnumEntry>().toSet()
        }
    }

    companion object {
        fun create(env: KspProcessingEnv, ksClassDeclaration: KSClassDeclaration): KspTypeElement {
            return when (ksClassDeclaration.classKind) {
                ClassKind.ENUM_CLASS -> KspEnumTypeElement(env, ksClassDeclaration)
                ClassKind.ENUM_ENTRY -> error("Expected declaration to not be an enum entry.")
                else -> DefaultKspTypeElement(env, ksClassDeclaration)
            }
        }
    }
}
