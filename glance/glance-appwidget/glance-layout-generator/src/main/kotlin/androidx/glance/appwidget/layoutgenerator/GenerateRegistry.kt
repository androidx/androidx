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

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.INTERNAL
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.joinToCode
import java.io.File

/**
 * Generate the registry: a mapping from `LayoutSelector` to `Layout`.
 *
 * For each generated layout, a selector is created describing what the layout can and cannot do. It
 * is mapped to a `Layout`, an object describing the parameters needed to act on the layout.
 */
internal fun generateRegistry(
    packageName: String,
    layouts: Map<File, List<ContainerProperties>>,
    boxChildLayouts: Map<File, List<BoxChildProperties>>,
    rowColumnChildLayouts: Map<File, List<RowColumnChildProperties>>,
    outputSourceDir: File,
) {
    outputSourceDir.mkdirs()
    val file = FileSpec.builder(packageName, "GeneratedLayouts")

    val generatedContainerApi21 =
        funSpec("registerContainers") {
            returns(ContainerMap)
            addModifiers(PRIVATE)
            addStatement("val result =")
            addCode(buildInitializer(layouts))
            addStatement("return result")
        }

    val generatedChildrenApi21 =
        funSpec("registerChildren") {
            returns(ContainerChildrenMap)
            addModifiers(PRIVATE)
            addStatement("val result =")
            addCode(buildChildrenInitializer(layouts, StubSizes))
            addStatement("return result")
        }

    val requireApi31 =
        AnnotationSpec.builder(RequiresApi).apply { addMember("%M", VersionCodeS) }.build()
    val generatedContainerApi31 =
        objectSpec("GeneratedContainersForApi31Impl") {
            addModifiers(PRIVATE)
            addAnnotation(requireApi31)
            addFunction(
                funSpec("registerContainers") {
                    returns(ContainerMap)
                    addAnnotation(DoNotInline)
                    addStatement("val result =")
                    addCode(buildInitializer(layouts))
                    addStatement("return result")
                }
            )
            addFunction(
                funSpec("registerChildren") {
                    returns(ContainerChildrenMap)
                    addAnnotation(DoNotInline)
                    addStatement("val result =")
                    addCode(buildChildrenInitializer(layouts, listOf(ValidSize.Wrap)))
                    addStatement("return result")
                }
            )
        }

    val generatedLayouts =
        propertySpec(
            "generatedContainers",
            ContainerMap,
            INTERNAL,
        ) {
            initializer(
                buildCodeBlock {
                    addStatement(
                        """
                |if(%M >= %M) {
                |  GeneratedContainersForApi31Impl.registerContainers()
                |} else {
                |  registerContainers()
                |}"""
                            .trimMargin(),
                        SdkInt,
                        VersionCodeS
                    )
                }
            )
        }
    file.addProperty(generatedLayouts)
    file.addFunction(generatedContainerApi21)

    val generatedChildren =
        propertySpec(
            "generatedChildren",
            ContainerChildrenMap,
            INTERNAL,
        ) {
            initializer(
                buildCodeBlock {
                    addStatement(
                        """
                |if(%M >= %M) {
                |  GeneratedContainersForApi31Impl.registerChildren()
                |} else {
                |  registerChildren()
                |}"""
                            .trimMargin(),
                        SdkInt,
                        VersionCodeS
                    )
                }
            )
        }
    file.addProperty(generatedChildren)
    file.addFunction(generatedChildrenApi21)
    file.addType(generatedContainerApi31)

    // TODO: only register the box children on T+, since the layouts are in layout-v32
    val generatedBoxChildren =
        propertySpec(
            "generatedBoxChildren",
            BoxChildrenMap,
            INTERNAL,
        ) {
            initializer(buildBoxChildInitializer(boxChildLayouts))
        }
    file.addProperty(generatedBoxChildren)
    val generatedRowColumnChildren =
        propertySpec(
            "generatedRowColumnChildren",
            RowColumnChildrenMap,
            INTERNAL,
        ) {
            initializer(buildRowColumnChildInitializer(rowColumnChildLayouts))
        }
    file.addProperty(generatedRowColumnChildren)

    val generatedComplexLayouts =
        propertySpec("generatedComplexLayouts", LayoutsMap, INTERNAL) {
            initializer(buildComplexInitializer())
        }
    file.addProperty(generatedComplexLayouts)

    val generatedRoots =
        propertySpec("generatedRootLayoutShifts", SizeSelectorToIntMap, INTERNAL) {
            addKdoc("Shift per root layout before Android S, based on width, height")
            initializer(buildRootInitializer())
        }
    file.addProperty(generatedRoots)

    val firstRootAlias =
        propertySpec("FirstRootAlias", INT, INTERNAL) {
            initializer("R.layout.${makeRootAliasResourceName(0)}")
        }
    val lastRootAlias =
        propertySpec("LastRootAlias", INT, INTERNAL) {
            initializer(
                "R.layout.%L",
                makeRootAliasResourceName(generatedRootSizePairs.size * RootLayoutAliasCount - 1)
            )
        }
    val rootAliasCount =
        propertySpec("RootAliasCount", INT, INTERNAL) {
            initializer("%L", generatedRootSizePairs.size * RootLayoutAliasCount)
        }
    file.addProperty(firstRootAlias)
    file.addProperty(lastRootAlias)
    file.addProperty(rootAliasCount)

    val firstViewId =
        propertySpec("FirstViewId", INT, INTERNAL) {
            initializer("R.id.${makeViewIdResourceName(0)}")
        }
    val lastViewId =
        propertySpec("LastViewId", INT, INTERNAL) {
            initializer("R.id.${makeViewIdResourceName(TotalViewCount - 1)}")
        }
    val totalViewCount =
        propertySpec("TotalViewCount", INT, INTERNAL) { initializer("%L", TotalViewCount) }
    file.addProperty(firstViewId)
    file.addProperty(lastViewId)
    file.addProperty(totalViewCount)

    file.build().writeTo(outputSourceDir)
}

