/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XConstructorType
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KspConstructorType
import androidx.room.compiler.processing.ksp.KspExecutableElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import androidx.room.compiler.processing.ksp.KspTypeElement
import androidx.room.compiler.processing.ksp.requireEnclosingMemberContainer
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Represents the no-arg constructor of a type element for which all parameters in the primary
 * constructor have default values.
 */
internal class KspSyntheticNoArgConstructorElement(
    env: KspProcessingEnv,
    declaration: KSFunctionDeclaration
) : KspExecutableElement(env, declaration), XConstructorElement {

    override val enclosingElement: KspTypeElement by lazy {
        declaration.requireEnclosingMemberContainer(env) as? KspTypeElement
            ?: error("Constructor parent must be a type element $this")
    }

    override val name: String
        get() = "<init>"

    override val parameters: List<XExecutableParameterElement>
        get() = emptyList()

    override val executableType: XConstructorType by lazy {
        KspConstructorType(
            env = env,
            origin = this,
            containing = this.enclosingElement.type
        )
    }

    override fun asMemberOf(other: XType): XConstructorType {
        check(other is KspType)
        return KspConstructorType(
            env = env,
            origin = this,
            containing = other
        )
    }
}