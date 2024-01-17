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

import androidx.room.compiler.codegen.java.JavaAnnotationSpec
import androidx.room.compiler.codegen.kotlin.KotlinAnnotationSpec

interface XAnnotationSpec : TargetLanguage {

    interface Builder : TargetLanguage {
        // TODO(b/127483380): Only supports one value, add support for arrays
        fun addMember(name: String, code: XCodeBlock): Builder
        fun build(): XAnnotationSpec

        companion object {
            fun Builder.addMember(
                name: String,
                format: String,
                vararg args: Any?
            ): Builder = addMember(
                name,
                XCodeBlock.of(language, format, *args)
            )
        }
    }

    companion object {
        fun builder(language: CodeLanguage, className: XClassName): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaAnnotationSpec.Builder(
                    com.squareup.javapoet.AnnotationSpec.builder(className.java)
                )
                CodeLanguage.KOTLIN -> KotlinAnnotationSpec.Builder(
                    com.squareup.kotlinpoet.AnnotationSpec.builder(className.kotlin)
                )
            }
        }
    }
}