private fun buildInitializer(layouts: Map<File, List<ContainerProperties>>): CodeBlock =
    buildCodeBlock {
        withIndent {
            addStatement("mapOf(")
            withIndent {
                add(
                    layouts
                        .map { it.key to createFileInitializer(it.key, it.value) }
                        .sortedBy { it.first.nameWithoutExtension }
                        .map { it.second }
                        .joinToCode("")
                )
            }
            addStatement(")")
        }
    }

private fun buildChildrenInitializer(
    layouts: Map<File, List<ContainerProperties>>,
    sizes: List<ValidSize>,
): CodeBlock = buildCodeBlock {
    withIndent {
        addStatement("mapOf(")
        withIndent {
            add(
                layouts
                    .map { it.key to createChildrenInitializer(it.key, it.value, sizes) }
                    .sortedBy { it.first.nameWithoutExtension }
                    .map { it.second }
                    .joinToCode("")
            )
        }
        addStatement(")")
    }
}

private fun buildBoxChildInitializer(layouts: Map<File, List<BoxChildProperties>>): CodeBlock =
    buildCodeBlock {
        withIndent {
            addStatement("mapOf(")
            withIndent {
                add(
                    layouts
                        .map { it.key to createBoxChildFileInitializer(it.key, it.value) }
                        .sortedBy { it.first.nameWithoutExtension }
                        .map { it.second }
                        .joinToCode("")
                )
            }
            addStatement(")")
        }
    }

