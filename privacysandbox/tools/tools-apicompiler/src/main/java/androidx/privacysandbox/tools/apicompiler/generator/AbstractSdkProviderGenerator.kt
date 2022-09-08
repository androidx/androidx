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

package androidx.privacysandbox.tools.apicompiler.generator

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.ParsedApi
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.OutputStream

class AbstractSdkProviderGenerator(
    private val codeGenerator: CodeGenerator,
    private val api: ParsedApi,
) {
    companion object {
        private val SANDBOXED_SDK_PROVIDER_CLASS =
            ClassName("android.app.sdksandbox", "SandboxedSdkProvider")
        private val SANDBOXED_SDK_CLASS =
            ClassName("android.app.sdksandbox", "SandboxedSdk")
        private val SANDBOXED_SDK_CONTEXT_CLASS =
            ClassName("android.app.sdksandbox", "SandboxedSdkContext")
        private val CONTEXT_CLASS = ClassName("android.content", "Context")
        private val BUNDLE_CLASS = ClassName("android.os", "Bundle")
        private val VIEW_CLASS = ClassName("android.view", "View")
    }

    fun generate() {
        val packageName = api.services.first().packageName
        val className = "AbstractSandboxedSdkProvider"
        val classSpec =
            TypeSpec.classBuilder(className)
                .superclass(SANDBOXED_SDK_PROVIDER_CLASS)
                .addModifiers(KModifier.ABSTRACT)
                .addFunction(generateOnLoadSdkFunction())
                .addFunction(generateGetViewFunction())
                .addFunction(generateCreateServiceFunction(api.services.first()))

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(classSpec.build())
            .build()
        codeGenerator.createNewFile(Dependencies(false), packageName, className).write(fileSpec)
    }

    private fun OutputStream.write(spec: FileSpec) = bufferedWriter().use(spec::writeTo)

    private fun generateOnLoadSdkFunction(): FunSpec {
        return FunSpec.builder("onLoadSdk")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("params", BUNDLE_CLASS)
            .returns(SANDBOXED_SDK_CLASS)
            .addStatement("TODO(\"Implement\")")
            .build()
    }

    private fun generateGetViewFunction(): FunSpec {
        return FunSpec.builder("getView")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("windowContext", CONTEXT_CLASS)
            .addParameter("params", BUNDLE_CLASS)
            .addParameter("width", Int::class)
            .addParameter("height", Int::class)
            .returns(VIEW_CLASS)
            .addStatement("TODO(\"Implement\")")
            .build()
    }

    private fun generateCreateServiceFunction(service: AnnotatedInterface): FunSpec {
        return FunSpec.builder("create${service.name}")
            .addModifiers(KModifier.ABSTRACT)
            .addParameter("sdkContext", SANDBOXED_SDK_CONTEXT_CLASS)
            .returns(service.toPoetClassName())
            .build()
    }

    private fun AnnotatedInterface.toPoetClassName() = ClassName(packageName, name)
}