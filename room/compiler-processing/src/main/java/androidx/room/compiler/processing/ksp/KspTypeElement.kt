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
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticConstructorForJava
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import androidx.room.compiler.processing.tryBox
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.squareup.javapoet.ClassName

internal class KspTypeElement(
    env: KspProcessingEnv,
    override val declaration: KSClassDeclaration
) : KspElement(env, declaration),
    XTypeElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE) {

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val packageName: String by lazy {
        declaration.getNormalizedPackageName()
    }

    override val enclosingTypeElement: XTypeElement? by lazy {
        declaration.findEnclosingTypeElement(env)
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(declaration)
    }

    override val qualifiedName: String by lazy {
        val pkgName = declaration.getNormalizedPackageName()
        if (pkgName.isBlank()) {
            declaration.simpleName.asString()
        } else {
            "$pkgName.${declaration.simpleName.asString()}"
        }
    }

    override val type: KspDeclaredType by lazy {
        val result = env.wrap(
            ksType = declaration.asStarProjectedType(),
            allowPrimitives = false
        )
        check(result is KspDeclaredType) {
            "Internal error, expected type of $this to resolve to a declared type but it resolved" +
                " to $result (${result::class})"
        }
        result
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

    private val _declaredPropertyFields by lazy {
        val declaredProperties = declaration.getDeclaredProperties()
        val companionProperties = declaration
            .findCompanionObject()
            ?.getDeclaredProperties()
            ?.filter {
                it.isStatic()
            }.orEmpty()
        (declaredProperties + companionProperties)
            .map {
                KspFieldElement(
                    env = env,
                    declaration = it,
                    containing = this
                )
            }
    }

    private val _declaredFieldsIncludingSupers by lazy {
        // Read all properties from all supers and select the ones that are not overridden.
        // TODO: remove once it is implemented in KSP
        // https://github.com/android/kotlin/issues/133
        val selectedNames = mutableSetOf<String>()
        _declaredPropertyFields.forEach {
            selectedNames.add(it.name)
        }
        val selection = mutableListOf<KSPropertyDeclaration>()
        declaration.getAllSuperTypes().map {
            it.declaration
        }.filterIsInstance(KSClassDeclaration::class.java)
            .flatMap {
                it.getDeclaredProperties().asSequence()
            }.forEach {
                if (!selectedNames.contains(it.simpleName.asString())) {
                    selection.add(it)
                }
            }
        _declaredPropertyFields + selection.map {
            KspFieldElement(
                env = env,
                declaration = it,
                containing = this
            )
        }
    }

    private val syntheticGetterSetterMethods: List<XMethodElement> by lazy {
        val setters = _declaredPropertyFields.mapNotNull {
            if (it.type.ksType.isInline()) {
                // KAPT does not generate getters/setters for inlines, we'll hide them as well
                // until room generates kotlin code
                return@mapNotNull null
            }
            val setter = it.declaration.setter
            val needsSetter = if (setter != null) {
                // kapt does not generate synthetics for private fields/setters so we won't either
                !setter.modifiers.contains(Modifier.PRIVATE)
            } else {
                isInterface() && it.declaration.isMutable
            }
            if (needsSetter) {
                KspSyntheticPropertyMethodElement.Setter(
                    env = env,
                    field = it
                )
            } else {
                null
            }
        }
        val getters = _declaredPropertyFields.mapNotNull {
            if (it.type.ksType.isInline()) {
                // KAPT does not generate getters/setters for inlines, we'll hide them as well
                // until room generates kotlin code
                return@mapNotNull null
            }
            val getter = it.declaration.getter
            val needsGetter = if (getter != null) {
                // kapt does not generate synthetics for private fields/getters so we won't either]
                !getter.modifiers.contains(Modifier.PRIVATE)
            } else {
                isInterface()
            }
            if (needsGetter) {
                KspSyntheticPropertyMethodElement.Getter(
                    env = env,
                    field = it
                )
            } else {
                null
            }
        }
        setters + getters
    }

    override fun isInterface(): Boolean {
        return declaration.classKind == ClassKind.INTERFACE
    }

    override fun isKotlinObject(): Boolean {
        return declaration.classKind == ClassKind.OBJECT
    }

    override fun isFinal(): Boolean {
        // workaround for https://github.com/android/kotlin/issues/128
        return !isInterface() && !declaration.isOpen()
    }

    override fun getAllFieldsIncludingPrivateSupers(): List<XFieldElement> {
        return _declaredFieldsIncludingSupers
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
        val companionMethods = declaration.findCompanionObject()
            ?.getDeclaredFunctions()
            ?.asSequence()
            ?.filter {
                it.isStatic()
            }
            ?: emptySequence()
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
        declaredMethods + syntheticGetterSetterMethods
    }

    override fun getDeclaredMethods(): List<XMethodElement> {
        return _declaredMethods
    }

    override fun getConstructors(): List<XConstructorElement> {
        val constructors = declaration.declaredConstructors().toMutableList()
        val primary = if (constructors.isEmpty() && !isInterface()) {
            declaration.primaryConstructor
        } else {
            declaration.getNonSyntheticPrimaryConstructor()
        }
        primary?.let(constructors::add)

        val elements: List<XConstructorElement> = constructors.map {
            KspConstructorElement(
                env = env,
                containing = this,
                declaration = it
            )
        }
        return if (elements.isEmpty() &&
            declaration.origin == Origin.JAVA &&
            !isInterface()
        ) {
            // workaround for https://github.com/google/ksp/issues/98
            // TODO remove if KSP support this
            listOf(
                KspSyntheticConstructorForJava(
                    env = env,
                    origin = this
                )
            )
        } else {
            elements
        }
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

    private fun KSClassDeclaration.getNonSyntheticPrimaryConstructor(): KSFunctionDeclaration? {
        // workaround for https://github.com/android/kotlin/issues/136
        // TODO remove once that bug is fixed
        return if (primaryConstructor?.simpleName?.asString() != "<init>") {
            primaryConstructor
        } else {
            null
        }
    }

    private fun KSClassDeclaration.declaredConstructors() = this.getDeclaredFunctions()
        .filter {
            it.simpleName == this.simpleName
        }
}
