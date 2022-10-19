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
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.joinToCode

class StubDelegatesGenerator(private val api: ParsedApi) {
    private val binderCodeConverter = BinderCodeConverter(api)

    companion object {
        private val ATOMIC_BOOLEAN_CLASS = ClassName("java.util.concurrent.atomic", "AtomicBoolean")
    }

    fun generate(): List<FileSpec> {
        if (api.services.isEmpty()) {
            return emptyList()
        }
        return api.services.map(::generateInterfaceStubDelegate) +
            api.interfaces.map(::generateInterfaceStubDelegate) +
            generateTransportCancellationCallback()
    }

    fun generateInterfaceStubDelegate(annotatedInterface: AnnotatedInterface): FileSpec {
        val className = annotatedInterface.stubDelegateNameSpec().simpleName
        val aidlBaseClassName =
            ClassName(annotatedInterface.type.packageName, annotatedInterface.aidlName(), "Stub")

        val classSpec = TypeSpec.classBuilder(className).build {
            superclass(aidlBaseClassName)

            primaryConstructor(
                listOf(
                    PropertySpec.builder(
                        "delegate",
                        annotatedInterface.type.poetSpec(),
                    ).addModifiers(KModifier.PUBLIC).build()
                ), KModifier.INTERNAL
            )

            addFunctions(annotatedInterface.methods.map(::toFunSpec))
        }

        return FileSpec.builder(annotatedInterface.type.packageName, className).build {
            addType(classSpec)
        }
    }

    private fun toFunSpec(method: Method): FunSpec {
        if (method.isSuspend) return toSuspendFunSpec(method)
        return toNonSuspendFunSpec(method)
    }

    private fun toSuspendFunSpec(method: Method): FunSpec {
        return FunSpec.builder(method.name).build {
            addModifiers(KModifier.OVERRIDE)
            addParameters(getParameters(method))
            addCode {
                addControlFlow(
                    "val job = %T.%M(%T)",
                    SpecNames.globalScopeClass,
                    SpecNames.launchMethod,
                    SpecNames.dispatchersMainClass
                ) {
                    addControlFlow("try") {
                        addStatement {
                            add("val result = ")
                            add(getDelegateCallBlock(method))
                        }
                        if (method.returnType == Types.unit) {
                            addStatement("transactionCallback.onSuccess()")
                        } else {
                            addStatement(
                                "transactionCallback.onSuccess(%L)",
                                binderCodeConverter.convertToBinderCode(method.returnType, "result")
                            )
                        }
                    }
                    addControlFlow("catch (t: Throwable)") {
                        addStatement("transactionCallback.onFailure(404, t.message)")
                    }
                }
                addStatement(
                    "val cancellationSignal = TransportCancellationCallback() { job.cancel() }"
                )
                addStatement("transactionCallback.onCancellable(cancellationSignal)")
            }
        }
    }

    private fun toNonSuspendFunSpec(method: Method) = FunSpec.builder(method.name).build {
        addModifiers(KModifier.OVERRIDE)
        addParameters(getParameters(method))
        addStatement { add(getDelegateCallBlock(method)) }
    }

    private fun getParameters(method: Method) = buildList {
        addAll(method.parameters.map { parameter ->
            ParameterSpec(
                parameter.name,
                binderCodeConverter.convertToBinderType(parameter.type)
            )
        })
        if (method.isSuspend) add(
            ParameterSpec(
                "transactionCallback", ClassName(
                    api.getOnlyService().type.packageName,
                    method.returnType.transactionCallbackName()
                )
            )
        )
    }

    private fun getDelegateCallBlock(method: Method) = CodeBlock.builder().build {
        add("delegate.${method.name}(")
        add(method.parameters.map { binderCodeConverter.convertToModelCode(it) }.joinToCode())
        add(")")
    }

    private fun generateTransportCancellationCallback(): FileSpec {
        val packageName = api.getOnlyService().type.packageName
        val className = "TransportCancellationCallback"
        val cancellationSignalStubName =
            ClassName(packageName, AidlGenerator.cancellationSignalName, "Stub")

        val classSpec = TypeSpec.classBuilder(className).build {
            superclass(cancellationSignalStubName)
            addModifiers(KModifier.INTERNAL)
            primaryConstructor(
                listOf(
                    PropertySpec.builder(
                        "onCancel",
                        LambdaTypeName.get(returnType = Unit::class.asTypeName()),
                    ).addModifiers(KModifier.PRIVATE).build()
                ), KModifier.INTERNAL
            )
            addProperty(
                PropertySpec.builder(
                    "hasCancelled", ATOMIC_BOOLEAN_CLASS, KModifier.PRIVATE
                ).initializer("%T(false)", ATOMIC_BOOLEAN_CLASS).build()
            )
            addFunction(FunSpec.builder("cancel").build {
                addModifiers(KModifier.OVERRIDE)
                addCode {
                    addControlFlow("if (hasCancelled.compareAndSet(false, true))") {
                        addStatement("onCancel()")
                    }
                }
            })
        }

        return FileSpec.builder(packageName, className).addType(classSpec).build()
    }
}