private fun buildRowColumnChildInitializer(
    layouts: Map<File, List<RowColumnChildProperties>>
): CodeBlock = buildCodeBlock {
    withIndent {
        addStatement("mapOf(")
        withIndent {
            add(
                layouts
                    .map { it.key to createRowColumnChildFileInitializer(it.key, it.value) }
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
                    SizeSelector,
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

private fun buildRootInitializer(): CodeBlock {
    return buildCodeBlock {
        addStatement("mapOf(")
        withIndent {
            generatedRootSizePairs.forEachIndexed { index, (width, height) ->
                addStatement(
                    "%T(width = %M, height = %M) to %L,",
                    SizeSelector,
                    width.toValue(),
                    height.toValue(),
                    index,
                )
            }
        }
        addStatement(")")
    }
}

private fun createFileInitializer(layout: File, generated: List<ContainerProperties>): CodeBlock =
    buildCodeBlock {
        val viewType = layout.nameWithoutExtension.toLayoutType()
        generated.forEach { props ->
            addContainer(
                resourceName =
                    makeContainerResourceName(
                        layout,
                        props.numberChildren,
                        props.horizontalAlignment,
                        props.verticalAlignment
                    ),
                viewType = viewType,
                horizontalAlignment = props.horizontalAlignment,
                verticalAlignment = props.verticalAlignment,
                numChildren = props.numberChildren,
            )
        }
    }

private fun createBoxChildFileInitializer(
    layout: File,
    generated: List<BoxChildProperties>
): CodeBlock = buildCodeBlock {
    val viewType = layout.nameWithoutExtension.toLayoutType()
    generated.forEach { props ->
        addBoxChild(
            resourceName =
                makeBoxChildResourceName(
                    layout,
                    props.horizontalAlignment,
                    props.verticalAlignment
                ),
            viewType = viewType,
            horizontalAlignment = props.horizontalAlignment,
            verticalAlignment = props.verticalAlignment,
        )
    }
}

private fun createRowColumnChildFileInitializer(
    layout: File,
    generated: List<RowColumnChildProperties>
): CodeBlock = buildCodeBlock {
    val viewType = layout.nameWithoutExtension.toLayoutType()
    generated.forEach { props ->
        addRowColumnChild(
            resourceName = makeRowColumnChildResourceName(layout, props.width, props.height),
            viewType = viewType,
            width = props.width,
            height = props.height,
        )
    }
}

private fun createChildrenInitializer(
    layout: File,
    generated: List<ContainerProperties>,
    sizes: List<ValidSize>,
): CodeBlock = buildCodeBlock {
    val viewType = layout.nameWithoutExtension.toLayoutType()
    val orientation = generated.first().containerOrientation
    val numChildren =
        generated.map { it.numberChildren }.maxOrNull() ?: error("There must be children")
    childrenInitializer(viewType, generateChildren(numChildren, orientation, sizes))
}

private fun generateChildren(
    numChildren: Int,
    containerOrientation: ContainerOrientation,
    sizes: List<ValidSize>
) =
    (0 until numChildren).associateWith { pos ->
        val widths = sizes + containerOrientation.extraWidths
        val heights = sizes + containerOrientation.extraHeights
        mapInCrossProduct(widths, heights) { width, height ->
            ChildProperties(
                childId = makeIdName(pos, width, height),
                width = width,
                height = height,
            )
        }
    }

private fun CodeBlock.Builder.childrenInitializer(
    viewType: String,
    children: Map<Int, List<ChildProperties>>,
) {
    addStatement("%M to mapOf(", makeViewType(viewType))
    withIndent {
        children
            .toList()
            .sortedBy { it.first }
            .forEach { (pos, children) ->
                addStatement("$pos to mapOf(")
                withIndent {
                    children.forEach { child ->
                        addStatement(
                            "%T(width = %M, height = %M)",
                            SizeSelector,
                            child.width.toValue(),
                            child.height.toValue(),
                        )
                        withIndent {
                            addStatement(
                                "to R.id.${
                                    makeIdName(
                                        pos,
                                        child.width,
                                        child.height
                                    )
                                },"
                            )
                        }
                    }
                }
                addStatement("),")
            }
    }
    addStatement("),")
}

private fun CodeBlock.Builder.addContainer(
    resourceName: String,
    viewType: String,
    horizontalAlignment: HorizontalAlignment?,
    verticalAlignment: VerticalAlignment?,
    numChildren: Int,
) {
    addStatement("%T(", ContainerSelector)
    withIndent {
        addStatement("type = %M,", makeViewType(viewType))
        addStatement("numChildren = %L,", numChildren)
        if (horizontalAlignment != null) {
            addStatement("horizontalAlignment = %M, ", horizontalAlignment.code)
        }
        if (verticalAlignment != null) {
            addStatement("verticalAlignment = %M, ", verticalAlignment.code)
        }
    }
    addStatement(") to %T(layoutId = R.layout.$resourceName),", ContainerInfo)
}

private fun CodeBlock.Builder.addBoxChild(
    resourceName: String,
    viewType: String,
    horizontalAlignment: HorizontalAlignment,
    verticalAlignment: VerticalAlignment,
) {
    addStatement("%T(", BoxChildSelector)
    withIndent {
        addStatement("type = %M,", makeViewType(viewType))
        addStatement("horizontalAlignment = %M, ", horizontalAlignment.code)
        addStatement("verticalAlignment = %M, ", verticalAlignment.code)
    }
    addStatement(") to %T(layoutId = R.layout.$resourceName),", LayoutInfo)
}

private fun CodeBlock.Builder.addRowColumnChild(
    resourceName: String,
    viewType: String,
    width: ValidSize,
    height: ValidSize,
) {
    addStatement("%T(", RowColumnChildSelector)
    withIndent {
        addStatement("type = %M,", makeViewType(viewType))
        addStatement("expandWidth = %L, ", width == ValidSize.Expand)
        addStatement("expandHeight = %L, ", height == ValidSize.Expand)
    }
    addStatement(") to %T(layoutId = R.layout.$resourceName),", LayoutInfo)
}

private val ContainerSelector = ClassName("androidx.glance.appwidget", "ContainerSelector")
private val SizeSelector = ClassName("androidx.glance.appwidget", "SizeSelector")
private val BoxChildSelector = ClassName("androidx.glance.appwidget", "BoxChildSelector")
private val RowColumnChildSelector =
    ClassName("androidx.glance.appwidget", "RowColumnChildSelector")
private val LayoutInfo = ClassName("androidx.glance.appwidget", "LayoutInfo")
private val ContainerInfo = ClassName("androidx.glance.appwidget", "ContainerInfo")
private val ContainerMap = Map::class.asTypeName().parameterizedBy(ContainerSelector, ContainerInfo)
private const val LayoutSpecSize = "androidx.glance.appwidget.LayoutSize"
private val WrapValue = MemberName("$LayoutSpecSize", "Wrap")
private val FixedValue = MemberName("$LayoutSpecSize", "Fixed")
private val MatchValue = MemberName("$LayoutSpecSize", "MatchParent")
private val ExpandValue = MemberName("$LayoutSpecSize", "Expand")
private val LayoutsMap = Map::class.asTypeName().parameterizedBy(SizeSelector, LayoutInfo)
private val SizeSelectorToIntMap = Map::class.asTypeName().parameterizedBy(SizeSelector, INT)
private val AndroidBuildVersion = ClassName("android.os", "Build", "VERSION")
private val AndroidBuildVersionCodes = ClassName("android.os", "Build", "VERSION_CODES")
private val SdkInt = AndroidBuildVersion.member("SDK_INT")
private val VersionCodeS = AndroidBuildVersionCodes.member("S")
private val RequiresApi = ClassName("androidx.annotation", "RequiresApi")
private val DoNotInline = ClassName("androidx.annotation", "DoNotInline")
private val HorizontalAlignmentType =
    ClassName("androidx.glance.layout", "Alignment", "Horizontal", "Companion")
private val VerticalAlignmentType =
    ClassName("androidx.glance.layout", "Alignment", "Vertical", "Companion")
internal val AlignmentStart = MemberName(HorizontalAlignmentType, "Start")
internal val AlignmentCenterHorizontally = MemberName(HorizontalAlignmentType, "CenterHorizontally")
internal val AlignmentEnd = MemberName(HorizontalAlignmentType, "End")
internal val AlignmentTop = MemberName(VerticalAlignmentType, "Top")
internal val AlignmentCenterVertically = MemberName(VerticalAlignmentType, "CenterVertically")
internal val AlignmentBottom = MemberName(VerticalAlignmentType, "Bottom")
private val LayoutType = ClassName("androidx.glance.appwidget", "LayoutType")
private val ChildrenMap = Map::class.asTypeName().parameterizedBy(INT, SizeSelectorToIntMap)
private val ContainerChildrenMap = Map::class.asTypeName().parameterizedBy(LayoutType, ChildrenMap)
private val BoxChildrenMap = Map::class.asTypeName().parameterizedBy(BoxChildSelector, LayoutInfo)
private val RowColumnChildrenMap =
    Map::class.asTypeName().parameterizedBy(RowColumnChildSelector, LayoutInfo)

private fun makeViewType(name: String) = MemberName("androidx.glance.appwidget.LayoutType", name)

private fun String.toLayoutType(): String =
    snakeRegex
        .replace(this.removePrefix("glance_")) { it.value.replace("_", "").uppercase() }
        .replaceFirstChar { it.uppercaseChar() }

private val snakeRegex = "_[a-zA-Z0-9]".toRegex()

private fun ValidSize.toValue() =
    when (this) {
        ValidSize.Wrap -> WrapValue
        ValidSize.Fixed -> FixedValue
        ValidSize.Expand -> ExpandValue
        ValidSize.Match -> MatchValue
    }

internal fun makeComplexResourceName(width: ValidSize, height: ValidSize) =
    listOf(
            "complex",
            width.resourceName,
            height.resourceName,
        )
        .joinToString(separator = "_")

internal fun makeRootResourceName(width: ValidSize, height: ValidSize) =
    listOf(
            "root",
            width.resourceName,
            height.resourceName,
        )
        .joinToString(separator = "_")

internal fun makeRootAliasResourceName(index: Int) = "root_alias_%03d".format(index)

internal fun makeViewIdResourceName(index: Int) = "glance_view_id_%03d".format(index)

internal fun makeContainerResourceName(
    file: File,
    numChildren: Int,
    horizontalAlignment: HorizontalAlignment?,
    verticalAlignment: VerticalAlignment?
) =
    listOf(
            file.nameWithoutExtension,
            horizontalAlignment?.resourceName,
            verticalAlignment?.resourceName,
            "${numChildren}children"
        )
        .joinToString(separator = "_")

internal fun makeChildResourceName(
    pos: Int,
    containerOrientation: ContainerOrientation,
    horizontalAlignment: HorizontalAlignment?,
    verticalAlignment: VerticalAlignment?
) =
    listOf(
            containerOrientation.resourceName,
            "child",
            horizontalAlignment?.resourceName,
            verticalAlignment?.resourceName,
            "group",
            pos
        )
        .joinToString(separator = "_")

internal fun makeBoxChildResourceName(
    file: File,
    horizontalAlignment: HorizontalAlignment?,
    verticalAlignment: VerticalAlignment?
) =
    listOf(
            file.nameWithoutExtension,
            horizontalAlignment?.resourceName,
            verticalAlignment?.resourceName,
        )
        .joinToString(separator = "_")

internal fun makeRowColumnChildResourceName(
    file: File,
    width: ValidSize,
    height: ValidSize,
) =
    listOf(
            file.nameWithoutExtension,
            if (width == ValidSize.Expand) "expandwidth" else "wrapwidth",
            if (height == ValidSize.Expand) "expandheight" else "wrapheight",
        )
        .joinToString(separator = "_")

internal fun makeIdName(pos: Int, width: ValidSize, height: ValidSize) =
    listOf("childStub$pos", width.resourceName, height.resourceName).joinToString(separator = "_")

internal fun CodeBlock.Builder.withIndent(
    builderAction: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder {
    indent()
    apply(builderAction)
    unindent()
    return this
}

internal fun funSpec(name: String, builder: FunSpec.Builder.() -> Unit) =
    FunSpec.builder(name).apply(builder).build()

internal fun objectSpec(name: String, builder: TypeSpec.Builder.() -> Unit) =
    TypeSpec.objectBuilder(name).apply(builder).build()

internal fun propertySpec(
    name: String,
    type: TypeName,
    vararg modifiers: KModifier,
    builder: PropertySpec.Builder.() -> Unit
) = PropertySpec.builder(name, type, *modifiers).apply(builder).build()

private val listConfigurations =
    crossProduct(ValidSize.values().toList(), ValidSize.values().toList())

private val generatedRootSizePairs = crossProduct(StubSizes, StubSizes)

internal inline fun mapConfiguration(
    function: (width: ValidSize, height: ValidSize) -> File
): List<File> = listConfigurations.map { (a, b) -> function(a, b) }

internal inline fun forEachConfiguration(function: (width: ValidSize, height: ValidSize) -> Unit) {
    listConfigurations.forEach { (a, b) -> function(a, b) }
}

internal inline fun <A, B, T> mapInCrossProduct(
    first: Iterable<A>,
    second: Iterable<B>,
    consumer: (A, B) -> T
): List<T> = first.flatMap { a -> second.map { b -> consumer(a, b) } }

internal inline fun <A, B, T> forEachInCrossProduct(
    first: Iterable<A>,
    second: Iterable<B>,
    consumer: (A, B) -> T
) {
    first.forEach { a -> second.forEach { b -> consumer(a, b) } }
}

internal fun <A, B> crossProduct(
    first: Iterable<A>,
    second: Iterable<B>,
): List<Pair<A, B>> = mapInCrossProduct(first, second) { a, b -> a to b }

internal fun File.resolveRes(resName: String) = resolve("$resName.xml")
