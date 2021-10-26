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

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Generate the layouts from the templates provided to the task.
 *
 * For each layout template, 18 layouts are created: 9 simple and 9 complex. The simple layouts
 * are there to create non-resizable views, while complex layouts are there to create resizable
 * layouts (i.e. layout with at least one dimension sets explicitly in dip).
 *
 * A layout should be of the form:
 *
 * ```
 * <TargetView prop1="" ... />
 * ```
 *
 * For example, for the row:
 *
 * ```
 * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:orientation="horizontal" />
 * ```
 *
 * The template should not define the view id, or the desired width and height of the view.
 */
internal class LayoutGenerator {

    private val documentBuilderFactory by lazy {
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
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
        files: List<File>,
        outputResourcesDir: File
    ): GeneratedFiles {
        val outputLayoutDir = outputResourcesDir.resolve("layout")
        val outputLayoutDirS = outputResourcesDir.resolve("layout-v31")
        val outputValueDir = outputResourcesDir.resolve("values")
        outputLayoutDir.mkdirs()
        outputLayoutDirS.mkdirs()
        outputValueDir.mkdirs()
        val generatedFiles = generateSizeLayouts(outputLayoutDir) +
            generateComplexLayouts(outputLayoutDir) +
            generateChildIds(outputValueDir) +
            generateContainersChildrenForS(outputLayoutDirS) +
            generateContainersChildrenForR(outputLayoutDir) +
            generateRootElements(outputLayoutDir)
        return GeneratedFiles(
            generatedContainers = files.associateWith {
                generateContainers(it, outputLayoutDir)
            },
            extraFiles = generatedFiles,
        )
    }

    private fun generateChildIds(outputValuesDir: File): Set<File> {
        val fileName = outputValuesDir.resolveRes("ids")
        val containerSizes = listOf(ValidSize.Match, ValidSize.Wrap, ValidSize.Expand)
        val generated = documentBuilder.newDocument().apply {
            val root = createElement("resources")
            appendChild(root)
            repeat(MaxChildCount) { pos ->
                forEachInCrossProduct(containerSizes, containerSizes) { width, height ->
                    val id = createElement("id")
                    root.appendChild(id)
                    id.attributes.apply {
                        setNamedItem(createAttribute("name").apply {
                            textContent = makeIdName(pos, width, height)
                        })
                    }
                }
            }
        }
        writeGeneratedLayout(generated, fileName)
        return setOf(fileName)
    }

    private fun generateContainersChildrenForS(outputLayoutDir: File) =
        generateContainersChildren(outputLayoutDir, listOf(ValidSize.Wrap))

    private fun generateContainersChildrenForR(outputLayoutDir: File) =
        generateContainersChildren(outputLayoutDir, StubSizes)

    private fun generateContainersChildren(
        outputLayoutDir: File,
        containerSizes: List<ValidSize>,
    ) = ContainerOrientation.values().flatMap { orientation ->
        generateContainersChildren(
            outputLayoutDir,
            containerSizes,
            orientation,
        )
    }.toSet()

    private fun generateContainersChildren(
        outputLayoutDir: File,
        sizes: List<ValidSize>,
        containerOrientation: ContainerOrientation
    ): Set<File> {
        val widths = sizes + containerOrientation.extraWidths
        val heights = sizes + containerOrientation.extraHeights
        return (0 until MaxChildCount).map { pos ->
            generateRes(outputLayoutDir, makeChildrenResourceName(pos, containerOrientation)) {
                val root = createElement("merge")
                appendChild(root)
                forEachInCrossProduct(widths, heights) { width, height ->
                    val childId = makeIdName(pos, width, height)
                    root.appendChild(makeStub(childId, width, height))
                }
            }
        }.toSet()
    }

