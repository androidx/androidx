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

import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.addOriginatingElement
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.toKClassName

interface XTypeSpec : TargetLanguage {

    val className: JClassName

    interface Builder : TargetLanguage {
        fun superclass(typeName: JTypeName): Builder
        fun addAnnotation(annotation: XAnnotationSpec)

        // TODO(b/247241418): Maybe make a XPropertySpec ?
        fun addProperty(
            typeName: JTypeName,
            name: String,
            nullability: XNullability,
            visibility: VisibilityModifier,
            isMutable: Boolean = false,
            initExpr: XCodeBlock? = null,
            annotations: List<XAnnotationSpec> = emptyList()
        ): Builder

        fun addFunction(functionSpec: XFunSpec): Builder
        fun build(): XTypeSpec

        companion object {
            fun Builder.apply(
                javaTypeBuilder: com.squareup.javapoet.TypeSpec.Builder.() -> Unit,
                kotlinTypeBuilder: com.squareup.kotlinpoet.TypeSpec.Builder.() -> Unit,
            ): Builder = apply {
                when (language) {
                    CodeLanguage.JAVA -> {
                        check(this is JavaTypeSpec.Builder)
                        this.actual.javaTypeBuilder()
                    }
                    CodeLanguage.KOTLIN -> {
                        check(this is KotlinTypeSpec.Builder)
                        this.actual.kotlinTypeBuilder()
                    }
                }
            }
        }
    }

    companion object {
        fun classBuilder(language: CodeLanguage, className: JClassName): Builder {
            return when (language) {
                CodeLanguage.JAVA -> JavaTypeSpec.Builder(
                    className = className,
                    actual = com.squareup.javapoet.TypeSpec.classBuilder(className)
                )
                CodeLanguage.KOTLIN -> KotlinTypeSpec.Builder(
                    className = className,
                    actual = com.squareup.kotlinpoet.TypeSpec.classBuilder(className.toKClassName())
                )
            }
        }
    }
}

fun XTypeSpec.Builder.addOriginatingElement(element: XElement) = apply {
    when (language) {
        CodeLanguage.JAVA -> {
            check(this is JavaTypeSpec.Builder)
            actual.addOriginatingElement(element)
        }
        CodeLanguage.KOTLIN -> {
            check(this is KotlinTypeSpec.Builder)
            actual.addOriginatingElement(element)
        }
    }
}