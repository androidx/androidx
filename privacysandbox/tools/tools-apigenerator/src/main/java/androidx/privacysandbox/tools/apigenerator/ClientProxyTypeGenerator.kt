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

import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.poetSpec
import androidx.privacysandbox.tools.core.generator.primaryConstructor
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class ClientProxyTypeGenerator(private val service: AnnotatedInterface) {
    internal val className =
        ClassName(service.packageName, "${service.name}ClientProxy")
    internal val remoteBinderClassName =
        ClassName(service.packageName, "I${service.name}")
    private val cancellationSignalClassName =
        ClassName(service.packageName, "ICancellationSignal")

    fun generate(): TypeSpec = TypeSpec.classBuilder(className).build {
        addModifiers(KModifier.PRIVATE)
        addSuperinterface(ClassName(service.packageName, service.name))
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

            val parameterList = method.parameters.map(Parameter::name)
            if (method.returnsUnit) {
                addStatement(
                    "remote.${method.name}(${parameterList.joinToString()})"
                )
            } else {
                addStatement(
                    "return remote.${method.name}(${parameterList.joinToString()})"
                )
            }
        }

    private fun generateSuspendProxyMethodImplementation(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.OVERRIDE)
            addModifiers(KModifier.SUSPEND)
            addParameters(method.parameters.map { it.poetSpec() })
            returns(method.returnType.poetSpec())

            beginControlFlow("return suspendCancellableCoroutine")

            addStatement("var mCancellationSignal: %T? = null", cancellationSignalClassName)

            addCode(generateTransactionCallbackObject(method))

            val parameterList = buildList {
                addAll(method.parameters.map(Parameter::name))
                add("transactionCallback")
            }
            addStatement("remote.${method.name}(${parameterList.joinToString()})")

            beginControlFlow("it.invokeOnCancellation")
            addStatement("mCancellationSignal?.cancel()")
            endControlFlow()

            endControlFlow()
        }

    private fun generateTransactionCallbackObject(method: Method) = CodeBlock.builder().build {
        val transactionCallbackClassName = ClassName(
            service.packageName,
            "I${method.returnType.poetSpec().simpleName}TransactionCallback",
            "Stub"
        )
        beginControlFlow(
            "val transactionCallback = object: %T()",
            transactionCallbackClassName
        )

        beginControlFlow(
            "override fun onCancellable(cancellationSignal: %T)",
            cancellationSignalClassName
        )
        beginControlFlow("if (it.isCancelled)")
        addStatement("cancellationSignal.cancel()")
        endControlFlow()
        addStatement("mCancellationSignal = cancellationSignal")
        endControlFlow()

        if (method.returnsUnit) {
            beginControlFlow("override fun onSuccess()")
            addStatement("it.resumeWith(Result.success(Unit))")
            endControlFlow()
        } else {
            beginControlFlow(
                "override fun onSuccess(result: %T)",
                method.returnType.poetSpec()
            )
            addStatement("it.resumeWith(Result.success(result))")
            endControlFlow()
        }

        beginControlFlow("override fun onFailure(errorCode: Int, errorMessage: String)")
        addStatement(
            "it.%M(RuntimeException(errorMessage))",
            MemberName("kotlin.coroutines", "resumeWithException")
        )
        endControlFlow()

        endControlFlow()
    }

    private val Method.returnsUnit: Boolean
        get() {
            return returnType.name == Unit::class.qualifiedName
        }
}