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

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XEnumEntry
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.tryBox
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.javapoet.ClassName

internal sealed class KspTypeElement(
    env: KspProcessingEnv,
    override val declaration: KSClassDeclaration
) : KspElement(env, declaration),
    XTypeElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE),
    KspMemberContainer {

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val packageName: String by lazy {
        declaration.getNormalizedPackageName()
    }

    override val enclosingTypeElement: XTypeElement? by lazy {
        // if it is a file, don't return it
        declaration.findEnclosingMemberContainer(env) as? XTypeElement
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(declaration)
    }

    override val qualifiedName: String by lazy {
        (declaration.qualifiedName ?: declaration.simpleName).asString()
    }

    override val type: KspType by lazy {
        env.wrap(
            ksType = declaration.asType(emptyList()),
            allowPrimitives = false
        )
    }

    override val superType: XType? by lazy {
        declaration.superTypes.firstOrNull {
            val type = it.resolve().declaration as? KSClassDeclaration ?: return@firstOrNull false
            type.classKind == ClassKind.CLASS
        }?.let {
            env.wrap(
                ksType = it.resolve(),
                allowPrimitives = false
            )
        }
    }

    override val className: ClassName by lazy {
        declaration.typeName(env.resolver).tryBox().also { typeName ->
            check(typeName is ClassName) {
                "Internal error. The type name for $declaration should be a class name but " +
                    "received ${typeName::class}"
            }
        } as ClassName
    }

    /**
     * This list includes fields for all properties in this class and its static companion
     * properties. They are not necessarily fields as it might include properties of interfaces.
     */
    private val _declaredProperties by lazy {
        val declaredProperties = declaration.getDeclaredProperties()
            .map {
                KspFieldElement(
                    env = env,
                    declaration = it,
                    containing = this
                )
            }.let {
                // only order instance properties with backing fields, we don't care about the order
                // of companion properties or properties without backing fields.
                val (withBackingField, withoutBackingField) = it.partition {
                    it.declaration.hasBackingFieldFixed
                }
                KspClassFileUtility.orderFields(declaration, withBackingField) + withoutBackingField
            }

        val companionProperties = declaration
            .findCompanionObject()
            ?.getDeclaredProperties()
            ?.filter {
                it.isStatic()
            }.orEmpty()
            .map {
                KspFieldElement(
                    env = env,
                    declaration = it,
                    containing = this
                )
            }
        declaredProperties + companionProperties
    }

    private val _declaredFields by lazy {
        _declaredProperties.filter {
            it.declaration.hasBackingFieldFixed
        }
    }

    private val syntheticGetterSetterMethods: List<XMethodElement> by lazy {
        _declaredProperties.flatMap { field ->
            when {
                field.type.ksType.isInline() -> {
                    // KAPT does not generate getters/setters for inlines, we'll hide them as well
                    // until room generates kotlin code
                    emptyList()
                }
                field.declaration.hasJvmFieldAnnotation() -> {
                    // jvm fields cannot have accessors but KSP generates synthetic accessors for
                    // them. We check for JVM field first before checking the getter
                    emptyList()
                }
                field.declaration.isPrivate() -> emptyList()

                else ->
                    sequenceOf(field.declaration.getter, field.declaration.setter)
                        .filterNotNull()
                        .filterNot {
                            // KAPT does not generate methods for privates, KSP does so we filter
                            // them out.
                            it.modifiers.contains(Modifier.PRIVATE)
                        }
                        .filter {
                            if (field.isStatic()) {
                                // static fields are the properties that are coming from the
                                // companion. Whether we'll generate method for it or not depends on
                                // the JVMStatic annotation
                                it.hasJvmStaticAnnotation() ||
                                    field.declaration.hasJvmStaticAnnotation()
                            } else {
                                true
                            }
                        }
                        .map { accessor ->
                            KspSyntheticPropertyMethodElement.create(
                                env = env,
                                field = field,
                                accessor = accessor
                            )
                        }.toList()
            }
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

    override fun isValueClass(): Boolean {
        return Modifier.INLINE in declaration.modifiers
    }

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

    override fun getDeclaredFields(): List<XFieldElement> {
        return _declaredFields
    }

    override fun findPrimaryConstructor(): XConstructorElement? {
        return declaration.primaryConstructor?.let {
            KspConstructorElement(
                env = env,
                containing = this,
                declaration = it
            )
        }
    }

    private val _declaredMethods by lazy {
        val instanceMethods = declaration.getDeclaredFunctions().asSequence()
            .filterNot { it.isConstructor() }
        val companionMethods = declaration.findCompanionObject()
            ?.getDeclaredFunctions()
            ?.filterNot {
                it.isConstructor()
            }
            ?.filter {
                it.hasJvmStaticAnnotation()
            } ?: emptySequence()
        val declaredMethods = (instanceMethods + companionMethods)
            .filterNot {
                // filter out constructors
                it.simpleName.asString() == name
            }.filterNot {
                // if it receives or returns inline, drop it.
                // we can re-enable these once room generates kotlin code
                it.parameters.any {
                    it.type.resolve().isInline()
                } || it.returnType?.resolve()?.isInline() == true
            }.map {
                KspMethodElement.create(
                    env = env,
                    containing = this,
                    declaration = it
                )
            }.toList()
        KspClassFileUtility.orderMethods(declaration, declaredMethods) +
            syntheticGetterSetterMethods
    }

    override fun getDeclaredMethods(): List<XMethodElement> {
        return _declaredMethods
    }

    override fun getConstructors(): List<XConstructorElement> {
        return declaration.getConstructors().map {
            KspConstructorElement(
                env = env,
                containing = this,
                declaration = it
            )
        }.toList()
    }

    override fun getSuperInterfaceElements(): List<XTypeElement> {
        return declaration.superTypes.asSequence().mapNotNull {
            it.resolve().declaration
        }.filterIsInstance<KSClassDeclaration>()
            .filter {
                it.classKind == ClassKind.INTERFACE
            }.mapTo(mutableListOf()) {
                env.wrapClassDeclaration(it)
            }
    }

    override fun getEnclosedTypeElements(): List<XTypeElement> {
        return declaration.declarations.filterIsInstance<KSClassDeclaration>()
            .map { env.wrapClassDeclaration(it) }
            .toList()
    }

    override fun toString(): String {
        return declaration.toString()
    }

    private class DefaultKspTypeElement(
        env: KspProcessingEnv,
        declaration: KSClassDeclaration
    ) : KspTypeElement(env, declaration)

    private class KspEnumTypeElement(
        env: KspProcessingEnv,
        declaration: KSClassDeclaration
    ) : KspTypeElement(env, declaration), XEnumTypeElement {
        override val entries: Set<XEnumEntry> by lazy {
            declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .mapTo(mutableSetOf()) {
                    KspEnumEntry(env, it, this)
                }
        }
    }

    /**
     * Workaround for https://github.com/google/ksp/issues/529 where KSP returns false for
     * backing field when the property has a lateinit modifier.
     */
    private val KSPropertyDeclaration.hasBackingFieldFixed
        get() = hasBackingField || modifiers.contains(Modifier.LATEINIT)

    companion object {
        fun create(
            env: KspProcessingEnv,
            ksClassDeclaration: KSClassDeclaration
        ): KspTypeElement {
            return when (ksClassDeclaration.classKind) {
                ClassKind.ENUM_CLASS -> KspEnumTypeElement(env, ksClassDeclaration)
                else -> DefaultKspTypeElement(env, ksClassDeclaration)
            }
        }
    }
}
