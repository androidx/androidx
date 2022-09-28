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

import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.Type
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

/** [ParameterSpec] equivalent to this parameter. */
public fun Parameter.poetSpec(): ParameterSpec {
    return ParameterSpec.builder(name, type.poetSpec()).build()
}

/** [TypeName] equivalent to this parameter. */
public fun Type.poetSpec(): ClassName {
    val splits = name.split('.')
    return ClassName(splits.dropLast(1).joinToString("."), splits.last())
}

/**
 * Defines the primary constructor of this type with the given list of properties.
 *
 * @param modifiers extra modifiers added to the constructor
 */
public fun TypeSpec.Builder.primaryConstructor(
    properties: List<PropertySpec>,
    vararg modifiers: KModifier,
) {
    val propertiesWithInitializer =
        properties.map {
            it.toBuilder().initializer(it.name)
                .build()
        }
    primaryConstructor(
        FunSpec.constructorBuilder().build {
            addParameters(propertiesWithInitializer.map { ParameterSpec(it.name, it.type) })
            addModifiers(*modifiers)
        }
    )
    addProperties(propertiesWithInitializer)
}

/** Builds a [TypeSpec] using the given builder block. */
public fun TypeSpec.Builder.build(block: TypeSpec.Builder.() -> Unit): TypeSpec {
    block()
    return build()
}

public fun CodeBlock.Builder.build(block: CodeBlock.Builder.() -> Unit): CodeBlock {
    block()
    return build()
}

/** Builds a [FunSpec] using the given builder block. */
public fun FunSpec.Builder.build(block: FunSpec.Builder.() -> Unit): FunSpec {
    block()
    return build()
}

/** Builds a [FileSpec] using the given builder block. */
public fun FileSpec.Builder.build(block: FileSpec.Builder.() -> Unit): FileSpec {
    block()
    return build()
}

public fun FunSpec.Builder.addCode(block: CodeBlock.Builder.() -> Unit) {
    addCode(CodeBlock.builder().build { block() })
}

/** Auto-closing control flow construct and its code. */
public fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    block: CodeBlock.Builder.() -> Unit
) {
    beginControlFlow(controlFlow)
    block()
    endControlFlow()
}
