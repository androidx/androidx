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

package androidx.hilt.work

import androidx.hilt.ClassNames
import androidx.hilt.ext.S
import androidx.hilt.ext.T
import androidx.hilt.ext.addGeneratedAnnotation
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.compiler.processing.writeTo
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import javax.lang.model.element.Modifier

/**
 * Source generator to support Hilt injection of Workers.
 *
 * Should generate:
 * ```
 * @Module
 * @InstallIn(SingletonComponent.class)
 * public interface $_HiltModule {
 *   @Binds
 *   @IntoMap
 *   @StringKey("pkg.$")
 *   ViewModelAssistedFactory<? extends Worker> bind($_AssistedFactory factory)
 * }
 * ```
 * and
 * ```
 * @AssistedFactory
 * public interface $_AssistedFactory extends WorkerAssistedFactory<$> {
 *
 * }
 * ```
 */
internal class WorkerGenerator(
    private val processingEnv: XProcessingEnv,
    private val injectedWorker: WorkerElement
) {
    fun generate() {
        val assistedFactoryTypeSpec = TypeSpec.interfaceBuilder(injectedWorker.factoryClassName)
            .addOriginatingElement(injectedWorker.typeElement)
            .addGeneratedAnnotation(processingEnv)
            .addAnnotation(ClassNames.ASSISTED_FACTORY)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(injectedWorker.factorySuperTypeName)
            .build()
        JavaFile.builder(injectedWorker.factoryClassName.packageName(), assistedFactoryTypeSpec)
            .build()
            .writeTo(processingEnv.filer)

        val hiltModuleTypeSpec = TypeSpec.interfaceBuilder(injectedWorker.moduleClassName)
            .addOriginatingElement(injectedWorker.typeElement)
            .addGeneratedAnnotation(processingEnv)
            .addAnnotation(ClassNames.MODULE)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.INSTALL_IN)
                    .addMember("value", "$T.class", ClassNames.SINGLETON_COMPONENT)
                    .build()
            )
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.ORIGINATING_ELEMENT)
                    .addMember(
                        "topLevelClass",
                        "$T.class",
                        injectedWorker.className.topLevelClassName()
                    )
                    .build()
            )
            .addModifiers(Modifier.PUBLIC)
            .addMethod(
                MethodSpec.methodBuilder("bind")
                    .addAnnotation(ClassNames.BINDS)
                    .addAnnotation(ClassNames.INTO_MAP)
                    .addAnnotation(
                        AnnotationSpec.builder(ClassNames.STRING_KEY)
                            .addMember("value", S, injectedWorker.className.reflectionName())
                            .build()
                    )
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassNames.WORKER_ASSISTED_FACTORY,
                            WildcardTypeName.subtypeOf(ClassNames.LISTENABLE_WORKER)
                        )
                    )
                    .addParameter(injectedWorker.factoryClassName, "factory")
                    .build()
            )
            .build()
        JavaFile.builder(injectedWorker.moduleClassName.packageName(), hiltModuleTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }
}
