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

package androidx.privacysandbox.tools.apicompiler.generator

import androidx.privacysandbox.tools.core.generator.AidlGenerator
import androidx.privacysandbox.tools.core.generator.SpecNames
import androidx.privacysandbox.tools.core.generator.addCode
import androidx.privacysandbox.tools.core.generator.addControlFlow
import androidx.privacysandbox.tools.core.generator.aidlName
import androidx.privacysandbox.tools.core.generator.build
import androidx.privacysandbox.tools.core.generator.poetSpec
import androidx.privacysandbox.tools.core.generator.primaryConstructor
import androidx.privacysandbox.tools.core.generator.transactionCallbackName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class StubDelegatesGenerator(
    private val codeGenerator: CodeGenerator,
    private val api: ParsedApi,
) {
    companion object {
        private val ATOMIC_BOOLEAN_CLASS = ClassName("java.util.concurrent.atomic", "AtomicBoolean")
    }

    fun generate() {
        if (api.services.isEmpty()) {
            return
        }
        api.services.forEach(::generateServiceStubDelegate)
        generateTransportCancellationCallback()
    }

    private fun generateServiceStubDelegate(service: AnnotatedInterface) {
        val className = service.stubDelegateName()
        val aidlBaseClassName = ClassName(service.type.packageName, service.aidlName(), "Stub")

        val classSpec = TypeSpec.classBuilder(className).build {
            superclass(aidlBaseClassName)

            primaryConstructor(
                listOf(
                    PropertySpec.builder(
                        "delegate",
                        service.type.poetSpec(),
                    ).addModifiers(KModifier.PRIVATE).build()
                ), KModifier.INTERNAL
            )

            addFunctions(service.methods.map(::toFunSpec))
        }

        val fileSpec = FileSpec.builder(service.type.packageName, className).build {
            addType(classSpec)
        }
        codeGenerator.createNewFile(
            Dependencies(false), service.type.packageName, className
        ).write(fileSpec)
    }

    private fun toFunSpec(method: Method): FunSpec {
        if (method.isSuspend) return toSuspendFunSpec(method)
        return toNonSuspendFunSpec(method)
    }

    private fun toSuspendFunSpec(method: Method): FunSpec {
        val resultStatement =
            "delegate.${method.name}(${method.parameters.joinToString(", ") { it.name }})"
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
                        addStatement("val result = $resultStatement")
                        addStatement("transactionCallback.onSuccess(result)")
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
        val returnStatement =
            "return delegate.${method.name}(${method.parameters.joinToString(", ") { it.name }})"
        addModifiers(KModifier.OVERRIDE)
        addParameters(getParameters(method))
        returns(method.returnType.poetSpec())
        addStatement(returnStatement).build()
    }

    private fun getParameters(method: Method) = buildList {
        addAll(method.parameters.map { parameter ->
            ParameterSpec(parameter.name, parameter.type.poetSpec())
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

    private fun generateTransportCancellationCallback() {
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

        val fileSpec = FileSpec.builder(packageName, className).addType(classSpec).build()
        codeGenerator.createNewFile(Dependencies(false), packageName, className).write(fileSpec)
    }
}

internal fun AnnotatedInterface.stubDelegateName() = "${type.simpleName}StubDelegate"