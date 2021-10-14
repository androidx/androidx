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
        KModifier.INTERNAL
    ).apply {
        initializer(buildInitializer(layouts))
    }.build()
    file.addProperty(generatedLayouts)
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

private fun createFileInitializer(layout: File, mainViewId: String): CodeBlock = buildCodeBlock {
    val viewType = layout.nameWithoutExtension.toLayoutType()
    forEachConfiguration(layout) { width, height, childCount ->
        addLayout(
            resourceName = makeSimpleResourceName(layout, width, height, childCount),
            viewType = viewType,
            width = width,
            height = height,
            canResize = false,
            mainViewId = "R.id.$mainViewId",
            sizeViewId = null,
            childCount = childCount
        )
        addLayout(
            resourceName = makeComplexResourceName(layout, width, height, childCount),
            viewType = viewType,
            width = width,
            height = height,
            canResize = true,
            mainViewId = "R.id.$mainViewId",
            sizeViewId = "R.id.sizeView",
            childCount = childCount
        )
    }
}

private fun CodeBlock.Builder.addLayout(
    resourceName: String,
    viewType: String,
    width: ValidSize,
    height: ValidSize,
    canResize: Boolean,
    mainViewId: String,
    sizeViewId: String?,
    childCount: Int
) {
    addStatement(
        "%T(type = %M, width = %M, height = %M, canResize = $canResize, childCount = %L) to ",
        LayoutSelector,
        makeViewType(viewType),
        width.toValue(),
        height.toValue(),
        childCount
    )
    withIndent {
        addStatement("%T(", LayoutIds)
        withIndent {
            addStatement("layoutId = R.layout.$resourceName,")
            addStatement("mainViewId = $mainViewId,")
            addStatement("sizeViewId = $sizeViewId,")
        }
        addStatement("),")
    }
}

private val LayoutSelector = ClassName("androidx.glance.appwidget", "LayoutSelector")
private val LayoutIds = ClassName("androidx.glance.appwidget", "LayoutIds")
private val LayoutsMap = Map::class.asTypeName().parameterizedBy(LayoutSelector, LayoutIds)
private const val LayoutSpecSize = "androidx.glance.appwidget.LayoutSelector.Size"
private val WrapValue = MemberName("$LayoutSpecSize", "Wrap")
private val FixedValue = MemberName("$LayoutSpecSize", "Fixed")
private val MatchValue = MemberName("$LayoutSpecSize", "MatchParent")
private val ExpandValue = MemberName("$LayoutSpecSize", "Expand")

private fun makeViewType(name: String) =
    MemberName("androidx.glance.appwidget.LayoutSelector.Type", name)

private fun String.toLayoutType() =
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

internal fun makeSimpleResourceName(
    file: File,
    width: ValidSize,
    height: ValidSize,
    childCount: Int
) = makeResourceName(file, width, height, childCount, isSimple = true)

internal fun makeComplexResourceName(
    file: File,
    width: ValidSize,
    height: ValidSize,
    childCount: Int
) = makeResourceName(file, width, height, childCount, isSimple = false)

private fun makeResourceName(
    file: File,
    width: ValidSize,
    height: ValidSize,
    childCount: Int,
    isSimple: Boolean,
): String {
    return listOfNotNull(
        file.nameWithoutExtension,
        if (isSimple) "simple" else "complex",
        width.resourceName,
        height.resourceName,
        if (childCount > 0) "${childCount}child" else null
    )
        .joinToString(separator = "_")
}

fun CodeBlock.Builder.withIndent(builderAction: CodeBlock.Builder.() -> Unit): CodeBlock.Builder {
    indent()
    apply(builderAction)
    unindent()
    return this
}

/**
 * The list of layout templates corresponding to collections that should have view stub children.
 */
private val CollectionFiles = listOf("box", "column", "row")

/**
 * Returns whether the [File] is for a collection layout that should have generated view stub
 * children.
 */
internal fun File.isCollectionLayout() = nameWithoutExtension in CollectionFiles

internal fun File.allChildCounts(): List<Int> {
    return if (isCollectionLayout()) {
        (0..MaxChildren).toList()
    } else {
        listOf(0)
    }
}

/** The maximum number of direct children that a collection layout can have. */
internal const val MaxChildren = 10
