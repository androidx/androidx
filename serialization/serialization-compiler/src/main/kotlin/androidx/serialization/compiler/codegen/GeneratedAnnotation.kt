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

package androidx.serialization.compiler.codegen

import androidx.serialization.compiler.processing.ext.packageElement
import com.google.auto.common.GeneratedAnnotations
import com.squareup.javapoet.ClassName as JavaClassName
import com.squareup.javapoet.AnnotationSpec as JavaAnnotationSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.NestingKind

internal class GeneratedAnnotation(
    val generatingClassName: String,
    val packageName: String,
    val simpleName: String = "Generated"
) {
    private lateinit var javaAnnotationSpec: JavaAnnotationSpec

    fun toJavaAnnotationSpec(): JavaAnnotationSpec {
        if (!::javaAnnotationSpec.isInitialized) {
            javaAnnotationSpec = JavaAnnotationSpec
                .builder(JavaClassName.get(packageName, simpleName))
                .addMember("value", "\$S", generatingClassName)
                .build()
        }

        return javaAnnotationSpec
    }

    companion object {
        fun fromEnvironment(
            processingEnv: ProcessingEnvironment,
            generatingClassName: String
        ): GeneratedAnnotation? {
            return GeneratedAnnotations.generatedAnnotation(
                processingEnv.elementUtils,
                processingEnv.sourceVersion
            ).orElse(null).let { typeElement ->
                require(typeElement.nestingKind == NestingKind.TOP_LEVEL) {
                    "Expected @Generated annotation to be a top-level type"
                }

                GeneratedAnnotation(
                    generatingClassName,
                    typeElement.packageElement.toString(),
                    typeElement.simpleName.toString()
                )
            }
        }
    }
}