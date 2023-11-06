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
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE_OR_FIELD
import androidx.room.compiler.processing.ksp.synthetic.KspSyntheticPropertyMethodElement
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal class KspFieldElement(
    env: KspProcessingEnv,
    override val declaration: KSPropertyDeclaration,
) : KspElement(env, declaration),
    XFieldElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(env, declaration, NO_USE_SITE_OR_FIELD) {

    override val enclosingElement: KspMemberContainer by lazy {
        declaration.requireEnclosingMemberContainer(env)
    }

    override val closestMemberContainer: KspMemberContainer by lazy {
        enclosingElement
    }

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val type: KspType by lazy {
        createAsMemberOf(closestMemberContainer.type)
    }

    override val jvmDescriptor: String
        get() = this.jvmDescriptor()

    val syntheticAccessors: List<KspSyntheticPropertyMethodElement> by lazy {
        listOfNotNull(getter, setter)
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
            accessor.modifiers.contains(Modifier.PRIVATE)) {
            null
        } else {
            KspSyntheticPropertyMethodElement.create(
                env = env,
                field = this,
                accessor = accessor,
                isSyntheticStatic = false
            )
        }
    }

    override fun asMemberOf(other: XType): KspType {
        return if (closestMemberContainer.type?.isSameType(other) != false) {
            type
        } else {
            return createAsMemberOf(other)
        }
    }

    private fun createAsMemberOf(container: XType?): KspType {
        check(container is KspType?)
        return env.wrap(
            originatingReference = declaration.type,
            ksType = declaration.typeAsMemberOf(container?.ksType)
        ).copyWithScope(
            KSTypeVarianceResolverScope.PropertyType(
                field = this,
                asMemberOf = container,
            )
        )
    }

    companion object {
        fun create(env: KspProcessingEnv, declaration: KSPropertyDeclaration): KspFieldElement {
            return KspFieldElement(env, declaration)
        }
    }
}
