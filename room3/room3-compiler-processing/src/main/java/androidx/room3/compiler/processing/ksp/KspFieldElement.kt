/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.room3.compiler.processing.XFieldElement
import androidx.room3.compiler.processing.XHasModifiers
import androidx.room3.compiler.processing.XMemberContainer
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.ksp.KspAnnotated.Companion.plus
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_FIELD
import androidx.room3.compiler.processing.ksp.KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_PROPERTY
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

internal class KspFieldElement(
    env: KspProcessingEnv,
    override val declaration: KSPropertyDeclaration,
    override val owner: KspPropertyElement,
) :
    KspElement(env, declaration),
    XFieldElement,
    XHasModifiers by KspHasModifiers.createFieldModifiers(declaration),
    InternalXAnnotated by createInternalAnnotated(env, declaration) {

    override val jvmDescriptor: String
        get() = this.jvmDescriptor()

    override val name: String
        get() = owner.name

    override val fallbackLocationText: String
        get() = owner.fallbackLocationText

    override val docComment: String?
        get() = owner.docComment

    override val closestMemberContainer: XMemberContainer
        get() = owner.closestMemberContainer

    override val enclosingElement: KspMemberContainer
        get() = owner.enclosingElement

    override val type: XType
        get() = owner.type

    override fun asMemberOf(other: XType): XType {
        return owner.asMemberOf(other)
    }

    companion object {
        private fun createInternalAnnotated(
            env: KspProcessingEnv,
            declaration: KSPropertyDeclaration,
        ): KspAnnotated {
            val fieldAnnotated = KspAnnotated.create(env, declaration, NO_USE_SITE_OR_FIELD)
            return if (env.config.includePropertyAnnotationsInFields) {
                val propertyAnnotated =
                    KspAnnotated.create(env, declaration, NO_USE_SITE_OR_PROPERTY)
                propertyAnnotated + fieldAnnotated
            } else {
                fieldAnnotated
            }
        }
    }
}
