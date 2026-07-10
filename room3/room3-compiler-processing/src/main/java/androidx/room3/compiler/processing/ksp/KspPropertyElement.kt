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

import androidx.room3.compiler.processing.InternalXAnnotated
import androidx.room3.compiler.processing.XHasModifiers
import androidx.room3.compiler.processing.XPropertyElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_PROPERTY
import androidx.room3.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal open class KspPropertyElement(
    env: KspProcessingEnv,
    override val declaration: KSPropertyDeclaration,
) :
    KspElement(env, declaration),
    XPropertyElement,
    XHasModifiers by KspHasModifiers.createPropertyModifiers(declaration),
    InternalXAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE_OR_PROPERTY) {

    override val enclosingElement: KspMemberContainer by lazy {
        declaration.requireEnclosingMemberContainer(env)
    }

    override val closestMemberContainer: KspMemberContainer by lazy { enclosingElement }

    override val name: String by lazy { declaration.simpleName.asString() }

    override val type: KspType by lazy { createAsMemberOf(closestMemberContainer.type) }

    override val backingField: KspFieldElement? by lazy {
        if (declaration.hasBackingField) {
            KspFieldElement(env, declaration, this)
        } else {
            null
        }
    }

    val syntheticAccessors: List<KspSyntheticPropertyMethodElement> by lazy {
        listOfNotNull(getter, setter)
    }

    val syntheticStaticAccessors: List<KspSyntheticPropertyMethodElement> by lazy {
        syntheticAccessors.mapNotNull { it.syntheticStaticAccessor }
    }

    override val getter: KspSyntheticPropertyMethodElement? by lazy {
        declaration.getter?.let { createSyntheticMethod(it) }
    }

    override val setter: KspSyntheticPropertyMethodElement? by lazy {
        declaration.setter?.let { createSyntheticMethod(it) }
    }

    private fun createSyntheticMethod(
        accessor: KSPropertyAccessor
    ): KspSyntheticPropertyMethodElement? {
        return if (
            // jvm fields cannot have accessors but KSP generates synthetic accessors for
            // them. We check for JVM field first before checking the getter
            declaration.hasJvmFieldAnnotation() ||
                declaration.isPrivate() ||
                // No accessors are needed for const properties:
                // https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-fields
                declaration.modifiers.contains(Modifier.CONST) ||
                accessor.modifiers.contains(Modifier.PRIVATE)
        ) {
            null
        } else {
            KspSyntheticPropertyMethodElement.create(env = env, prop = this, accessor = accessor)
        }
    }

    override fun asMemberOf(other: XType): KspType =
        if (closestMemberContainer.type?.isSameType(other) != false) {
            type
        } else {
            createAsMemberOf(other)
        }

    private fun createAsMemberOf(container: XType?): KspType {
        check(container is KspType?)
        return env.wrap(
                originatingReference = declaration.type,
                ksType = declaration.typeAsMemberOf(container?.ksType),
            )
            .copyWithScope(
                KSTypeVarianceResolverScope.PropertyType(prop = this, asMemberOf = container)
            )
    }

    companion object {
        fun create(env: KspProcessingEnv, declaration: KSPropertyDeclaration): KspPropertyElement {
            return KspPropertyElement(env, declaration)
        }
    }
}
