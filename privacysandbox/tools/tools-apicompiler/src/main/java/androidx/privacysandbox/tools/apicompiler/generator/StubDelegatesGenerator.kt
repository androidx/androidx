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

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.generator.AidlGenerator
import androidx.privacysandbox.tools.core.generator.aidlName
import androidx.privacysandbox.tools.core.generator.transactionCallbackName
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
        api.services.forEach(::generateServiceStubDelegate)
        generateTransportCancellationCallback()
    }

    private fun generateServiceStubDelegate(service: AnnotatedInterface) {
        val className = service.stubDelegateName()
        val aidlBaseClassName = ClassName(service.packageName, service.aidlName(), "Stub")

        val classSpec =
            TypeSpec.classBuilder(className)
                .superclass(aidlBaseClassName)
                .primaryConstructor(
                    PropertySpec.builder(
                        "delegate",
                        service.specClassName(),
                    ).addModifiers(KModifier.PRIVATE).build()
                )
                .addFunctions(
                    service.methods.map(::toFunSpec)
                )

        val fileSpec = FileSpec.builder(service.packageName, className)
            .addType(classSpec.build())
            .apply {
                if (service.methods.any(Method::isSuspend)) {
                    addImport("kotlinx.coroutines", "CoroutineScope")
                    addImport("kotlinx.coroutines", "Dispatchers")
                    addImport("kotlinx.coroutines", "GlobalScope")
                    addImport("kotlinx.coroutines", "launch")
                }
            }
            .build()
        codeGenerator.createNewFile(Dependencies(false), service.packageName, className)
            .write(fileSpec)
    }

    private fun toFunSpec(method: Method): FunSpec {
        if (method.isSuspend) return toSuspendFunSpec(method)
        return toNonSuspendFunSpec(method)
    }

    private fun toSuspendFunSpec(method: Method): FunSpec {
        val resultStatement =
            "delegate.${method.name}(${method.parameters.joinToString(", ") { it.name }})"
        return FunSpec.builder(method.name)
            .addModifiers(KModifier.OVERRIDE)
            .addParameters(getParameters(method))
            .addStatement("val job = GlobalScope.launch(Dispatchers.Main) {")
            .addStatement("  try {")
            .addStatement("    val result = $resultStatement")
            .addStatement("    transactionCallback.onSuccess(result)")
            .addStatement("  } catch (t: Throwable) {")
            .addStatement("    transactionCallback.onFailure(404, t.message)")
            .addStatement("  }")
            .addStatement("}")
            .addStatement(
                "val cancellationSignal = TransportCancellationCallback(){ job.cancel() }"
            )
            .addStatement("transactionCallback.onCancellable(cancellationSignal)")
            .build()
    }

    private fun toNonSuspendFunSpec(method: Method): FunSpec {
        val returnStatement =
            "return delegate.${method.name}(${method.parameters.joinToString(", ") { it.name }})"
        return FunSpec.builder(method.name)
            .addModifiers(KModifier.OVERRIDE)
            .addParameters(getParameters(method))
            .returns(method.returnType.specClassName())
            .addStatement(returnStatement)
            .build()
    }

    private fun getParameters(method: Method) = buildList {
        addAll(method.parameters.map { parameter ->
            ParameterSpec(parameter.name, parameter.type.specClassName())
        })
        if (method.isSuspend)
            add(
                ParameterSpec(
                    "transactionCallback",
                    ClassName(
                        api.services.first().packageName,
                        method.returnType.transactionCallbackName()
                    )
                )
            )
    }

    private fun generateTransportCancellationCallback() {
        val packageName = api.services.first().packageName
        val className = "TransportCancellationCallback"
        val cancellationSignalStubName =
            ClassName(packageName, AidlGenerator.cancellationSignalName, "Stub")

        val classSpec =
            TypeSpec.classBuilder(className)
                .superclass(cancellationSignalStubName)
                .addModifiers(KModifier.INTERNAL)
                .primaryConstructor(
                    PropertySpec.builder(
                        "onCancel",
                        LambdaTypeName.get(returnType = Unit::class.asTypeName()),
                    ).addModifiers(KModifier.PRIVATE).build()
                )
                .addProperty(
                    PropertySpec.builder(
                        "hasCancelled",
                        ATOMIC_BOOLEAN_CLASS,
                        KModifier.PRIVATE
                    ).initializer("${ATOMIC_BOOLEAN_CLASS.simpleName}(false)").build()
                )
                .addFunction(
                    FunSpec.builder("cancel")
                        .addModifiers(KModifier.OVERRIDE)
                        .addStatement("if (hasCancelled.compareAndSet(false, true)) {")
                        .addStatement("  onCancel()")
                        .addStatement("}")
                        .build()
                )

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(classSpec.build())
            .build()
        codeGenerator.createNewFile(Dependencies(false), packageName, className)
            .write(fileSpec)
    }
}

internal fun AnnotatedInterface.stubDelegateName() = "${name}StubDelegate"