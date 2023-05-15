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

import androidx.privacysandbox.tools.core.generator.AidlGenerator.Companion.throwableParcelName
import androidx.privacysandbox.tools.core.generator.GenerationTarget.SERVER
import androidx.privacysandbox.tools.core.generator.SpecNames.contextClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.resumeWithExceptionMethod
import androidx.privacysandbox.tools.core.generator.SpecNames.suspendCancellableCoroutineMethod
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Types
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode

class ClientProxyTypeGenerator(
    private val basePackageName: String,
    private val binderCodeConverter: BinderCodeConverter
) {
    private val cancellationSignalClassName = ClassName(basePackageName, "ICancellationSignal")
    private val sandboxedUiAdapterPropertyName = "sandboxedUiAdapter"
    private val sandboxedUiAdapterFactoryClass =
        ClassName("androidx.privacysandbox.ui.client", "SandboxedUiAdapterFactory")

    /**
     * Generates a ClientProxy for this interface.
     *
     * This allows a client to call remote methods on a server using a binder named 'remote'.
     *
     * If  [target] is [GenerationTarget.SERVER] (ie. this will run on the SDK-side) includes a
     * Context that will be the SDK context.
     */
    fun generate(annotatedInterface: AnnotatedInterface, target: GenerationTarget): FileSpec {
        val className = annotatedInterface.clientProxyNameSpec().simpleName
        val remoteBinderClassName = annotatedInterface.aidlType().innerType.poetTypeName()
        val inheritsUiAdapter = annotatedInterface.superTypes.contains(Types.sandboxedUiAdapter)

        val classSpec = TypeSpec.classBuilder(className).build {
            addSuperinterface(annotatedInterface.type.poetTypeName())

            primaryConstructor(buildList {
                add(
                    PropertySpec.builder("remote", remoteBinderClassName)
                        .addModifiers(KModifier.PUBLIC).build()
                )
                if (target == SERVER) {
                    add(
                        PropertySpec.builder(contextPropertyName, contextClass)
                            .addModifiers(KModifier.PUBLIC).build()
                    )
                }
                if (inheritsUiAdapter) add(
                    PropertySpec.builder("coreLibInfo", SpecNames.bundleClass)
                        .addModifiers(KModifier.PUBLIC).build()
                )
            })

            addFunctions(annotatedInterface.methods.map(::toFunSpec))

            if (inheritsUiAdapter) {
                addProperty(
                    PropertySpec.builder(
                        sandboxedUiAdapterPropertyName, Types.sandboxedUiAdapter.poetTypeName()
                    ).addModifiers(KModifier.PUBLIC)
                        .initializer(
                            "%T.createFromCoreLibInfo(coreLibInfo)",
                            sandboxedUiAdapterFactoryClass
                        )
                        .build()
                )
                addFunction(generateOpenSession())
            }
        }

        return FileSpec.builder(annotatedInterface.type.packageName, className).build {
            addCommonSettings()
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
            returns(method.returnType.poetTypeName())

            addCode {
                addControlFlow("return %M", suspendCancellableCoroutineMethod) {
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

    private fun generateOpenSession() = FunSpec.builder("openSession").build {
        addModifiers(KModifier.OVERRIDE)
        addParameters(
            listOf(
                ParameterSpec(contextPropertyName, contextClass),
                ParameterSpec("initialWidth", Types.int.poetClassName()),
                ParameterSpec("initialHeight", Types.int.poetClassName()),
                ParameterSpec("isZOrderOnTop", Types.boolean.poetClassName()),
                ParameterSpec("clientExecutor", ClassName("java.util.concurrent", "Executor")),
                ParameterSpec(
                    "client", ClassName(
                        "androidx.privacysandbox.ui.core", "SandboxedUiAdapter.SessionClient"
                    )
                ),
            )
        )
        addStatement(
            "$sandboxedUiAdapterPropertyName.openSession(%N, initialWidth, initialHeight, " +
                "isZOrderOnTop, clientExecutor, client)",
            contextPropertyName,
        )
    }

    private fun generateTransactionCallbackObject(method: Method) = CodeBlock.builder().build {
        val transactionCallbackClassName = ClassName(
            basePackageName,
            wrapWithListIfNeeded(method.returnType).transactionCallbackName(),
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

            addControlFlow(
                "override fun onFailure(throwableParcel: %T)",
                ClassName(basePackageName, throwableParcelName)
            ) {
                addStatement(
                    "it.%M(%M(throwableParcel))",
                    resumeWithExceptionMethod,
                    ThrowableParcelConverterFileGenerator.fromThrowableParcelNameSpec(
                        basePackageName
                    )
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
                    binderCodeConverter.convertToModelCode(method.returnType, "result")
                )
            }
        }
    }

    private fun generateRemoteCall(
        method: Method,
        extraParameters: List<CodeBlock> = emptyList(),
    ) = CodeBlock.builder().build {
        val parameters =
            method.parameters.map { binderCodeConverter.convertToBinderCode(it.type, it.name) } +
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