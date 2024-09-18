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

import com.squareup.kotlinpoet.MemberName
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Node

/**
 * Generate the layouts from the templates provided to the task.
 *
 * For each layout template, 18 layouts are created: 9 simple and 9 complex. The simple layouts are
 * there to create non-resizable views, while complex layouts are there to create resizable layouts
 * (i.e. layout with at least one dimension sets explicitly in dip).
 *
 * A layout should be of the form:
 * ```
 * <TargetView prop1="" ... />
 * ```
 *
 * For example, for the row:
 * ```
 * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:orientation="horizontal" />
 * ```
 *
 * The template should not define the view id, or the desired width and height of the view.
 */
internal class LayoutGenerator {

    private val documentBuilderFactory by lazy {
        DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
    }

    private val documentBuilder by lazy { documentBuilderFactory.newDocumentBuilder()!! }

    private val transformerFactory by lazy { TransformerFactory.newInstance() }

    fun parseLayoutTemplate(input: File): Document = documentBuilder.parse(input)

    fun writeGeneratedLayout(document: Document, output: File) {
        transformerFactory.newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            transform(DOMSource(document), StreamResult(output))
        }
    }

    /**
     * Generate files and return a mapping from File object to a structure defining useful
     * information extracted from the input.
     */
    fun generateAllFiles(
        containerFiles: List<File>,
        childrenFiles: List<File>,
        outputResourcesDir: File
    ): GeneratedFiles {
        val outputLayoutDir = outputResourcesDir.resolve("layout")
        val outputLayoutDirS = outputResourcesDir.resolve("layout-v31")
        val outputLayoutDirT = outputResourcesDir.resolve("layout-v33")
        val outputValueDir = outputResourcesDir.resolve("values")
        outputLayoutDir.mkdirs()
        outputLayoutDirS.mkdirs()
        outputLayoutDirT.mkdirs()
        outputValueDir.mkdirs()
        val generatedFiles =
            generateSizeLayouts(outputLayoutDir) +
                generateComplexLayouts(outputLayoutDir) +
                generateChildIds(outputValueDir) +
                generateContainersChildrenForS(outputLayoutDirS) +
                generateContainersChildrenBeforeS(outputLayoutDir) +
                generateRootElements(outputLayoutDir) +
                generateRootAliases(outputValueDir) +
                generateViewIds(outputValueDir)
        val topLevelLayouts = containerFiles + childrenFiles.filter { isTopLevelLayout(it) }
        return GeneratedFiles(
            generatedContainers =
                containerFiles.associateWith { generateContainers(it, outputLayoutDir) },
            generatedBoxChildren =
                topLevelLayouts.associateWith { generateBoxChildrenForT(it, outputLayoutDirT) },
            generatedRowColumnChildren =
                topLevelLayouts.associateWith {
                    generateRowColumnChildrenForT(it, outputLayoutDirT)
                },
            extraFiles = generatedFiles,
        )
    }

    private fun generateChildIds(outputValuesDir: File) =
        generateRes(outputValuesDir, "child_ids") {
            val containerSizes = listOf(ValidSize.Match, ValidSize.Wrap, ValidSize.Expand)
            val root = createElement("resources")
            appendChild(root)
            repeat(MaxChildCount) { pos ->
                forEachInCrossProduct(containerSizes, containerSizes) { width, height ->
                    val id = createElement("id")
                    root.appendChild(id)
                    id.attributes.apply {
                        setNamedItem(attribute("name", makeIdName(pos, width, height)))
                    }
                }
            }
        }

    private fun generateViewIds(outputValueDir: File) =
        generateRes(outputValueDir, "view_ids") {
            val root = createElement("resources")
            appendChild(root)
            repeat(TotalViewCount) {
                val id =
                    createElement("id").apply {
                        attributes.setNamedItem(attribute("name", makeViewIdResourceName(it)))
                    }
                root.appendChild(id)
            }
        }

    private fun generateContainersChildrenForS(outputLayoutDir: File) =
        generateContainersChildren(outputLayoutDir, listOf(ValidSize.Wrap))

    private fun generateContainersChildrenBeforeS(outputLayoutDir: File) =
        generateContainersChildren(outputLayoutDir, StubSizes)

    private fun generateContainersChildren(
        outputLayoutDir: File,
        containerSizes: List<ValidSize>,
    ) =
        ContainerOrientation.values()
            .flatMap { orientation ->
                generateContainersChildren(
                    outputLayoutDir,
                    containerSizes,
                    orientation,
                )
            }
            .toSet()

    private fun generateContainersChildren(
        outputLayoutDir: File,
        sizes: List<ValidSize>,
        containerOrientation: ContainerOrientation
    ): Set<File> {
        val widths = sizes + containerOrientation.extraWidths
        val heights = sizes + containerOrientation.extraHeights
        val alignments = containerOrientation.alignments
        return (0 until MaxChildCount)
            .flatMap { pos ->
                alignments.map { (horizontalAlignment, verticalAlignment) ->
                    generateRes(
                        outputLayoutDir,
                        makeChildResourceName(
                            pos,
                            containerOrientation,
                            horizontalAlignment,
                            verticalAlignment
                        )
                    ) {
                        val root = createElement("merge")
                        appendChild(root)
                        forEachInCrossProduct(widths, heights) { width, height ->
                            val childId = makeIdName(pos, width, height)
                            root.appendChild(
                                makeStub(
                                    childId,
                                    width,
                                    height,
                                    horizontalAlignment,
                                    verticalAlignment,
                                )
                            )
                        }
                    }
                }
            }
            .toSet()
    }

    private fun Document.makeStub(
        name: String,
        width: ValidSize,
        height: ValidSize,
        horizontalAlignment: HorizontalAlignment?,
        verticalAlignment: VerticalAlignment?
    ) =
        createElement("ViewStub").apply {
            attributes.apply {
                setNamedItemNS(androidId("@id/$name"))
                setNamedItemNS(androidWidth(width))
                setNamedItemNS(androidHeight(height))
                if (width == ValidSize.Expand || height == ValidSize.Expand) {
                    setNamedItemNS(androidWeight(1))
                }
                setNamedItemNS(
                    androidGravity(
                        listOfNotNull(
                                horizontalAlignment?.resourceName,
                                verticalAlignment?.resourceName
                            )
                            .joinToString(separator = "|")
                    )
                )
            }
        }

    private fun generateSizeLayouts(outputLayoutDir: File): Set<File> {
        val stubSizes = listOf(ValidSize.Wrap, ValidSize.Match)
        return mapInCrossProduct(stubSizes, stubSizes) { width, height ->
                generateRes(outputLayoutDir, "size_${width.resourceName}_${height.resourceName}") {
                    val root = createElement("TextView")
                    appendChild(root)
                    root.attributes.apply {
                        setNamedItem(androidNamespace)
                        setNamedItemNS(androidWidth(width))
                        setNamedItemNS(androidHeight(height))
                    }
                }
            }
            .toSet()
    }

    /**
     * Generate the various layouts needed for complex layouts.
     *
     * These layouts can be used with any view when it is not enough to have the naked view.
     * Currently, it only creates layouts to allow resizing views on API 30-. In the generated
     * layout, there is always a `ViewStub` with id `@id/glanceViewStub`, which needs to be replaced
     * with the actual view.
     *
     * The skeleton is:
     *
     * <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
     * android:id="@id/relativeLayout" android:layout_height="wrap_content"
     * android:layout_width="wrap_content"> <FrameLayout android:id="@id/sizeView"
     * android:layout_height="wrap_content" android:layout_width="wrap_content"> <ViewStub
     * android:id="@id/sizeViewStub" android:layout_height="wrap_content"
     * android:layout_width="wrap_content"/> </FrameLayout> <ViewStub
     * android:id="@id/glanceViewStub" android:layout_height="wrap_content"
     * android:layout_width="wrap_content"/> </RelativeLayout>
     *
     * With the `sizeView` frame layout only present if either dimension needs to be fixed.
     */
    private fun generateComplexLayouts(outputLayoutDir: File): Set<File> =
        mapConfiguration { width, height ->
                generateRes(outputLayoutDir, makeComplexResourceName(width, height)) {
                    val root = createElement("RelativeLayout")
                    appendChild(root)
                    root.attributes.apply {
                        setNamedItemNS(androidId("@id/relativeLayout"))
                        setNamedItemNS(androidAttr("tag", "glanceComplexLayout"))
                        setNamedItemNS(androidWidth(width))
                        setNamedItemNS(androidHeight(height))
                        if (width == ValidSize.Expand || height == ValidSize.Expand) {
                            setNamedItemNS(androidWeight(1))
                        }
                    }

                    if (width == ValidSize.Fixed || height == ValidSize.Fixed) {
                        // A sizing view is only required if the width or height are fixed.
                        val sizeView = createElement("FrameLayout")
                        root.appendChild(sizeView)
                        sizeView.attributes.apply {
                            setNamedItemNS(androidId("@id/sizeView"))
                            setNamedItemNS(androidAttr("visibility", "invisible"))
                            setNamedItemNS(androidWidth(ValidSize.Wrap))
                            setNamedItemNS(androidHeight(ValidSize.Wrap))
                        }
                        val sizeViewStub = createElement("ViewStub")
                        sizeView.appendChild(sizeViewStub)
                        sizeViewStub.attributes.apply {
                            setNamedItemNS(androidId("@id/sizeViewStub"))
                            setNamedItemNS(androidWidth(ValidSize.Wrap))
                            setNamedItemNS(androidHeight(ValidSize.Wrap))
                        }
                    }

                    val glanceViewStub = createElement("ViewStub")
                    root.appendChild(glanceViewStub)
                    glanceViewStub.attributes.apply {
                        setNamedItemNS(androidId("@id/glanceViewStub"))
                        when (width) {
                            ValidSize.Wrap -> setNamedItemNS(androidWidth(ValidSize.Wrap))
                            ValidSize.Match,
                            ValidSize.Expand -> {
                                setNamedItemNS(androidWidth(ValidSize.Match))
                            }
                            ValidSize.Fixed -> {
                                // If the view's height is fixed, its height is determined by
                                // sizeView.
                                // Use 0dp width for efficiency.
                                setNamedItemNS(androidWidth(ValidSize.Expand))
                                setNamedItemNS(androidAttr("layout_alignLeft", "@id/sizeView"))
                                setNamedItemNS(androidAttr("layout_alignRight", "@id/sizeView"))
                            }
                        }
                        when (height) {
                            ValidSize.Wrap -> setNamedItemNS(androidHeight(ValidSize.Wrap))
                            ValidSize.Match,
                            ValidSize.Expand -> {
                                setNamedItemNS(androidHeight(ValidSize.Match))
                            }
                            ValidSize.Fixed -> {
                                // If the view's height is fixed, its height is determined by
                                // sizeView.
                                // Use 0dp width for efficiency.
                                setNamedItemNS(androidHeight(ValidSize.Expand))
                                setNamedItemNS(androidAttr("layout_alignTop", "@id/sizeView"))
                                setNamedItemNS(androidAttr("layout_alignBottom", "@id/sizeView"))
                            }
                        }
                    }
                }
            }
            .toSet()

    private fun generateRootElements(outputLayoutDir: File): Set<File> =
        mapInCrossProduct(StubSizes, StubSizes) { width, height ->
                outputLayoutDir.resolveRes(makeRootResourceName(width, height)).also { output ->
                    writeGeneratedLayout(createRootElement(width, height), output)
                }
            }
            .toSet()

    private fun createRootElement(width: ValidSize, height: ValidSize) =
        documentBuilder.newDocument().apply {
            val root = createElement("FrameLayout")
            appendChild(root)
            root.attributes.apply {
                setNamedItemNS(androidId("@id/rootView"))
                setNamedItemNS(androidWidth(width))
                setNamedItemNS(androidHeight(height))
                setNamedItemNS(androidAttr("theme", "@style/Glance.AppWidget.Theme"))
            }
            val stub = createElement("ViewStub")
            root.appendChild(stub)
            stub.attributes.apply {
                setNamedItemNS(androidId("@id/rootStubId"))
                setNamedItemNS(androidWidth(width))
                setNamedItemNS(androidHeight(height))
            }
        }

    private fun generateContainers(
        file: File,
        outputLayoutDir: File,
    ): List<ContainerProperties> {
        val document = parseLayoutTemplate(file)
        val orientation =
            when (document.documentElement.androidAttr("orientation")?.nodeValue) {
                "horizontal" -> ContainerOrientation.Horizontal
                "vertical" -> ContainerOrientation.Vertical
                null -> ContainerOrientation.None
                else -> throw IllegalStateException("Unknown orientation in $file")
            }
        val alignments = orientation.alignments
        return (0..MaxChildCount).flatMap { numChildren ->
            alignments.map { (horizontalAlignment, verticalAlignment) ->
                val generated =
                    generateContainer(
                        document,
                        numChildren,
                        orientation,
                        horizontalAlignment,
                        verticalAlignment,
                    )
                val output =
                    outputLayoutDir.resolveRes(
                        makeContainerResourceName(
                            file,
                            numChildren,
                            horizontalAlignment,
                            verticalAlignment
                        )
                    )
                writeGeneratedLayout(generated, output)
                ContainerProperties(
                    output,
                    numChildren,
                    orientation,
                    horizontalAlignment,
                    verticalAlignment,
                )
            }
        }
    }

    private fun generateContainer(
        inputDoc: Document,
        numberChildren: Int,
        containerOrientation: ContainerOrientation,
        horizontalAlignment: HorizontalAlignment?,
        verticalAlignment: VerticalAlignment?,
    ) =
        documentBuilder.newDocument().apply {
            val root = importNode(inputDoc.documentElement, true)
            appendChild(root)
            root.attributes.apply {
                setNamedItemNS(androidWidth(ValidSize.Wrap))
                setNamedItemNS(androidHeight(ValidSize.Wrap))
            }
            for (pos in 0 until numberChildren) {
                root.appendChild(
                    createElement("include").apply {
                        attributes.apply {
                            setNamedItem(
                                attribute(
                                    "layout",
                                    "@layout/${
                                    makeChildResourceName(
                                        pos,
                                        containerOrientation,
                                        horizontalAlignment,
                                        verticalAlignment
                                    )
                                }"
                                )
                            )
                        }
                    }
                )
            }
        }

    private fun generateBoxChildrenForT(
        file: File,
        outputLayoutDir: File,
    ): List<BoxChildProperties> =
        crossProduct(HorizontalAlignment.values().toList(), VerticalAlignment.values().toList())
            .map { (horizontalAlignment, verticalAlignment) ->
                val generated =
                    generateAlignedLayout(
                        parseLayoutTemplate(file),
                        horizontalAlignment,
                        verticalAlignment,
                    )
                val output =
                    outputLayoutDir.resolveRes(
                        makeBoxChildResourceName(file, horizontalAlignment, verticalAlignment)
                    )
                writeGeneratedLayout(generated, output)
                BoxChildProperties(output, horizontalAlignment, verticalAlignment)
            }

    private fun generateRowColumnChildrenForT(
        file: File,
        outputLayoutDir: File,
    ): List<RowColumnChildProperties> =
        listOf(
                Pair(ValidSize.Expand, ValidSize.Wrap),
                Pair(ValidSize.Wrap, ValidSize.Expand),
            )
            .map { (width, height) ->
                val generated = generateSimpleLayout(parseLayoutTemplate(file), width, height)
                val output =
                    outputLayoutDir.resolveRes(makeRowColumnChildResourceName(file, width, height))
                writeGeneratedLayout(generated, output)
                RowColumnChildProperties(output, width, height)
            }

    /**
     * Generate a simple layout.
     *
     * A simple layout only contains the view itself, set up for a given width and height. On
     * Android R-, simple layouts are non-resizable.
     */
    private fun generateSimpleLayout(
        document: Document,
        width: ValidSize,
        height: ValidSize,
    ): Document {
        val generated = documentBuilder.newDocument()
        val root = generated.importNode(document.documentElement, true)
        generated.appendChild(root)
        root.attributes.apply {
            setNamedItem(generated.androidNamespace)
            if (root.androidId == null) {
                setNamedItemNS(generated.androidId("@id/glanceView"))
            }
            setNamedItemNS(generated.androidWidth(width))
            setNamedItemNS(generated.androidHeight(height))
            if (width == ValidSize.Expand || height == ValidSize.Expand) {
                setNamedItemNS(generated.androidWeight(1))
            }
            setNamedItemNS(generated.androidLayoutDirection("locale"))
        }
        return generated
    }

    /**
     * This function is used to generate FrameLayout children with "layout_gravity" set for Android
     * T+. We can ignore size here since it is set programmatically for T+.
     */
    private fun generateAlignedLayout(
        document: Document,
        horizontalAlignment: HorizontalAlignment,
        verticalAlignment: VerticalAlignment,
    ) =
        generateSimpleLayout(document, ValidSize.Wrap, ValidSize.Wrap).apply {
            documentElement.attributes.setNamedItemNS(
                androidGravity(
                    listOfNotNull(horizontalAlignment.resourceName, verticalAlignment.resourceName)
                        .joinToString(separator = "|")
                )
            )
        }

    private fun generateRootAliases(outputValueDir: File) =
        generateRes(outputValueDir, "layouts") {
            val root = createElement("resources")
            appendChild(root)
            val sizes = crossProduct(StubSizes, StubSizes)
            val numStubs = sizes.size
            repeat(RootLayoutAliasCount) { aliasIndex ->
                sizes.forEachIndexed() { index, (width, height) ->
                    val fullIndex = aliasIndex * numStubs + index
                    val alias =
                        createElement("item").apply {
                            attributes.apply {
                                setNamedItem(
                                    attribute("name", makeRootAliasResourceName(fullIndex))
                                )
                                setNamedItem(attribute("type", "layout"))
                            }
                            textContent = "@layout/${makeRootResourceName(width, height)}"
                        }
                    root.appendChild(alias)
                }
            }
        }

    private inline fun generateRes(
        outputDir: File,
        resName: String,
        builder: Document.() -> Unit
    ): File {
        val document = documentBuilder.newDocument()
        val file = outputDir.resolveRes(resName)
        builder(document)
        writeGeneratedLayout(document, file)
        return file
    }

    private fun isTopLevelLayout(file: File) =
        parseLayoutTemplate(file).run {
            documentElement.appAttr("glance_isTopLevelLayout")?.nodeValue == "true"
        }
}

