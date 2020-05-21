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
import androidx.hilt.assisted.AssistedFactoryGenerator
import androidx.hilt.ext.S
import androidx.hilt.ext.T
import androidx.hilt.ext.addGeneratedAnnotation
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

/**
 * Source generator to support Hilt injection of Workers.
 *
 * Should generate:
 * ```
 * @Module
 * @InstallIn(ApplicationComponent.class)
 * public interface $_HiltModule {
 *   @Binds
 *   @IntoMap
 *   @StringKey("pkg.$")
 *   ViewModelAssistedFactory<? extends Worker> bind($_AssistedFactory factory)
 * }
 * ```
 * and
 * ```
 * public final class $_AssistedFactory extends WorkerAssistedFactory<$> {
 *
 *   private final Provider<Dep1> dep1;
 *   private final Provider<Dep2> dep2;
 *   ...
 *
 *   @Inject
 *   $_AssistedFactory(Provider<Dep1> dep1, Provider<Dep2> dep2, ...) {
 *     this.dep1 = dep1;
 *     this.dep2 = dep2;
 *     ...
 *   }
 *
 *   @Override
 *   @NonNull
 *   public $ create(@NonNull Context context, @NonNull WorkerParameter params) {
 *     return new $(context, params, dep1.get(), dep2.get());
 *   }
 * }
 * ```
 */
internal class WorkerGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val injectedWorker: WorkerInjectElements
) {
    fun generate() {
        AssistedFactoryGenerator(
            processingEnv = processingEnv,
            productClassName = injectedWorker.className,
            factoryClassName = injectedWorker.factoryClassName,
            factorySuperTypeName = injectedWorker.factorySuperTypeName,
            originatingElement = injectedWorker.typeElement,
            dependencyRequests = injectedWorker.dependencyRequests
        ).generate()

        val hiltModuleTypeSpec = TypeSpec.interfaceBuilder(injectedWorker.moduleClassName)
            .addOriginatingElement(injectedWorker.typeElement)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addAnnotation(ClassNames.MODULE)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.INSTALL_IN)
                    .addMember("value", "$T.class", ClassNames.APPLICATION_COMPONENT)
                    .build())
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
                            .addMember("value", S, injectedWorker.className.canonicalName())
                            .build())
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassNames.WORKER_ASSISTED_FACTORY,
                            WildcardTypeName.subtypeOf(ClassNames.WORKER)))
                    .addParameter(injectedWorker.factoryClassName, "factory")
                    .build())
            .build()
        JavaFile.builder(injectedWorker.moduleClassName.packageName(), hiltModuleTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }
}
