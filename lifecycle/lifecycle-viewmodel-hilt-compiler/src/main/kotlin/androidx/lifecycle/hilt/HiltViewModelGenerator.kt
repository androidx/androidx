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

package androidx.lifecycle.hilt

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
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
 *   @ViewModelKey($.class)
 *   ViewModelAssistedFactory<?> bind($_AssistedFactory f)
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
 *   @Overrides
 *   @NonNull
 *   public $ create(@NonNull SavedStateHandle handle) {
 *     return new $(dep1.get(), dep2.get(), ..., handle);
 *   }
 * }
 * ```
 */
internal class HiltViewModelGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val viewModelElements: HiltViewModelElements
) {
    fun generate() {
        val fieldsSpecs = getFieldSpecs(viewModelElements)
        val constructorSpec = getConstructorMethodSpec(fieldsSpecs)
        val createMethodSpec = getCreateMethodSpec(viewModelElements)
        val factoryTypeSpec = TypeSpec.classBuilder(viewModelElements.factoryClassName)
            .addOriginatingElement(viewModelElements.typeElement)
            .addSuperinterface(viewModelElements.factorySuperTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addFields(fieldsSpecs)
            .addMethod(constructorSpec)
            .addMethod(createMethodSpec)
            .build()
        JavaFile.builder(viewModelElements.factoryClassName.packageName(), factoryTypeSpec)
            .build()
            .writeTo(processingEnv.filer)

        val hiltModuleTypeSpec = TypeSpec.interfaceBuilder(viewModelElements.moduleClassName)
            .addOriginatingElement(viewModelElements.typeElement)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addAnnotation(ClassNames.MODULE)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.INSTALL_IN)
                    .addMember("value", "$T.class", ClassNames.ACTIVITY_RETAINED_COMPONENT)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("bind")
                    .addAnnotation(ClassNames.BINDS)
                    .addAnnotation(ClassNames.INTO_MAP)
                    .addAnnotation(
                        AnnotationSpec.builder(ClassNames.VIEW_MODEL_KEY)
                            .addMember("value", "$T.class", viewModelElements.className)
                            .build())
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassNames.VIEW_MODEL_ASSISTED_FACTORY,
                            WildcardTypeName.subtypeOf(ClassNames.VIEW_MODEL)))
                    .addParameter(viewModelElements.factoryClassName, "factory")
                    .build())
            .build()
        JavaFile.builder(viewModelElements.moduleClassName.packageName(), hiltModuleTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }

    private fun getFieldSpecs(viewModelElements: HiltViewModelElements) =
        viewModelElements.constructorElement.parameters.mapNotNull { parameter ->
            val paramTypeName = TypeName.get(parameter.asType())
            if (paramTypeName == ClassNames.SAVED_STATE_HANDLE) {
                // Skip SavedStateHandle since it is assisted injected.
                return@mapNotNull null
            }
            // TODO(danysantiago): Handle qualifiers
            // TODO(danysantiago): Don't wrap params that are already a Provider
            FieldSpec.builder(
                ParameterizedTypeName.get(ClassNames.PROVIDER, paramTypeName),
                "${parameter.simpleName}Provider",
                Modifier.PRIVATE, Modifier.FINAL)
                .build()
        }

    private fun getConstructorMethodSpec(fieldsSpecs: List<FieldSpec>) =
        MethodSpec.constructorBuilder()
            .addAnnotation(ClassNames.INJECT)
            .apply {
                fieldsSpecs.forEach { field ->
                    addParameter(field.type, field.name)
                    addStatement("this.$1N = $1N", field)
                }
            }
            .build()

    private fun getCreateMethodSpec(viewModelElements: HiltViewModelElements): MethodSpec {
        val constructorArgs = viewModelElements.constructorElement.parameters.map { param ->
            val paramTypeName = TypeName.get(param.asType())
            val paramLiteral = if (paramTypeName == ClassNames.SAVED_STATE_HANDLE) {
                "handle"
            } else {
                // TODO(danysantiago): Consider using the field specs?
                "${param.simpleName}Provider.get()"
            }
            CodeBlock.of(L, paramLiteral)
        }
        return MethodSpec.methodBuilder("create")
            .addAnnotation(Override::class.java)
            .addAnnotation(ClassNames.NON_NULL)
            .addModifiers(Modifier.PUBLIC)
            .returns(viewModelElements.className)
            .addParameter(
                ParameterSpec.builder(ClassNames.SAVED_STATE_HANDLE, "handle")
                    .addAnnotation(ClassNames.NON_NULL)
                    .build())
            .addStatement("return new $T($L)",
                viewModelElements.className, CodeBlock.join(constructorArgs, ",$W"))
            .build()
    }
}