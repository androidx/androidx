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

import androidx.room.compiler.processing.XExecutableType
import androidx.room.compiler.processing.XType
import javax.lang.model.type.ExecutableType

internal abstract class JavacExecutableType(
    val env: JavacProcessingEnv,
    open val element: JavacExecutableElement,
    val executableType: ExecutableType
) : XExecutableType {

    override val parameterTypes: List<JavacType> by lazy {
        executableType.parameterTypes.mapIndexed { index, typeMirror ->
            env.wrap(
                typeMirror = typeMirror,
                kotlinType = element.parameters[index].kotlinType,
                elementNullability = element.parameters[index].element.nullability
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is JavacExecutableType) return false
        return executableType == other.executableType
    }

    override fun hashCode(): Int {
        return executableType.hashCode()
    }

    override fun toString(): String {
        return executableType.toString()
    }

    override val thrownTypes: List<XType>
        // The thrown types are the same as on the element since those can't change
        get() = element.thrownTypes

    override fun isSameType(other: XExecutableType): Boolean {
        return other is JavacExecutableType &&
            env.typeUtils.isSameType(executableType, other.executableType)
    }
}
