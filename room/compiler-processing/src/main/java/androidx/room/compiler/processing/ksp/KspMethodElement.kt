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

import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import org.jetbrains.kotlin.ksp.symbol.KSFunctionDeclaration
import org.jetbrains.kotlin.ksp.symbol.Modifier

internal class KspMethodElement(
    env: KspProcessingEnv,
    containing: KspTypeElement,
    declaration: KSFunctionDeclaration
) : KspExecutableElement(
    env = env,
    containing = containing,
    declaration = declaration
), XMethodElement {

    override val name: String by lazy {
        declaration.simpleName.asString()
    }

    override val returnType: XType
        get() = TODO(
            """
            Implement return type.
            Need to handle suspend functions where their signature is different as long as we
            generate java code.
        """.trimIndent()
        )

    override val executableType: XMethodType
        get() = TODO("Not yet implemented")

    override fun isJavaDefault(): Boolean {
        return declaration.modifiers.contains(Modifier.JAVA_DEFAULT) || declaration.isJvmDefault()
    }

    override fun asMemberOf(other: XDeclaredType): XMethodType {
        TODO("Not yet implemented")
    }

    override fun hasKotlinDefaultImpl(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSuspendFunction(): Boolean {
        return declaration.modifiers.contains(Modifier.SUSPEND)
    }

    override fun overrides(other: XMethodElement, owner: XTypeElement): Boolean {
        return other is KspMethodElement && declaration.overrides(other.declaration)
    }

    override fun copyTo(newContainer: XTypeElement): XMethodElement {
        check(newContainer is KspTypeElement)
        return KspMethodElement(
            env = env,
            containing = newContainer,
            declaration = declaration
        )
    }
}
