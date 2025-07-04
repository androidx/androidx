/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen

import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.java.JavaAnnotationSpec
import androidx.room.compiler.codegen.kotlin.KotlinAnnotationSpec
import com.squareup.kotlinpoet.javapoet.JAnnotationSpec
import com.squareup.kotlinpoet.javapoet.KAnnotationSpec

interface XAnnotationSpec {

    fun toBuilder(): Builder

    interface Builder {
        // TODO(b/127483380): Only supports one value, add support for arrays
        fun addMember(name: String, code: XCodeBlock): Builder

        fun addMember(name: String, format: String, vararg args: Any): Builder =
            addMember(name, XCodeBlock.of(format, *args))

        fun build(): XAnnotationSpec

        companion object {
            fun Builder.applyTo(block: Builder.(CodeLanguage) -> Unit) = apply {
                when (this) {
                    is XAnnotationSpecImpl.Builder -> {
                        this.java.block(CodeLanguage.JAVA)
                        this.kotlin.block(CodeLanguage.KOTLIN)
                    }
                    is JavaAnnotationSpec.Builder -> block(CodeLanguage.JAVA)
                    is KotlinAnnotationSpec.Builder -> block(CodeLanguage.KOTLIN)
                }
            }

            fun Builder.applyTo(language: CodeLanguage, block: Builder.() -> Unit) =
                applyTo { codeLanguage ->
                    if (codeLanguage == language) {
                        block()
                    }
                }
        }
    }

    companion object {
        @JvmStatic fun of(className: XClassName) = builder(className).build()

        @JvmStatic
        fun builder(className: XClassName): Builder =
            XAnnotationSpecImpl.Builder(
                JavaAnnotationSpec.Builder(JAnnotationSpec.builder(className.java)),
                KotlinAnnotationSpec.Builder(KAnnotationSpec.builder(className.kotlin)),
            )
    }
}
