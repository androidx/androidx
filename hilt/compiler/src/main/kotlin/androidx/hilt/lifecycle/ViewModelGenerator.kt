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
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier

/**
 * Source generator to support Hilt injection of ViewModels.
 *
 * Should generate:
 * ```
 * @Module
 * @InstallIn(ActivityRetainedComponent.class)
 * public interface $_HiltModule {
 *   @Binds
 *   @IntoMap
 *   @StringKey("pkg.$")
 *   ViewModelAssistedFactory<? extends ViewModel> bind($_AssistedFactory factory)
 * }
 * ```
 * and
 * ```
 * public final class $_AssistedFactory extends ViewModelAssistedFactory<$> {
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
 *   public $ create(@NonNull SavedStateHandle handle) {
 *     return new $(dep1.get(), dep2.get(), ..., handle);
 *   }
 * }
 * ```
 */
internal class ViewModelGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val injectedViewModel: ViewModelInjectElements
) {
    fun generate() {
        val factoryTypeSpec = TypeSpec.classBuilder(injectedViewModel.factoryClassName)
            .addOriginatingElement(injectedViewModel.typeElement)
            .addSuperinterface(injectedViewModel.factorySuperTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addFields(getFieldSpecs())
            .addMethod(getConstructorMethodSpec())
            .addMethod(getCreateMethodSpec())
            .build()
        JavaFile.builder(injectedViewModel.factoryClassName.packageName(), factoryTypeSpec)
            .build()
            .writeTo(processingEnv.filer)

        val hiltModuleTypeSpec = TypeSpec.interfaceBuilder(injectedViewModel.moduleClassName)
            .addOriginatingElement(injectedViewModel.typeElement)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addAnnotation(ClassNames.MODULE)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.INSTALL_IN)
                    .addMember("value", "$T.class", ClassNames.ACTIVITY_RETAINED_COMPONENT)
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .addMethod(
                MethodSpec.methodBuilder("bind")
                    .addAnnotation(ClassNames.BINDS)
                    .addAnnotation(ClassNames.INTO_MAP)
                    .addAnnotation(
                        AnnotationSpec.builder(ClassNames.STRING_KEY)
                            .addMember("value", S, injectedViewModel.className.canonicalName())
                            .build())
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassNames.VIEW_MODEL_ASSISTED_FACTORY,
                            WildcardTypeName.subtypeOf(ClassNames.VIEW_MODEL)))
                    .addParameter(injectedViewModel.factoryClassName, "factory")
                    .build())
            .build()
        JavaFile.builder(injectedViewModel.moduleClassName.packageName(), hiltModuleTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }

    private fun getFieldSpecs() = injectedViewModel.dependencyRequests
        .filterNot { it.isSavedStateHandle }
        .map { dependencyRequest ->
            val fieldTypeName = dependencyRequest.providerTypeName.withoutAnnotations()
            FieldSpec.builder(fieldTypeName, dependencyRequest.name)
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build()
        }

    private fun getConstructorMethodSpec() =
        MethodSpec.constructorBuilder()
            .addAnnotation(ClassNames.INJECT)
            .apply {
                injectedViewModel.dependencyRequests
                    .filterNot { it.isSavedStateHandle }
                    .forEach { dependencyRequest ->
                        addParameter(dependencyRequest.providerTypeName, dependencyRequest.name)
                        addStatement("this.$1N = $1N", dependencyRequest.name)
                }
            }
            .build()

    private fun getCreateMethodSpec(): MethodSpec {
        val constructorArgs = injectedViewModel.dependencyRequests.map { dependencyRequest ->
            val paramLiteral = when {
                dependencyRequest.isSavedStateHandle -> "handle"
                dependencyRequest.isProvider -> dependencyRequest.name
                else -> "${dependencyRequest.name}.get()"
            }
            CodeBlock.of(L, paramLiteral)
        }
        return MethodSpec.methodBuilder("create")
            .addAnnotation(Override::class.java)
            .addAnnotation(ClassNames.NON_NULL)
            .addModifiers(Modifier.PUBLIC)
            .returns(injectedViewModel.className)
            .addParameter(
                ParameterSpec.builder(ClassNames.SAVED_STATE_HANDLE, "handle")
                    .addAnnotation(ClassNames.NON_NULL)
                    .build())
            .addStatement("return new $T($L)",
                injectedViewModel.className, CodeBlock.join(constructorArgs, ",$W"))
            .build()
    }
}

internal val DependencyRequest.isSavedStateHandle: Boolean
    get() = type == ClassNames.SAVED_STATE_HANDLE && qualifier == null
