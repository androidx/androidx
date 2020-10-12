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
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Implementation of [XHasModifiers] for ksp declarations.
 */
class KspHasModifiers(
    private val declaration: KSDeclaration
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
}