/** Maximum number of children generated in containers. */
private const val MaxChildCount = 10

/**
 * Number of aliases for the root view. As pre-S we need four aliases per position, effectively 4
 * times that number will be generated.
 */
internal const val RootLayoutAliasCount = 100

/**
 * Number of View IDs that will be generated for use throughout the UI layout. This number
 * determines the maximum number of total views a layout may contain.
 */
internal const val TotalViewCount = 500

internal data class GeneratedFiles(
    val generatedContainers: Map<File, List<ContainerProperties>>,
    val generatedBoxChildren: Map<File, List<BoxChildProperties>>,
    val generatedRowColumnChildren: Map<File, List<RowColumnChildProperties>>,
    val extraFiles: Set<File>
)

internal data class ChildProperties(
    val childId: String,
    val width: ValidSize,
    val height: ValidSize,
)

internal data class ContainerProperties(
    val generatedFile: File,
    val numberChildren: Int,
    val containerOrientation: ContainerOrientation,
    val horizontalAlignment: HorizontalAlignment?,
    val verticalAlignment: VerticalAlignment?,
)

internal data class BoxChildProperties(
    val generatedFile: File,
    val horizontalAlignment: HorizontalAlignment,
    val verticalAlignment: VerticalAlignment,
)

internal data class RowColumnChildProperties(
    val generatedFile: File,
    val width: ValidSize,
    val height: ValidSize,
)

