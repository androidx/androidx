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

import androidx.privacysandbox.tools.core.model.AnnotatedDataClass
import androidx.privacysandbox.tools.core.model.AnnotatedEnumClass
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Constant
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/** Generates a file that defines a previously declared SDK value. */
class ValueFileGenerator {
    fun generate(value: AnnotatedValue) =
        FileSpec.builder(value.type.packageName, value.type.simpleName).build {
            addCommonSettings()
            addType(generateValue(value))
        }

    private fun generateValue(value: AnnotatedValue) =
        when (value) {
            is AnnotatedDataClass ->
                TypeSpec.classBuilder(value.type.poetClassName()).build {
                    addModifiers(KModifier.DATA)
                    primaryConstructor(
                        value.properties.map {
                            PropertySpec.builder(it.name, it.type.poetTypeName())
                                .mutable(false)
                                .build()
                        }
                    )
                    if (value.constants.isNotEmpty()) {
                        addType(
                            TypeSpec.companionObjectBuilder().build {
                                addProperties(value.constants.map(::generateConstant))
                            }
                        )
                    }
                }
            is AnnotatedEnumClass ->
                TypeSpec.enumBuilder(value.type.poetClassName()).build {
                    value.variants.forEach(::addEnumConstant)
                    if (value.constants.isNotEmpty()) {
                        addType(
                            TypeSpec.companionObjectBuilder().build {
                                addProperties(value.constants.map(::generateConstant))
                            }
                        )
                    }
                }
        }
}

fun generateConstant(constant: Constant): PropertySpec {
    var value = constant.value
    // JVM bytecode stores boolean values as 0 or 1, so we convert this back to Boolean.
    if (constant.type.simpleName == "Boolean") {
        value = value != 0
    }
    if (constant.type.simpleName == "String") {
        // Escape strings using KotlinPoet
        value = CodeBlock.of("%S", value)
    }
    // JVM bytecode stores char values as a u16, so we convert this back to Char.
    if (constant.type.simpleName == "Char") {
        val char = (value as Int).toChar()
        // Use KotlinPoet to handle most escape sequences, but we need to handle single-quote
        // ourselves since Poet thinks this is a String.
        val escapedAsString = CodeBlock.of("%S", char).toString().replace("'", "\\'")
        val escapedChar = escapedAsString.substring(1, escapedAsString.length - 1)
        value = "'$escapedChar'"
    }

    return PropertySpec.builder(constant.name, constant.type.poetTypeName(), KModifier.CONST)
        .initializer("%L", value)
        .build()
}
