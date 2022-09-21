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
import androidx.privacysandbox.tools.core.generator.build
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec

internal class ServiceFactoryFileGenerator(private val service: AnnotatedInterface) {
    private val proxyTypeGenerator by lazy {
        ClientProxyTypeGenerator(service)
    }

    fun generate(): FileSpec =
        FileSpec.builder(service.packageName, "${service.name}Factory").build {
            addImport("kotlinx.coroutines", "suspendCancellableCoroutine")
            addImport("kotlin.coroutines", "resume")
            addImport("kotlin.coroutines", "resumeWithException")
            addKotlinDefaultImports(includeJvm = false, includeJs = false)

            addFunction(generateFactoryFunction())

            addType(proxyTypeGenerator.generate())
        }

    private fun generateFactoryFunction() = FunSpec.builder("create${service.name}").build {
        addModifiers(KModifier.SUSPEND)
        addParameter(ParameterSpec("context", AndroidClassNames.context))
        returns(ClassName(service.packageName, service.name))

        addCode(CodeBlock.builder().build {
            beginControlFlow("return suspendCancellableCoroutine")
            addStatement("val sdkSandboxManager = context.getSystemService(%T::class.java)",
                AndroidClassNames.sandboxManager)
            addNamed("""
                |sdkSandboxManager.loadSdk(
                |    %sdkPackageName:S,
                |    %bundle:T.EMPTY,
                |    { obj: Runnable -> obj.run() },
                |    object : %outcomeReceiver:T<%sandboxedSdk:T, %loadSdkException:T> {
                |        override fun onResult(result: %sandboxedSdk:T) {
                |            it.resume(%proxy:T(
                |                %aidlInterface:T.Stub.asInterface(result.getInterface())))
                |        }
                |
                |        override fun onError(error: %loadSdkException:T) {
                |            it.resumeWithException(error)
                |        }
                |    })
            """.trimMargin(),
                mapOf(
                    "sdkPackageName" to service.packageName,
                    "proxy" to proxyTypeGenerator.className,
                    "aidlInterface" to proxyTypeGenerator.remoteBinderClassName,
                    "bundle" to AndroidClassNames.bundle,
                    "outcomeReceiver" to AndroidClassNames.outcomeReceiver,
                    "sandboxedSdk" to AndroidClassNames.sandboxedSdk,
                    "loadSdkException" to AndroidClassNames.loadSdkException,
                ))
            endControlFlow()
        })
    }
}

private object AndroidClassNames {
    val context = ClassName("android.content", "Context")
    val bundle = ClassName("android.os", "Bundle")
    val outcomeReceiver = ClassName("android.os", "OutcomeReceiver")
    val sandboxManager =
        ClassName("android.app.sdksandbox", "SdkSandboxManager")
    val sandboxedSdk = ClassName("android.app.sdksandbox", "SandboxedSdk")
    val loadSdkException = ClassName("android.app.sdksandbox", "LoadSdkException")
}