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

import androidx.room.compiler.processing.XConstructorElement
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal class KspConstructorElement(
    env: KspProcessingEnv,
    override val containing: KspTypeElement,
    declaration: KSFunctionDeclaration
) : KspExecutableElement(
    env = env,
    containing = containing,
    declaration = declaration
),
    XConstructorElement {
    override val enclosingElement: KspTypeElement by lazy {
        declaration.requireEnclosingMemberContainer(env) as? KspTypeElement
            ?: error("Constructor parent must be a type element $this")
    }
}