    private fun Document.makeStub(name: String, width: ValidSize, height: ValidSize) =
        createElement("ViewStub").apply {
            attributes.apply {
                setNamedItemNS(androidId("@id/$name"))
                setNamedItemNS(androidWidth(width))
                setNamedItemNS(androidHeight(height))
                if (width == ValidSize.Expand || height == ValidSize.Expand) {
                    setNamedItemNS(androidWeight(1))
                }
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
        }.toSet()
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
     *      android:id="@id/relativeLayout"
     *      android:layout_height="wrap_content"
     *      android:layout_width="wrap_content">
     *   <FrameLayout
     *       android:id="@id/sizeView"
     *       android:layout_height="wrap_content"
     *       android:layout_width="wrap_content">
     *     <ViewStub
     *         android:id="@id/sizeViewStub"
     *         android:layout_height="wrap_content"
     *         android:layout_width="wrap_content"/>
     *   </FrameLayout>
     *   <ViewStub android:id="@id/glanceViewStub"
     *       android:layout_height="wrap_content"
     *       android:layout_width="wrap_content"/>
     * </RelativeLayout>
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
                        ValidSize.Match, ValidSize.Expand -> {
                            setNamedItemNS(androidWidth(ValidSize.Match))
                        }
                        ValidSize.Fixed -> {
                            // If the view's height is fixed, its height is determined by sizeView.
                            // Use 0dp width for efficiency.
                            setNamedItemNS(androidWidth(ValidSize.Expand))
                            setNamedItemNS(androidAttr("layout_alignLeft", "@id/sizeView"))
                            setNamedItemNS(androidAttr("layout_alignRight", "@id/sizeView"))
                        }
                    }
                    when (height) {
                        ValidSize.Wrap -> setNamedItemNS(androidHeight(ValidSize.Wrap))
                        ValidSize.Match, ValidSize.Expand -> {
                            setNamedItemNS(androidHeight(ValidSize.Match))
                        }
                        ValidSize.Fixed -> {
                            // If the view's height is fixed, its height is determined by sizeView.
                            // Use 0dp width for efficiency.
                            setNamedItemNS(androidHeight(ValidSize.Expand))
                            setNamedItemNS(androidAttr("layout_alignTop", "@id/sizeView"))
                            setNamedItemNS(androidAttr("layout_alignBottom", "@id/sizeView"))
                        }
                    }
                }
            }
        }.toSet()

    private fun generateRootElements(outputLayoutDir: File): Set<File> =
        mapInCrossProduct(StubSizes, StubSizes) { width, height ->
            outputLayoutDir.resolveRes(makeRootResourceName(width, height)).also { output ->
                writeGeneratedLayout(createRootElement(width, height), output)
            }
        }.toSet()

    private fun createRootElement(width: ValidSize, height: ValidSize) =
        documentBuilder.newDocument().apply {
            val root = createElement("FrameLayout")
            appendChild(root)
            root.attributes.apply {
                setNamedItemNS(androidId("@id/rootView"))
                setNamedItemNS(androidWidth(width))
                setNamedItemNS(androidHeight(height))
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
        return (0..MaxChildCount).map { numChildren ->
            val generated = generateContainer(
                document,
                numChildren,
                orientation,
            )
            val output =
                outputLayoutDir.resolveRes(makeContainerResourceName(file, numChildren))
            writeGeneratedLayout(generated, output)
            ContainerProperties(
                output,
                numChildren,
                orientation
            )
        }
    }

    private fun generateContainer(
        inputDoc: Document,
        numberChildren: Int,
        containerOrientation: ContainerOrientation,
    ) = documentBuilder.newDocument().apply {
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
                            createAttribute("layout").apply {
                                textContent =
                                    "@layout/${makeChildrenResourceName(pos, containerOrientation)}"
                            }
                        )
                    }
                }
            )
        }
    }

    /**
     * Generate a simple layout.
     *
     * A simple layout only contains the view itself, set up for a given width and height.
     * On Android R-, simple layouts are non-resizable.
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

    private inline fun generateRes(
        outputLayoutDir: File,
        resName: String,
        builder: Document.() -> Unit
    ): File {
        val document = documentBuilder.newDocument()
        val file = outputLayoutDir.resolveRes(resName)
        builder(document)
        writeGeneratedLayout(document, file)
        return file
    }
}

/** Maximum number of children generated in containers. */
private const val MaxChildCount = 10

internal data class GeneratedFiles(
    val generatedContainers: Map<File, List<ContainerProperties>>,
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

/** Sizes a ViewStub can meaningfully have, if expand is not an option. */
internal val StubSizes = listOf(ValidSize.Wrap, ValidSize.Match)

internal fun getChildMergeFilenameWithoutExtension(childCount: Int) = "merge_${childCount}child"

private val AndroidNS = "http://schemas.android.com/apk/res/android"

internal fun Document.androidAttr(name: String, value: String) =
    createAttributeNS(AndroidNS, "android:$name").apply {
        textContent = value
    }

internal fun Node.androidAttr(name: String): Node? =
    attributes.getNamedItemNS(AndroidNS, name)

internal fun Document.androidId(value: String) = androidAttr("id", value)

internal val Node.androidId: Node?
    get() = androidAttr("id")

internal fun Document.androidWidth(value: ValidSize) =
    androidAttr("layout_width", value.androidValue)

internal fun Document.androidHeight(value: ValidSize) =
    androidAttr("layout_height", value.androidValue)

internal fun Document.androidWeight(value: Int) = androidAttr("layout_weight", value.toString())

internal fun Document.androidLayoutDirection(value: String) =
    androidAttr("layoutDirection", value)

internal fun Document.include(layout: String) =
    createAttribute("layout").apply {
        textContent = layout
    }

internal val Document.androidNamespace
    get() = createAttribute("xmlns:android").apply {
        textContent = AndroidNS
    }
