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

import androidx.privacysandbox.tools.core.model.AnnotatedValue
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a file that defines a previously declared SDK value.
 */
class ValueFileGenerator {
    fun generate(value: AnnotatedValue) =
        FileSpec.builder(value.type.packageName, value.type.simpleName).build {
            addCommonSettings()
            addType(generateValue(value))
        }

    private fun generateValue(value: AnnotatedValue) =
        TypeSpec.classBuilder(value.type.poetClassName()).build {
            addModifiers(KModifier.DATA)
            primaryConstructor(value.properties.map {
                PropertySpec.builder(it.name, it.type.poetTypeName())
                    .mutable(false)
                    .build()
            })
        }
}
