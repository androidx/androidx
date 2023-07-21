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

import androidx.privacysandbox.tools.core.generator.GenerationTarget.SERVER
import androidx.privacysandbox.tools.core.generator.SpecNames.contextClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.coroutineScopeClass
import androidx.privacysandbox.tools.core.generator.SpecNames.dispatchersMainClass
import androidx.privacysandbox.tools.core.generator.SpecNames.launchMethod
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

class StubDelegatesGenerator(
    private val basePackageName: String,
    private val binderCodeConverter: BinderCodeConverter,
) {
    companion object {
        fun transportCancellationCallbackNameSpec(packageName: String) =
            ClassName(packageName, TransportCancellationGenerator.className)
    }

    private val coroutineScopePropertyName = "coroutineScope"

    /**
     * Generates a StubDelegate for this interface.
     *
     * This allows a server-side interface to be called by a remote ClientProxy.
     *
     * If  [target] is [GenerationTarget.SERVER] (ie. this will run on the SDK-side) includes a
     * Context that will be the SDK context.
     */
    fun generate(annotatedInterface: AnnotatedInterface, target: GenerationTarget): FileSpec {
        val className = annotatedInterface.stubDelegateNameSpec().simpleName
        val aidlBaseClassName = ClassName(
            annotatedInterface.type.packageName, annotatedInterface.aidlName(), "Stub"
        )

        val classSpec = TypeSpec.classBuilder(className).build {
            superclass(aidlBaseClassName)

            primaryConstructor(
                buildList {
                    add(
                        PropertySpec.builder(
                            "delegate",
                            annotatedInterface.type.poetTypeName(),
                        ).addModifiers(KModifier.PUBLIC).build()
                    )
                    if (target == SERVER) {
                        add(
                            PropertySpec.builder(contextPropertyName, contextClass)
                                .addModifiers(KModifier.PUBLIC).build()
                        )
                    }
                },
                KModifier.INTERNAL,
            )

            val coroutineProperty =
                PropertySpec.builder(coroutineScopePropertyName, coroutineScopeClass)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(CodeBlock.of("%T(%T)", coroutineScopeClass, dispatchersMainClass))
                    .build()
            addProperty(coroutineProperty)
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
                    "val job = %L.%M", coroutineScopePropertyName, launchMethod
                ) {
                    addControlFlow("try") {
                        addStatement {
                            if (method.returnType != Types.unit) {
                                add("val result = ")
                            }
                            add(getDelegateCallBlock(method))
                        }
                        if (method.returnType == Types.unit) {
                            addStatement("transactionCallback.onSuccess()")
                        } else {
                            addStatement(
                                "transactionCallback.onSuccess(%L)",
                                binderCodeConverter.convertToBinderCode(
                                    method.returnType, "result"
                                )
                            )
                        }
                    }
                    addControlFlow("catch (t: Throwable)") {
                        addStatement(
                            "transactionCallback.onFailure(%M(t))",
                            ThrowableParcelConverterFileGenerator.toThrowableParcelNameSpec(
                                basePackageName
                            )
                        )
                    }
                }
                addStatement(
                    "val cancellationSignal = %T() { job.cancel() }",
                    transportCancellationCallbackNameSpec(basePackageName)
                )
                addStatement("transactionCallback.onCancellable(cancellationSignal)")
            }
        }
    }

    private fun toNonSuspendFunSpec(method: Method) = FunSpec.builder(method.name).build {
        addModifiers(KModifier.OVERRIDE)
        addParameters(getParameters(method))
        addCode(CodeBlock.builder().build {
            addControlFlow("%L.%M", coroutineScopePropertyName, launchMethod) {
                addStatement { add(getDelegateCallBlock(method)) }
            }
        })
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
                    basePackageName,
                    wrapWithListIfNeeded(method.returnType).transactionCallbackName()
                )
            )
        )
    }

    private fun getDelegateCallBlock(method: Method) = CodeBlock.builder().build {
        add("delegate.${method.name}(")
        add(method.parameters.map { binderCodeConverter.convertToModelCode(it.type, it.name) }
            .joinToCode())
        add(")")
    }
}
