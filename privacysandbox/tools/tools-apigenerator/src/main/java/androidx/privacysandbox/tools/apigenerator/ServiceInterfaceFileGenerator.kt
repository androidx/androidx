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

package androidx.privacysandbox.tools.apigenerator

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.poetSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

internal class ServiceInterfaceFileGenerator(private val service: AnnotatedInterface) {

    fun generate(): FileSpec {
        val annotatedInterface =
            TypeSpec.interfaceBuilder(ClassName(service.packageName, service.name)).build {
                addFunctions(service.methods.map(::generateInterfaceMethod))
            }

        return FileSpec.get(service.packageName, annotatedInterface).toBuilder().build {
            addKotlinDefaultImports(includeJvm = false, includeJs = false)
        }
    }

    private fun generateInterfaceMethod(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.ABSTRACT)
            addParameters(method.parameters.map { it.poetSpec() })
            returns(method.returnType.poetSpec())
        }
}