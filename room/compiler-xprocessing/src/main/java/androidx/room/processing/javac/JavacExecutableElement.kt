/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing.javac

import androidx.room.processing.XExecutableElement
import androidx.room.processing.XTypeElement
import androidx.room.processing.javac.kotlin.descriptor
import javax.lang.model.element.ExecutableElement

internal abstract class JavacExecutableElement(
    env: JavacProcessingEnv,
    val containing: JavacTypeElement,
    override val element: ExecutableElement
) : JavacElement(
    env,
    element
), XExecutableElement {
    protected val kotlinMetadata by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata
    }

    val descriptor by lazy {
        element.descriptor()
    }

    override val enclosingElement: XTypeElement
        get() = super.enclosingElement as XTypeElement

    override val parameters: List<JavacVariableElement> by lazy {
        val kotlinParamNames = kotlinMetadata?.getParameterNames(element)
        element.parameters.mapIndexed { index, variable ->
            JavacMethodParameter(
                env = env,
                containing = containing,
                element = variable,
                kotlinName = kotlinParamNames?.getOrNull(index)
            )
        }
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(element, containing)
    }

    override fun isVarArgs(): Boolean {
        return element.isVarArgs
    }

    companion object {
        internal const val DEFAULT_IMPLS_CLASS_NAME = "DefaultImpls"
    }
}
