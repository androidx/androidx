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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class TransportCancellationGenerator(private val basePackageName: String) {
    companion object {
        const val className = "TransportCancellationCallback"
    }

    private val atomicBooleanClass = ClassName("java.util.concurrent.atomic", "AtomicBoolean")

    fun generate(): FileSpec {
        val packageName = basePackageName
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
                    "hasCancelled", atomicBooleanClass, KModifier.PRIVATE
                ).initializer("%T(false)", atomicBooleanClass).build()
            )
            addFunction(FunSpec.builder("cancel").build {
                addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
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
