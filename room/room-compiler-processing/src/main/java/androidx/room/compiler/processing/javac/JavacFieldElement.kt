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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XHasModifiers
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.javac.kotlin.KmProperty
import androidx.room.compiler.processing.javac.kotlin.KmType
import javax.lang.model.element.VariableElement

internal class JavacFieldElement(
    env: JavacProcessingEnv,
    containing: JavacTypeElement,
    element: VariableElement
) : JavacVariableElement(env, containing, element),
    XFieldElement,
    XHasModifiers by JavacHasModifiers(element) {

    private val kotlinMetadata: KmProperty? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata?.getPropertyMetadata(name)
    }

    override val kotlinType: KmType?
        get() = kotlinMetadata?.type

    override val enclosingElement: XTypeElement by lazy {
        element.requireEnclosingType(env)
    }

    override fun copyTo(newContainer: XTypeElement): JavacFieldElement {
        check(newContainer is JavacTypeElement) {
            "Unexpected container (${newContainer::class}), expected JavacTypeElement"
        }
        return JavacFieldElement(
            env = env,
            containing = newContainer,
            element = element
        )
    }
}
