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

package androidx.hilt.assisted

import androidx.hilt.ClassNames
import androidx.hilt.ext.L
import androidx.hilt.ext.T
import androidx.hilt.ext.W
import androidx.hilt.ext.addGeneratedAnnotation
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

/**
 * Source generator for assisted factories.
 */
internal class AssistedFactoryGenerator(
    private val processingEnv: ProcessingEnvironment,
    private val productClassName: ClassName,
    private val factoryClassName: ClassName,
    private val factorySuperTypeName: ParameterizedTypeName,
    private val originatingElement: TypeElement,
    private val dependencyRequests: List<DependencyRequest>
) {

    fun generate() {
        val factoryTypeSpec = TypeSpec.classBuilder(factoryClassName)
            .addOriginatingElement(originatingElement)
            .addSuperinterface(factorySuperTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addGeneratedAnnotation(processingEnv.elementUtils, processingEnv.sourceVersion)
            .addFields(getFieldSpecs())
            .addMethod(getConstructorMethodSpec())
            .addMethod(getCreateMethodSpec())
            .build()
        JavaFile.builder(factoryClassName.packageName(), factoryTypeSpec)
            .build()
            .writeTo(processingEnv.filer)
    }

    private fun getFieldSpecs() = dependencyRequests
        .filterNot { it.isAssisted }
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
                dependencyRequests
                    .filterNot { it.isAssisted }
                    .forEach { dependencyRequest ->
                        addParameter(dependencyRequest.providerTypeName, dependencyRequest.name)
                        addStatement("this.$1N = $1N", dependencyRequest.name)
                    }
            }
            .build()

    private fun getCreateMethodSpec(): MethodSpec {
        val factoryTypeElement =
            processingEnv.elementUtils.getTypeElement(factorySuperTypeName.rawType.canonicalName())
        val factoryMethod = ElementFilter.methodsIn(factoryTypeElement.enclosedElements).first()
        val parameterSpecs = factoryMethod.parameters.map { ParameterSpec.get(it) }
        val constructorArgs = dependencyRequests.map {
            val paramLiteral = when {
                it.isAssisted -> {
                    factoryMethod.parameters.first { param ->
                        TypeName.get(param.asType()) == it.type
                    }.simpleName.toString()
                }
                it.isProvider -> it.name
                else -> "${it.name}.get()"
            }
            CodeBlock.of(L, paramLiteral)
        }
        return MethodSpec.methodBuilder(factoryMethod.simpleName.toString())
            .addAnnotation(Override::class.java)
            .addAnnotation(ClassNames.NON_NULL)
            .addModifiers(Modifier.PUBLIC)
            .returns(productClassName)
            .addParameters(parameterSpecs)
            .addStatement("return new $T($L)",
                productClassName, CodeBlock.join(constructorArgs, ",$W"))
            .build()
    }
}