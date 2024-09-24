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

import androidx.privacysandbox.tools.core.generator.addCommonSettings
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.generateConstant
import androidx.privacysandbox.tools.core.generator.poetClassName
import androidx.privacysandbox.tools.core.generator.poetSpec
import androidx.privacysandbox.tools.core.generator.poetTypeName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

internal class InterfaceFileGenerator {

    fun generate(annotatedInterface: AnnotatedInterface): FileSpec {
        val annotatedInterfaceType =
            TypeSpec.interfaceBuilder(annotatedInterface.type.poetClassName()).build {
                addSuperinterfaces(annotatedInterface.superTypes.map { it.poetClassName() })
                addFunctions(annotatedInterface.methods.map(::generateInterfaceMethod))
                if (annotatedInterface.constants.isNotEmpty()) {
                    addType(
                        TypeSpec.companionObjectBuilder().build {
                            addProperties(annotatedInterface.constants.map(::generateConstant))
                        }
                    )
                }
            }

        return FileSpec.get(annotatedInterface.type.packageName, annotatedInterfaceType)
            .toBuilder()
            .build { addCommonSettings() }
    }

    private fun generateInterfaceMethod(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.ABSTRACT)
            if (method.isSuspend) {
                addModifiers(KModifier.SUSPEND)
            }
            addParameters(method.parameters.map { it.poetSpec() })
            returns(method.returnType.poetTypeName())
        }
}