internal enum class ValidSize(val androidValue: String, val resourceName: String) {
    Wrap("wrap_content", "wrap"),
    Fixed("wrap_content", "fixed"),
    Match("match_parent", "match"),
    Expand("0dp", "expand"),
}

internal enum class ContainerOrientation(
    val resourceName: String,
    val extraWidths: List<ValidSize>,
    val extraHeights: List<ValidSize>
) {
    None("box", emptyList(), emptyList()),
    Horizontal("row", listOf(ValidSize.Expand), emptyList()),
    Vertical("column", emptyList(), listOf(ValidSize.Expand))
}

internal val ContainerOrientation.alignments: List<Pair<HorizontalAlignment?, VerticalAlignment?>>
    get() =
        when (this) {
            ContainerOrientation.None -> {
                crossProduct(
                    HorizontalAlignment.values().toList(),
                    VerticalAlignment.values().toList()
                )
            }
            ContainerOrientation.Horizontal -> VerticalAlignment.values().map { null to it }
            ContainerOrientation.Vertical -> HorizontalAlignment.values().map { it to null }
        }

internal enum class HorizontalAlignment(
    val resourceName: String,
    val code: MemberName,
) {
    Start("start", AlignmentStart),
    Center("center_horizontal", AlignmentCenterHorizontally),
    End("end", AlignmentEnd),
}

