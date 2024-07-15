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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XEnumEntry
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XPackageElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XTypeParameterElement
import androidx.room.compiler.processing.collectAllMethods
import androidx.room.compiler.processing.collectFieldsIncludingPrivateSupers
import androidx.room.compiler.processing.filterMethodsByConfig
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticConstructorElement
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.tryBox
import androidx.room.compiler.processing.util.MemoizedSequence
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
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
    override val declaration: KSClassDeclaration
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

    override val qualifiedName: String by lazy {
        (declaration.qualifiedName ?: declaration.simpleName).asString()
    }

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
                .singleOrNull {
                    val declaration = it.resolve().declaration.replaceTypeAliases()
                    declaration is KSClassDeclaration && declaration.classKind == ClassKind.CLASS
                }
                ?.let { env.wrap(it).makeNonNullable() } ?: anyTypeElement.type
        }
    }

    override val superInterfaces by lazy {
        declaration.superTypes
            .filter {
                val declaration = it.resolve().declaration.replaceTypeAliases()
                declaration is KSClassDeclaration && declaration.classKind == ClassKind.INTERFACE
            }
            .mapTo(mutableListOf()) { env.wrap(it).makeNonNullable() }
    }

    @Deprecated(
        "Use asClassName().toJavaPoet() to be clear the name is for JavaPoet.",
        replaceWith =
            ReplaceWith("asClassName().toJavaPoet()", "androidx.room.compiler.codegen.toJavaPoet")
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

    /**
     * This list includes fields for all properties in this class and its static companion
     * properties. They are not necessarily fields as it might include properties of interfaces.
     */
    private val _declaredProperties by lazy {
        buildList {
                addAll(declaration.getDeclarationsInSourceOrder())
                addAll(
                    declaration.findCompanionObject().getDeclarationsInSourceOrder().filter {
                        it.isStatic()
                    }
                )
            }
            .filterIsInstance(KSPropertyDeclaration::class.java)
            .map { KspFieldElement(env = env, declaration = it) }
    }

    private val _constructors by lazy {
        if (isAnnotationClass()) {
            emptyList()
        } else {
            val constructors = declaration.getConstructors().toList()
            buildList {
                addAll(constructors.map { env.wrapFunctionDeclaration(it) as XConstructorElement })
                constructors
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
                                    valueParameters = emptyList()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private val _declaredFields by lazy {
        _declaredProperties.filter { it.declaration.hasBackingField }
    }

    private fun syntheticGetterSetterMethods(field: KspFieldElement): List<XMethodElement> {
        if (declaration.isCompanionObject) {
            return field.syntheticAccessors
        }
        if (field.isStatic() && !field.declaration.hasJvmStaticAnnotation()) {
            return field.syntheticAccessors.filter { it.accessor.hasJvmStaticAnnotation() }
        }
        if (field.isStatic() && field.declaration.hasJvmStaticAnnotation()) {
            // Getter and setter are copied from companion object into current type
            // element by Compiler in KAPT when @JvmStatic is present, in this case, the
            // copied over method element should swap its enclosing element to be
            // current type element instead of companion object.
            return field.syntheticAccessors.map { element ->
                KspSyntheticPropertyMethodElement.create(
                    env,
                    field,
                    element.accessor,
                    isSyntheticStatic = true
                )
            }
        }
        return field.syntheticAccessors
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
                declaration.getDeclarationsInSourceOrder().forEach {
                    if (it is KSFunctionDeclaration && !it.isConstructor()) {
                        add(KspMethodElement.create(env = env, declaration = it))
                    } else if (it is KSPropertyDeclaration) {
                        addAll(
                            syntheticGetterSetterMethods(
                                KspFieldElement(env = env, declaration = it)
                            )
                        )
                    }
                }
                declaration.findCompanionObject().getDeclarationsInSourceOrder().forEach {
                    if (
                        it.hasJvmStaticAnnotation() &&
                            it is KSFunctionDeclaration &&
                            !it.isConstructor()
                    ) {
                        add(
                            KspMethodElement.create(
                                env = env,
                                declaration = it,
                                isSyntheticStatic = true
                            )
                        )
                    } else if (it is KSPropertyDeclaration) {
                        addAll(
                            syntheticGetterSetterMethods(
                                KspFieldElement(env = env, declaration = it)
                            )
                        )
                    }
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

    override fun getEnclosedTypeElements(): List<XTypeElement> {
        return declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filterNot { it.classKind == ClassKind.ENUM_ENTRY }
            .map { env.wrapClassDeclaration(it) }
            .toList()
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
        override val entries: Set<XEnumEntry> by lazy {
            declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .mapTo(mutableSetOf()) { KspEnumEntry(env, it, this) }
        }
    }

    @OptIn(KspExperimental::class)
    fun KSDeclarationContainer?.getDeclarationsInSourceOrder() =
        this?.let { env.resolver.getDeclarationsInSourceOrder(it) } ?: emptySequence()

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
