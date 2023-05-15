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

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import kotlin.coroutines.cancellation.CancellationException

class PrivacySandboxCancellationExceptionFileGenerator(private val basePackageName: String) {

    private val privacySandboxCancellationExceptionName = "PrivacySandboxCancellationException"

    fun generate(): FileSpec {
        val classSpec = TypeSpec.classBuilder(privacySandboxCancellationExceptionName).build {
            superclass(CancellationException::class)
            addModifiers(KModifier.PUBLIC)
            primaryConstructor(
                listOf(
                    PropertySpec.builder(
                        "message",
                        String::class.asTypeName().copy(nullable = true),
                    ).addModifiers(KModifier.OVERRIDE).build(),
                    PropertySpec.builder(
                        "cause",
                        Throwable::class.asTypeName().copy(nullable = true),
                    ).addModifiers(KModifier.OVERRIDE).build()
                )
            )
        }

        return FileSpec.builder(
            basePackageName,
            privacySandboxCancellationExceptionName
        ).build {
            addCommonSettings()
            addType(classSpec)
        }
    }
}