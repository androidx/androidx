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
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.ksp.KspAnnotated.UseSiteFilter.Companion.NO_USE_SITE
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

internal abstract class KspExecutableElement(
    env: KspProcessingEnv,
    val containing: KspTypeElement,
    override val declaration: KSFunctionDeclaration
) : KspElement(
    env = env,
    declaration = declaration
),
    XExecutableElement,
    XHasModifiers by KspHasModifiers.create(declaration),
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = declaration,
        filter = NO_USE_SITE
    ) {

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(containing, declaration)
    }

    override val enclosingTypeElement: XTypeElement by lazy {
        declaration.requireEnclosingTypeElement(env)
    }

    override val parameters: List<XExecutableParameterElement> by lazy {
        declaration.parameters.map {
            KspExecutableParameterElement(
                env = env,
                method = this,
                parameter = it
            )
        }
    }

    override fun isVarArgs(): Boolean {
        // in java, only the last argument can be a vararg so for suspend functions, it is never
        // a vararg function. this would change if room generated kotlin code
        return !declaration.modifiers.contains(Modifier.SUSPEND) &&
            declaration.parameters.any {
                it.isVararg
            }
    }
}
