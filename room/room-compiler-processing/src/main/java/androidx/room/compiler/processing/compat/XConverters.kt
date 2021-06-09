/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.compat

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XExecutableElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.javac.JavacElement
import androidx.room.compiler.processing.javac.JavacExecutableElement
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.javac.JavacType
import androidx.room.compiler.processing.javac.JavacTypeElement
import androidx.room.compiler.processing.javac.JavacVariableElement
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

// Migration APIs for converting between Javac and XProcessing types.
object XConverters {

    @JvmStatic
    fun XElement.toJavac(): Element = (this as JavacElement).element

    @JvmStatic
    fun XTypeElement.toJavac(): TypeElement = (this as JavacTypeElement).element

    @JvmStatic
    fun XExecutableElement.toJavac(): ExecutableElement = (this as JavacExecutableElement).element

    @JvmStatic
    fun XVariableElement.toJavac(): VariableElement = (this as JavacVariableElement).element

    @JvmStatic
    fun XType.toJavac(): TypeMirror = (this as JavacType).typeMirror

    @JvmStatic
    fun Element.toXProcessing(env: XProcessingEnv): XElement {
        return when (this) {
            is TypeElement -> this.toXProcessing(env)
            is ExecutableElement -> this.toXProcessing(env)
            is VariableElement -> this.toXProcessing(env)
            else -> error(
                "Don't know how to convert element of type '${this::class}' to a XElement"
            )
        }
    }

    @JvmStatic
    fun TypeElement.toXProcessing(env: XProcessingEnv): XTypeElement =
        (env as JavacProcessingEnv).wrapTypeElement(this)

    @JvmStatic
    fun ExecutableElement.toXProcessing(env: XProcessingEnv): XExecutableElement =
        (env as JavacProcessingEnv).wrapExecutableElement(this)

    @JvmStatic
    fun VariableElement.toXProcessing(env: XProcessingEnv): XVariableElement =
        (env as JavacProcessingEnv).wrapVariableElement(this)

    // TODO: TypeMirror to XType, this will be more complicated since location context is lost...
}