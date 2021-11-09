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
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isProtected
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Visibility

/**
 * Implementation of [XHasModifiers] for ksp declarations.
 */
sealed class KspHasModifiers(
    protected val declaration: KSDeclaration
) : XHasModifiers {
    override fun isPublic(): Boolean {
        // internals are public from java but KSP's declaration.isPublic excludes them.
        return declaration.getVisibility() == Visibility.INTERNAL ||
            declaration.getVisibility() == Visibility.PUBLIC
    }

    override fun isProtected(): Boolean {
        return declaration.isProtected()
    }

    override fun isAbstract(): Boolean {
        return declaration.modifiers.contains(Modifier.ABSTRACT) ||
            when (declaration) {
                is KSPropertyDeclaration -> declaration.isAbstract()
                is KSClassDeclaration -> declaration.isAbstract()
                is KSFunctionDeclaration -> declaration.isAbstract
                else -> false
            }
    }

    override fun isPrivate(): Boolean {
        return declaration.isPrivate()
    }

    override fun isStatic(): Boolean {
        return declaration.isStatic()
    }

    override fun isTransient(): Boolean {
        return declaration.isTransient()
    }

    override fun isFinal(): Boolean {
        return !declaration.isOpen()
    }

    private class Declaration(declaration: KSDeclaration) : KspHasModifiers(declaration)

    private class ClassDeclaration(declaration: KSDeclaration) : KspHasModifiers(declaration) {
        override fun isStatic(): Boolean {
            if (declaration.isStatic()) {
                return true
            }
            // inner classes in kotlin are static by default unless they have inner modifier.
            // for .class files, there is currently a bug:
            // https://github.com/google/ksp/pull/232 and once it is fixed, inner modifier will
            // be reported for .class files as well.
            if (declaration.origin != Origin.JAVA &&
                declaration.parentDeclaration is KSClassDeclaration // nested class
            ) {
                return !declaration.modifiers.contains(Modifier.INNER)
            }
            return false
        }
    }

    private class PropertyField(
        declaration: KSPropertyDeclaration
    ) : KspHasModifiers(declaration) {
        private val acceptDeclarationModifiers by lazy {
            // Deciding whether we should read modifiers from a KSPropertyDeclaration is not very
            // straightforward. (jvmField == true -> read modifiers from declaration)
            // When origin is java, always read.
            // When origin is kotlin, read if it has @JvmField annotation
            // When origin is .class, it depends whether the property was originally a kotlin code
            // or java code.
            // Unfortunately, we don't have a way of checking it as KotlinMetadata annotation is not
            // visible via KSP. We approximate it by checking if it is delegated or not.
            when (declaration.origin) {
                Origin.JAVA, Origin.JAVA_LIB -> true
                Origin.KOTLIN, Origin.KOTLIN_LIB -> declaration.hasJvmFieldAnnotation()
                else -> false
            }
        }

        override fun isPublic(): Boolean {
            return acceptDeclarationModifiers && super.isPublic()
        }

        override fun isProtected(): Boolean {
            return acceptDeclarationModifiers && super.isProtected()
        }

        override fun isPrivate(): Boolean {
            return if (acceptDeclarationModifiers) {
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
            return ClassDeclaration(owner)
        }
    }
}
