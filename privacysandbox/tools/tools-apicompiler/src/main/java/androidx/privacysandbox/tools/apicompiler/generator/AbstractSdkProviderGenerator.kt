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

import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.poetSpec
import androidx.privacysandbox.tools.core.generator.stubDelegateNameSpec
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec

class AbstractSdkProviderGenerator(private val api: ParsedApi) {
    companion object {
        private val sandboxedSdkProviderClass =
            ClassName("androidx.privacysandbox.sdkruntime.core", "SandboxedSdkProviderCompat")
        private val sandboxedSdkClass =
            ClassName("androidx.privacysandbox.sdkruntime.core", "SandboxedSdkCompat")
        private val sandboxedSdkCreateMethod =
            MemberName(
                ClassName(
                    sandboxedSdkClass.packageName,
                    sandboxedSdkClass.simpleName,
                    "Companion"
                ), "create"
            )
        private val contextClass = ClassName("android.content", "Context")
        private val bundleClass = ClassName("android.os", "Bundle")
        private val viewClass = ClassName("android.view", "View")
    }

    fun generate(): FileSpec? {
        if (api.services.isEmpty()) {
            return null
        }
        val packageName = api.getOnlyService().type.packageName
        val className = "AbstractSandboxedSdkProvider"
        val classSpec =
            TypeSpec.classBuilder(className)
                .superclass(sandboxedSdkProviderClass)
                .addModifiers(KModifier.ABSTRACT)
                .addFunction(generateOnLoadSdkFunction())
                .addFunction(generateGetViewFunction())
                .addFunction(generateCreateServiceFunction(api.getOnlyService()))

        return FileSpec.builder(packageName, className)
            .addType(classSpec.build())
            .build()
    }

    private fun generateOnLoadSdkFunction(): FunSpec {
        return FunSpec.builder("onLoadSdk").build {
            addModifiers(KModifier.OVERRIDE)
            addParameter("params", bundleClass)
            returns(sandboxedSdkClass)
            addStatement(
                "val sdk = ${getCreateServiceFunctionName(api.getOnlyService())}(context!!)"
            )
            addStatement(
                "return %M(%T(sdk))",
                sandboxedSdkCreateMethod,
                api.getOnlyService().stubDelegateNameSpec()
            )
        }
    }

    private fun generateGetViewFunction(): FunSpec {
        return FunSpec.builder("getView").build {
            addModifiers(KModifier.OVERRIDE)
            addParameter("windowContext", contextClass)
            addParameter("params", bundleClass)
            addParameter("width", Int::class)
            addParameter("height", Int::class)
            returns(viewClass)
            addStatement("TODO(\"Implement\")")
        }
    }

    private fun generateCreateServiceFunction(service: AnnotatedInterface): FunSpec {
        return FunSpec.builder(getCreateServiceFunctionName(service))
            .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
            .addParameter("context", contextClass)
            .returns(service.type.poetSpec())
            .build()
    }

    private fun getCreateServiceFunctionName(service: AnnotatedInterface) =
        "create${service.type.simpleName}"
}