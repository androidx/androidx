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
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.ParsedApi
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

class ClientProxyTypeGenerator(
    private val api: ParsedApi,
    private val service: AnnotatedInterface
) {
    val className =
        ClassName(service.type.packageName, "${service.type.simpleName}ClientProxy")
    val remoteBinderClassName =
        ClassName(service.type.packageName, "I${service.type.simpleName}")
    private val cancellationSignalClassName =
        ClassName(service.type.packageName, "ICancellationSignal")

    fun generate(): TypeSpec = TypeSpec.classBuilder(className).build {
        addModifiers(KModifier.PRIVATE)
        addSuperinterface(ClassName(service.type.packageName, service.type.simpleName))
        primaryConstructor(
            listOf(
                PropertySpec.builder("remote", remoteBinderClassName)
                    .addModifiers(KModifier.PRIVATE).build()
            )
        )
        addFunctions(service.methods.map { method ->
            if (method.isSuspend) {
                generateSuspendProxyMethodImplementation(method)
            } else {
                generateProxyMethodImplementation(method)
            }
        })
    }

    private fun generateProxyMethodImplementation(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.OVERRIDE)
            addParameters(method.parameters.map { it.poetSpec() })

            addCode(generateRemoteCall(method))
        }

    private fun generateSuspendProxyMethodImplementation(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.OVERRIDE)
            addModifiers(KModifier.SUSPEND)
            addParameters(method.parameters.map { it.poetSpec() })
            returns(method.returnType.poetSpec())

            addCode {
                addControlFlow("return suspendCancellableCoroutine") {
                    addStatement("var mCancellationSignal: %T? = null", cancellationSignalClassName)

                    add(generateTransactionCallbackObject(method))
                    add(generateRemoteCall(method, listOf(CodeBlock.of("transactionCallback"))))

                    addControlFlow("it.invokeOnCancellation") {
                        addStatement("mCancellationSignal?.cancel()")
                    }
                }
            }
        }

    private fun generateTransactionCallbackObject(method: Method) = CodeBlock.builder().build {
        val transactionCallbackClassName = ClassName(
            service.type.packageName,
            method.returnType.transactionCallbackName(),
            "Stub"
        )

        addControlFlow("val transactionCallback = object: %T()", transactionCallbackClassName) {
            addControlFlow(
                "override fun onCancellable(cancellationSignal: %T)",
                cancellationSignalClassName
            ) {
                addControlFlow("if (it.isCancelled)") {
                    addStatement("cancellationSignal.cancel()")
                }
                addStatement("mCancellationSignal = cancellationSignal")
            }

            add(generateTransactionCallbackOnSuccess(method))

            addControlFlow("override fun onFailure(errorCode: Int, errorMessage: String)") {
                addStatement(
                    "it.%M(RuntimeException(errorMessage))",
                    MemberName("kotlin.coroutines", "resumeWithException")
                )
            }
        }
    }

    private fun generateTransactionCallbackOnSuccess(method: Method): CodeBlock {
        if (method.returnsUnit) {
            return CodeBlock.builder().build {
                addControlFlow("override fun onSuccess()") {
                    addStatement("it.resumeWith(Result.success(Unit))")
                }
            }
        }

        val value = api.valueMap[method.returnType]
        if (value != null) {
            return CodeBlock.builder().build {
                addControlFlow(
                    "override fun onSuccess(result: %T)",
                    value.parcelableNameSpec()
                ) {
                    addStatement("it.resumeWith(Result.success(%M(result)))",
                        value.fromParcelableNameSpec())
                }
            }
        }

        return CodeBlock.builder().build {
            addControlFlow(
                "override fun onSuccess(result: %T)",
                method.returnType.poetSpec()
            ) {
                addStatement("it.resumeWith(Result.success(result))")
            }
        }
    }

    private fun generateRemoteCall(
        method: Method,
        extraParameters: List<CodeBlock> = emptyList(),
    ) = CodeBlock.builder().build {
        val parameters = method.parameters.map { parameter ->
            api.valueMap[parameter.type]?.toParcelableNameSpec()?.let { converterFunctionName ->
                CodeBlock.of("%M(${parameter.name})", converterFunctionName)
            } ?: CodeBlock.of(parameter.name)
        } + extraParameters
        addStatement {
            add("remote.${method.name}(")
            add(parameters.joinToCode())
            add(")")
        }
    }

    private val Method.returnsUnit: Boolean
        get() {
            return returnType.qualifiedName == Unit::class.qualifiedName
        }
}