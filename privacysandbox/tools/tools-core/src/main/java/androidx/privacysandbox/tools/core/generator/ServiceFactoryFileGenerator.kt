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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.internal.GeneratedPublicApi
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec

class ServiceFactoryFileGenerator(private val generateStubs: Boolean = false) {

    fun generate(service: AnnotatedInterface): FileSpec =
        FileSpec.builder(service.type.packageName, factoryName(service)).build {
            addCommonSettings()
            addType(generateFactory(service))
        }

    private fun generateFactory(service: AnnotatedInterface) =
        TypeSpec.objectBuilder(factoryName(service)).build {
            if (generateStubs) {
                addAnnotation(GeneratedPublicApi::class)
            }
            addFunction(generateFactoryFunction(service))
        }

    private fun generateFactoryFunction(service: AnnotatedInterface) =
        FunSpec.builder("wrapTo${service.type.simpleName}").build {
            addParameter(ParameterSpec("binder", SpecNames.iBinderClass))
            returns(ClassName(service.type.packageName, service.type.simpleName))
            if (generateStubs) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class).addMember("%S", "UNUSED_PARAMETER")
                        .build()
                )
                addStatement("throw RuntimeException(%S)", "Stub!")
            } else {
                addStatement(
                    "return %T(%T.Stub.asInterface(binder))",
                    service.clientProxyNameSpec(), service.aidlInterfaceNameSpec()
                )
            }
        }

    private fun factoryName(service: AnnotatedInterface) = "${service.type.simpleName}Factory"
}
