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

package androidx.serialization.compiler.codegen.java

import com.google.auto.common.GeneratedAnnotations
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier

/**
 * Central state holder for Java generation.
 *
 * Individual Java generators are implemented as extension methods.
 */
internal class JavaGenerator(
    val sourceVersion: SourceVersion = SourceVersion.latest(),
    private val generatedAnnotation: AnnotationSpec? = null
) {
    constructor(
        processingEnv: ProcessingEnvironment,
        generatingClassName: String
    ) : this (
        sourceVersion = processingEnv.sourceVersion,
        generatedAnnotation = GeneratedAnnotations.generatedAnnotation(
            processingEnv.elementUtils,
            processingEnv.sourceVersion
        ).orElse(null)?.let { typeElement ->
            AnnotationSpec.builder(ClassName.get(typeElement))
                .addMember("value", "\$S", generatingClassName)
                .build()
        }
    )

    /** Build a class spec as a java file. */
    inline fun buildClass(
        className: ClassName,
        originatingElement: Element,
        vararg modifiers: Modifier,
        init: TypeSpec.Builder.() -> Unit
    ): JavaFile {
        return buildJavaFile(
            TypeSpec.classBuilder(className).apply {
                addOriginatingElement(originatingElement)
                addModifiers(*modifiers)
                init()
            },
            className
        )
    }

    /** Finish a type spec builder, adding a generated annotation if present. */
    fun buildJavaFile(
        builder: TypeSpec.Builder,
        className: ClassName
    ): JavaFile {
        val typeSpec = builder.run {
            generatedAnnotation?.let { addAnnotation(it) }
            build()
        }
        return JavaFile.builder(className.packageName(), typeSpec).indent(INDENTATION).build()
    }

    private companion object {
        const val INDENTATION = "    "
    }
}
