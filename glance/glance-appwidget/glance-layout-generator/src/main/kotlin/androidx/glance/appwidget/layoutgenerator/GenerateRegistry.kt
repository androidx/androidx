/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.layoutgenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import java.io.File

/**
 * Generate the registry: a mapping from `LayoutSelector` to `Layout`.
 *
 * For each generated layout, a selector is created describing what the layout can and cannot do.
 * It is mapped to a `Layout`, an object describing the parameters needed to act on the layout.
 */
internal fun generateRegistry(
    packageName: String,
    layouts: Map<File, LayoutProperties>,
    outputSourceDir: File,
) {
    outputSourceDir.mkdirs()
    val file = FileSpec.builder(packageName, "GeneratedLayouts")
    val generatedLayouts = PropertySpec.builder(
        "generatedLayouts",
        LayoutsMap,
        KModifier.INTERNAL,
    ).apply {
        initializer(buildInitializer(layouts))
    }.build()
    file.addProperty(generatedLayouts)

    val generatedComplexLayouts = PropertySpec.builder(
        "generatedComplexLayouts",
        ComplexLayoutsMap,
        KModifier.INTERNAL,
    ).apply {
        initializer(buildComplexInitializer())
    }.build()
    file.addProperty(generatedComplexLayouts)

    file.build().writeTo(outputSourceDir)
}

private fun buildInitializer(layouts: Map<File, LayoutProperties>): CodeBlock {
    return buildCodeBlock {
        addStatement("mapOf(")
        withIndent {
            add(
                layouts.map {
                    it.key to createFileInitializer(it.key, it.value.mainViewId)
                }
                    .sortedBy { it.first.nameWithoutExtension }
                    .map { it.second }
                    .joinToCode("")
            )
        }
        addStatement(")")
    }
}

private fun buildComplexInitializer(): CodeBlock {
    return buildCodeBlock {
        addStatement("mapOf(")
        withIndent {
            forEachConfiguration { width, height ->
                addStatement(
                    "%T(width = %M, height = %M) to ",
                    ComplexSelector,
                    width.toValue(),
                    height.toValue(),
                )
                withIndent {
                    val resId = makeComplexResourceName(width, height)
                    addStatement("%T(layoutId = R.layout.$resId),", LayoutInfo)
                }
            }
        }
        addStatement(")")
    }
}

private fun createFileInitializer(layout: File, mainViewId: String): CodeBlock = buildCodeBlock {
    val viewType = layout.nameWithoutExtension.toLayoutType()
    forEachConfiguration { width, height ->
        addLayout(
            resourceName = makeSimpleResourceName(layout, width, height),
            viewType = viewType,
            width = width,
            height = height,
            mainViewId = "R.id.$mainViewId",
        )
    }
}

private fun CodeBlock.Builder.addLayout(
    resourceName: String,
    viewType: String,
    width: ValidSize,
    height: ValidSize,
    mainViewId: String,
) {
    addStatement(
        "%T(type = %M, width = %M, height = %M) to ",
        LayoutSelector,
        makeViewType(viewType),
        width.toValue(),
        height.toValue(),
    )
    withIndent {
        addStatement("%T(", LayoutInfo)
        withIndent {
            addStatement("layoutId = R.layout.$resourceName,")
            addStatement("mainViewId = $mainViewId,")
        }
        addStatement("),")
    }
}

private val LayoutSelector = ClassName("androidx.glance.appwidget", "LayoutSelector")
private val ComplexSelector = ClassName("androidx.glance.appwidget", "ComplexSelector")
private val LayoutInfo = ClassName("androidx.glance.appwidget", "LayoutInfo")
private val LayoutsMap = Map::class.asTypeName().parameterizedBy(LayoutSelector, LayoutInfo)
private const val LayoutSpecSize = "androidx.glance.appwidget.LayoutSelector.Size"
private val WrapValue = MemberName("$LayoutSpecSize", "Wrap")
private val FixedValue = MemberName("$LayoutSpecSize", "Fixed")
private val MatchValue = MemberName("$LayoutSpecSize", "MatchParent")
private val ExpandValue = MemberName("$LayoutSpecSize", "Expand")
private val ComplexLayoutsMap = Map::class.asTypeName().parameterizedBy(ComplexSelector, LayoutInfo)

private fun makeViewType(name: String) =
    MemberName("androidx.glance.appwidget.LayoutSelector.Type", name)

private fun String.toLayoutType(): String =
    snakeRegex.replace(this) {
        it.value.replace("_", "").uppercase()
    }.replaceFirstChar { it.uppercaseChar() }

private val snakeRegex = "_[a-zA-Z0-9]".toRegex()

private fun ValidSize.toValue() = when (this) {
    ValidSize.Wrap -> WrapValue
    ValidSize.Fixed -> FixedValue
    ValidSize.Expand -> ExpandValue
    ValidSize.Match -> MatchValue
}

internal fun makeSimpleResourceName(file: File, width: ValidSize, height: ValidSize) =
    listOf(
        file.nameWithoutExtension,
        width.resourceName,
        height.resourceName,
    ).joinToString(separator = "_")

internal fun makeComplexResourceName(width: ValidSize, height: ValidSize) =
    listOf(
        "complex",
        width.resourceName,
        height.resourceName,
    ).joinToString(separator = "_")

fun CodeBlock.Builder.withIndent(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock.Builder {
    indent()
    apply(builderAction)
    unindent()
    return this
}

private val listConfigurations =
    ValidSize.values().flatMap { width ->
        ValidSize.values().map { height ->
            width to height
        }
    }

internal inline fun mapConfiguration(
    function: (width: ValidSize, height: ValidSize) -> File
): List<File> =
    listConfigurations.map { (a, b) -> function(a, b) }

internal inline fun forEachConfiguration(function: (width: ValidSize, height: ValidSize) -> Unit) {
    listConfigurations.forEach { (a, b) -> function(a, b) }
}

internal inline fun <A, B, T> mapInCrossProduct(
    first: Iterable<A>,
    second: Iterable<B>,
    consumer: (A, B) -> T
): List<T> =
    first.flatMap { a ->
        second.map { b ->
            consumer(a, b)
        }
    }
