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

import androidx.privacysandbox.tools.core.generator.SpecNames.bundleClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.viewClass
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.poetTypeName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

/** Generates an SDK provider that delegates calls to SDK defined classes. */
internal abstract class AbstractSdkProviderGenerator(protected val api: ParsedApi) {

    fun generate(): FileSpec? {
        if (api.services.isEmpty()) {
            return null
        }
        val packageName = api.getOnlyService().type.packageName
        val className = "AbstractSandboxedSdkProviderCompat"
        val classSpec =
            TypeSpec.classBuilder(className)
                .superclass(superclassName)
                .addModifiers(KModifier.ABSTRACT)
                .addFunction(generateOnLoadSdkFunction())
                .addFunction(generateGetViewFunction())
                .addFunction(generateCreateServiceFunction(api.getOnlyService()))

        return FileSpec.builder(packageName, className)
            .addType(classSpec.build())
            .build()
    }

    abstract val superclassName: ClassName
    abstract fun generateOnLoadSdkFunction(): FunSpec

    protected fun createServiceFunctionName(service: AnnotatedInterface) =
        "create${service.type.simpleName}"

    private fun generateGetViewFunction(): FunSpec {
        return FunSpec.builder("getView").build {
            addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            addParameter("windowContext", contextClass)
            addParameter("params", bundleClass)
            addParameter("width", Int::class)
            addParameter("height", Int::class)
            returns(viewClass)
            addStatement(
                "throw UnsupportedOperationException(%S)",
                "This SDK doesn't support explicit SurfaceView requests."
            )
        }
    }

    private fun generateCreateServiceFunction(service: AnnotatedInterface): FunSpec {
        return FunSpec.builder(createServiceFunctionName(service))
            .addModifiers(KModifier.ABSTRACT, KModifier.PROTECTED)
            .addParameter(contextPropertyName, contextClass)
            .returns(service.type.poetTypeName())
            .build()
    }
}