internal enum class VerticalAlignment(
    val resourceName: String,
    val code: MemberName,
) {
    Top("top", AlignmentTop),
    Center("center_vertical", AlignmentCenterVertically),
    Bottom("bottom", AlignmentBottom),
}

/** Sizes a ViewStub can meaningfully have, if expand is not an option. */
internal val StubSizes = listOf(ValidSize.Wrap, ValidSize.Match)

internal fun getChildMergeFilenameWithoutExtension(childCount: Int) = "merge_${childCount}child"

private val AndroidNS = "http://schemas.android.com/apk/res/android"
private val AppNS = "http://schemas.android.com/apk/res-auto"

internal fun Document.androidAttr(name: String, value: String) =
    createAttributeNS(AndroidNS, "android:$name").apply { textContent = value }

internal fun Node.androidAttr(name: String): Node? = attributes.getNamedItemNS(AndroidNS, name)

internal fun Node.appAttr(name: String): Node? = attributes.getNamedItemNS(AppNS, name)

internal fun Document.attribute(name: String, value: String): Node? =
    createAttribute(name).apply { textContent = value }

internal fun Document.androidId(value: String) = androidAttr("id", value)

internal val Node.androidId: Node?
    get() = androidAttr("id")

internal fun Document.androidWidth(value: ValidSize) =
    androidAttr("layout_width", value.androidValue)

internal fun Document.androidHeight(value: ValidSize) =
    androidAttr("layout_height", value.androidValue)

internal fun Document.androidWeight(value: Int) = androidAttr("layout_weight", value.toString())

internal fun Document.androidGravity(value: String) = androidAttr("layout_gravity", value)

internal fun Document.androidLayoutDirection(value: String) = androidAttr("layoutDirection", value)

internal val Document.androidNamespace
    get() = createAttribute("xmlns:android").apply { textContent = AndroidNS }
