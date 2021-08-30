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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.javac.kotlin.KmType
import androidx.room.compiler.processing.javac.kotlin.KmValueParameter
import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import javax.lang.model.element.VariableElement

internal class JavacMethodParameter(
    env: JavacProcessingEnv,
    private val executable: JavacExecutableElement,
    containing: JavacTypeElement,
    element: VariableElement,
    val kotlinMetadata: KmValueParameter?,
    val argIndex: Int
) : JavacVariableElement(env, containing, element), XExecutableParameterElement {

    override val name: String
        get() = (kotlinMetadata?.name ?: super.name).sanitizeAsJavaParameterName(
            argIndex = argIndex
        )

    override val kotlinType: KmType?
        get() = kotlinMetadata?.type

    override val hasDefaultValue: Boolean
        get() = kotlinMetadata?.hasDefault() ?: false

    override val fallbackLocationText: String
        get() = if (executable is JavacMethodElement && executable.isSuspendFunction() &&
            this === executable.parameters.last()
        ) {
            "return type of ${executable.fallbackLocationText}"
        } else {
            "$name in ${executable.fallbackLocationText}"
        }
}
