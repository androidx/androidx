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

package androidx.hilt.lifecycle

import androidx.hilt.ClassNames
import androidx.hilt.ext.L
import androidx.hilt.ext.S
import androidx.hilt.ext.T
import androidx.hilt.ext.W
import androidx.hilt.ext.addGeneratedAnnotation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

/**
 * Source generator to support Hilt injection of ViewModels.
 *
 * Should generate:
 * ```
 * @Module
 * @InstallIn(ViewModelComponent.class)
 * public final class $_HiltModule {
 *   @Provides
 *   @IntoMap
 *   @StringKey("pkg.$")
 *   @InternalViewModelInjectMap
 *   public static ViewModel provide(dep1, Dep1, dep2, Dep2, ...) {
 *     return new $(dep1, dep2, ...)
 *   }
 * }
 * ```
 */
internal class ViewModelGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val injectedViewModel: ViewModelInjectElements
) {
    fun generate() {
        val hiltModuleTypeSpec = TypeSpec.classBuilder(injectedViewModel.moduleClassName)
            .addOriginatingElement(injectedViewModel.typeElement)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addAnnotation(ClassNames.MODULE)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.INSTALL_IN)
                    .addMember("value", "$T.class", ClassNames.VIEW_MODEL_COMPONENT)
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.ORIGINATING_ELEMENT)
                    .addMember(
                        "topLevelClass",
                        "$T.class",
                        injectedViewModel.className.topLevelClassName()
                    )
                    .build()
            )
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("provide")
                    .addAnnotation(ClassNames.PROVIDES)
                    .addAnnotation(ClassNames.INTO_MAP)
                    .addAnnotation(
                        AnnotationSpec.builder(ClassNames.STRING_KEY)
                            .addMember("value", S, injectedViewModel.className.reflectionName())
                            .build()
                    )
                    .addAnnotation(ClassNames.VIEW_MODEL_INJECT_MAP_QUALIFIER)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ClassNames.VIEW_MODEL)
                    .apply {
                        injectedViewModel.dependencyRequests.forEach { dependency ->
                            addParameter(
                                ParameterSpec.builder(dependency.type, dependency.name)
                                    .apply {
                                        dependency.qualifier?.let { addAnnotation(it) }
                                    }.build()
                            )
                        }
                        val constructorArgs = injectedViewModel.dependencyRequests.map {
                            CodeBlock.of(L, it.name)
                        }
                        addStatement(
                            "return new $T($L)",
                            injectedViewModel.className,
                            CodeBlock.join(constructorArgs, ",$W")
                        )
                    }
                    .build()
            )
            .build()
        JavaFile.builder(injectedViewModel.moduleClassName.packageName(), hiltModuleTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }
}