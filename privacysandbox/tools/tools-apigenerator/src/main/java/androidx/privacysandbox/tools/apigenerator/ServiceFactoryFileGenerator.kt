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

import androidx.privacysandbox.tools.core.generator.SpecNames.iBinderClassName
import androidx.privacysandbox.tools.core.generator.addCommonSettings
import androidx.privacysandbox.tools.core.generator.aidlInterfaceNameSpec
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.clientProxyNameSpec
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec

internal class ServiceFactoryFileGenerator {

    fun generate(service: AnnotatedInterface): FileSpec =
        FileSpec.builder(service.type.packageName, "${service.type.simpleName}Factory").build {
            addCommonSettings()
            addFunction(generateFactoryFunction(service))
        }

    private fun generateFactoryFunction(service: AnnotatedInterface) =
        FunSpec.builder("wrapTo${service.type.simpleName}").build {
            addParameter(ParameterSpec("binder", iBinderClassName))
            returns(ClassName(service.type.packageName, service.type.simpleName))
            addStatement(
                "return %T(%T.Stub.asInterface(binder))",
                service.clientProxyNameSpec(), service.aidlInterfaceNameSpec()
            )
        }
}
