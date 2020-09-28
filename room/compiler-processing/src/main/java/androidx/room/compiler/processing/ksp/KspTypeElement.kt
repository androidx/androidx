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

import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import com.squareup.javapoet.ClassName
import org.jetbrains.kotlin.ksp.getAllSuperTypes
import org.jetbrains.kotlin.ksp.getDeclaredFunctions
import org.jetbrains.kotlin.ksp.getDeclaredProperties
import org.jetbrains.kotlin.ksp.isOpen
import org.jetbrains.kotlin.ksp.symbol.ClassKind
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration

internal class KspTypeElement(
    env: KspProcessingEnv,
    override val declaration: KSClassDeclaration
) : KspElement(env, declaration), XTypeElement, XHasModifiers by KspHasModifiers(declaration) {

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

    override val type: KspType by lazy {
        env.wrap(declaration.asStarProjectedType())
    }

    override val superType: XType? by lazy {
        declaration.superTypes.firstOrNull {
            val type = it.resolve()?.declaration as? KSClassDeclaration ?: return@firstOrNull false
            type.classKind == ClassKind.CLASS
        }?.let {
            env.wrap(it)
        }
    }

    override val className: ClassName by lazy {
        declaration.typeName()
    }

    private val _declaredFieldsIncludingSupers by lazy {
        // Read all properties from all supers and select the ones that are not overridden.
        // TODO: remove once it is implemented in KSP
        // https://github.com/android/kotlin/issues/133
        val selection = declaration
            .getDeclaredProperties()
            .associateByTo(mutableMapOf()) {
                it.simpleName
            }
        declaration.getAllSuperTypes().map {
            it.declaration
        }.filterIsInstance(KSClassDeclaration::class.java)
            .flatMap {
                it.getDeclaredProperties()
            }.forEach {
                if (!selection.containsKey(it.simpleName)) {
                    selection[it.simpleName] = it
                }
            }
        selection.values.map {
            KspFieldElement(
                env = env,
                declaration = it,
                containing = this
            )
        }
    }

    private val syntheticGetterSetterMethods: List<XMethodElement> by lazy {
        val setters = declaration.getDeclaredProperties()
            .mapNotNull {
                it.setter?.let { setter ->
                    KspSyntheticSetterMethodElement(
                        env = env,
                        containing = this,
                        setter = setter
                    )
                }
            }
        val getters = declaration.getDeclaredProperties()
            .mapNotNull {
                it.getter?.let { getter ->
                    KspSyntheticGetterMethodElement(
                        env = env,
                        containing = this,
                        getter = getter
                    )
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
        TODO("Not yet implemented")
    }

    private val _declaredMethods by lazy {
        val myMethods = declaration.getDeclaredFunctions()
            .filter {
                // filter out constructors
                it.simpleName.asString() != name
            }.map {
                KspMethodElement(
                    env = env,
                    containing = this,
                    declaration = it
                )
            }
        val companionMethods = declaration.findCompanionObject()
            ?.let {
                env.wrapClassDeclaration(it)
            }?.getDeclaredMethods()
            ?.filter {
                it.isStatic()
            } ?: emptyList()

        myMethods + syntheticGetterSetterMethods + companionMethods
    }

    override fun getDeclaredMethods(): List<XMethodElement> {
        return _declaredMethods
    }

    override fun getConstructors(): List<XConstructorElement> {
        val constructors = declaration.getDeclaredFunctions()
            .filter {
                it.simpleName.asString() == name
            }.toMutableList()
        declaration.primaryConstructor?.let { primary ->
            // workaround for https://github.com/android/kotlin/issues/136
            // TODO remove once that bug is fixed
            if (primary.simpleName.asString() != "<init>") {
                constructors.add(primary)
            }
        }

        return constructors.map {
            KspConstructorElement(
                env = env,
                containing = this,
                declaration = it
            )
        }
    }

    override fun getSuperInterfaceElements(): List<XTypeElement> {
        return declaration.superTypes.asSequence().mapNotNull {
            it.resolve()?.declaration
        }.filterIsInstance<KSClassDeclaration>()
            .filter {
                it.classKind == ClassKind.INTERFACE
            }.mapTo(mutableListOf()) {
                env.wrapClassDeclaration(it)
            }
    }
}
