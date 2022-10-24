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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

class ClientProxyTypeGenerator(
    private val basePackageName: String,
    private val binderCodeConverter: BinderCodeConverter
) {
    private val cancellationSignalClassName =
        ClassName(basePackageName, "ICancellationSignal")

    fun generate(annotatedInterface: AnnotatedInterface): FileSpec {
        val className = annotatedInterface.clientProxyNameSpec().simpleName
        val remoteBinderClassName = annotatedInterface.aidlType().innerType.poetSpec()

        val classSpec = TypeSpec.classBuilder(className).build {
            addSuperinterface(annotatedInterface.type.poetSpec())

            primaryConstructor(
                listOf(
                    PropertySpec.builder("remote", remoteBinderClassName)
                        .addModifiers(KModifier.PUBLIC).build()
                )
            )

            addFunctions(annotatedInterface.methods.map(::toFunSpec))
        }

        return FileSpec.builder(annotatedInterface.type.packageName, className).build {
            addCommonSettings()

            // TODO(b/254660742): Only add these when needed
            addImport("kotlinx.coroutines", "suspendCancellableCoroutine")
            addImport("kotlin.coroutines", "resume")
            addImport("kotlin.coroutines", "resumeWithException")

            addType(classSpec)
        }
    }

    private fun toFunSpec(method: Method): FunSpec {
        if (method.isSuspend) return toSuspendFunSpec(method)
        return toNonSuspendFunSpec(method)
    }

    private fun toSuspendFunSpec(method: Method) =
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

    private fun toNonSuspendFunSpec(method: Method) =
        FunSpec.builder(method.name).build {
            addModifiers(KModifier.OVERRIDE)
            addParameters(method.parameters.map { it.poetSpec() })

            addCode(generateRemoteCall(method))
        }

    private fun generateTransactionCallbackObject(method: Method) = CodeBlock.builder().build {
        val transactionCallbackClassName = ClassName(
            basePackageName,
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

        return CodeBlock.builder().build {
            addControlFlow(
                "override fun onSuccess(result: %T)",
                binderCodeConverter.convertToBinderType(method.returnType)
            ) {
                addStatement(
                    "it.resumeWith(Result.success(%L))",
                    binderCodeConverter.convertToModelCodeInClient(method.returnType, "result")
                )
            }
        }
    }

    private fun generateRemoteCall(
        method: Method,
        extraParameters: List<CodeBlock> = emptyList(),
    ) = CodeBlock.builder().build {
        val parameters =
            method.parameters.map { binderCodeConverter.convertToBinderCodeInClient(it) } +
                extraParameters
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