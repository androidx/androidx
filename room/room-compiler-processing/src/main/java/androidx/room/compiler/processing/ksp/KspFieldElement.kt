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
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_FIELD
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal class KspFieldElement(
    env: KspProcessingEnv,
    override val declaration: KSPropertyDeclaration,
    val containing: KspMemberContainer
) : KspElement(env, declaration),
    XFieldElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE_OR_FIELD) {

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(declaration, containing)
    }

    override val enclosingElement: KspMemberContainer by lazy {
        declaration.requireEnclosingMemberContainer(env)
    }

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val type: KspType by lazy {
        env.wrap(
            originatingReference = declaration.type,
            ksType = declaration.typeAsMemberOf(containing.type?.ksType)
        )
    }

    /**
     * The original field from the declaration. For instance, if you have `val x:String` declared
     * in `BaseClass` and inherited in `SubClass`, if `this` is the instance in `SubClass`,
     * [declarationField] will be the instance in `BaseClass`. If `this` is the instance in
     * `BaseClass`, [declarationField] will be `null`.
     */
    val declarationField: KspFieldElement? by lazy {
        val declaredIn = declaration.closestClassDeclaration()
        if (declaredIn == null || declaredIn == containing.declaration) {
            null
        } else {
            KspFieldElement(
                env = env,
                declaration = declaration,
                containing = env.wrapClassDeclaration(declaredIn)
            )
        }
    }

    val syntheticAccessors: List<KspSyntheticPropertyMethodElement> by lazy {
        when {
            declaration.hasJvmFieldAnnotation() -> {
                // jvm fields cannot have accessors but KSP generates synthetic accessors for
                // them. We check for JVM field first before checking the getter
                emptyList()
            }
            declaration.isPrivate() -> emptyList()

            else -> {
                sequenceOf(declaration.getter, declaration.setter)
                    .filterNotNull()
                    .filterNot {
                        // KAPT does not generate methods for privates, KSP does so we filter
                        // them out.
                        it.modifiers.contains(Modifier.PRIVATE)
                    }
                    .filter {
                        if (isStatic()) {
                            // static fields are the properties that are coming from the
                            // companion. Whether we'll generate method for it or not depends on
                            // the JVMStatic annotation
                            it.hasJvmStaticAnnotation() || declaration.hasJvmStaticAnnotation()
                        } else {
                            true
                        }
                    }
                    .map { accessor ->
                        KspSyntheticPropertyMethodElement.create(
                            env = env,
                            field = this,
                            accessor = accessor
                        )
                    }.toList()
            }
        }
    }

    val syntheticSetter
        get() = syntheticAccessors.firstOrNull {
            it.parameters.size == 1
        }

    override fun asMemberOf(other: XType): KspType {
        if (containing.type?.isSameType(other) != false) {
            return type
        }
        check(other is KspType)
        val asMember = declaration.typeAsMemberOf(other.ksType)
        return env.wrap(
            originatingReference = declaration.type,
            ksType = asMember
        )
    }

    override fun copyTo(newContainer: XTypeElement): KspFieldElement {
        check(newContainer is KspTypeElement) {
            "Unexpected container (${newContainer::class}), expected KspTypeElement"
        }
        return KspFieldElement(
            env = env,
            declaration = declaration,
            containing = newContainer
        )
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            declaration: KSPropertyDeclaration
        ): KspFieldElement {
            return KspFieldElement(
                env = env,
                declaration = declaration,
                containing = declaration.requireEnclosingMemberContainer(env)
            )
        }
    }
}
