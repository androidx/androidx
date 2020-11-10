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

import androidx.room.compiler.processing.XHasModifiers
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Implementation of [XHasModifiers] for ksp declarations.
 */
sealed class KspHasModifiers(
    protected val declaration: KSDeclaration
) : XHasModifiers {
    override fun isPublic(): Boolean {
        return declaration.isPublic()
    }

    override fun isProtected(): Boolean {
        return declaration.isProtected()
    }

    override fun isAbstract(): Boolean {
        return declaration.modifiers.contains(Modifier.ABSTRACT)
    }

    override fun isPrivate(): Boolean {
        return declaration.isPrivate()
    }

    override fun isStatic(): Boolean {
        return declaration.modifiers.contains(Modifier.JAVA_STATIC) || declaration.isJvmStatic()
    }

    override fun isTransient(): Boolean {
        return declaration.modifiers.contains(Modifier.JAVA_TRANSIENT)
    }

    override fun isFinal(): Boolean {
        return !declaration.isOpen()
    }

    private class Declaration(declaration: KSDeclaration) : KspHasModifiers(declaration)

    private class PropertyField(
        declaration: KSDeclaration
    ) : KspHasModifiers(declaration) {
        private val jvmField by lazy {
            declaration.isJvmField()
        }

        override fun isPublic(): Boolean {
            return jvmField && super.isPublic()
        }

        override fun isProtected(): Boolean {
            return jvmField && super.isProtected()
        }

        override fun isPrivate(): Boolean {
            return if (jvmField) {
                super.isPrivate()
            } else {
                // it is always private unless it is a jvm field
                true
            }
        }
    }

    /**
     * Handles accessor visibility when there is an accessor declared in code.
     * We cannot simply merge modifiers of the property and the accessor as the visibility rules
     * of the declaration is more complicated than just looking at modifiers.
     */
    private class PropertyFieldAccessor(
        private val accessor: KSPropertyAccessor
    ) : KspHasModifiers(accessor.receiver) {
        override fun isPublic(): Boolean {
            return accessor.modifiers.contains(Modifier.PUBLIC) ||
                (!isPrivate() && !isProtected() && super.isPublic())
        }

        override fun isProtected(): Boolean {
            return accessor.modifiers.contains(Modifier.PROTECTED) ||
                (!isPrivate() && super.isProtected())
        }

        override fun isPrivate(): Boolean {
            return accessor.modifiers.contains(Modifier.PRIVATE) ||
                super.isPrivate()
        }
    }

    companion object {
        fun createForSyntheticAccessor(
            property: KSPropertyDeclaration,
            accessor: KSPropertyAccessor?
        ): XHasModifiers {
            if (accessor != null) {
                return PropertyFieldAccessor(accessor)
            }
            return Declaration(property)
        }

        fun create(owner: KSPropertyDeclaration): XHasModifiers {
            return PropertyField(owner)
        }

        fun create(owner: KSFunctionDeclaration): XHasModifiers {
            return Declaration(owner)
        }

        fun create(owner: KSClassDeclaration): XHasModifiers {
            return Declaration(owner)
        }
    }
}
