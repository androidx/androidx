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

    fun extractMainViewId(document: Document) =
        document.documentElement.androidId?.textContent?.replace("@id/", "") ?: "glanceView"

    /**
     * Generate files and return a mapping from File object to a structure defining useful
     * information extracted from the input.
     */
    fun generateAllFiles(files: List<File>, outputResourcesDir: File): GeneratedFiles {
        val outputLayoutDir = outputResourcesDir.resolve("layout")
        outputLayoutDir.mkdirs()
        val generatedFiles = generateSizeLayouts(outputLayoutDir) +
            generateComplexLayouts(outputLayoutDir)
        return GeneratedFiles(
            generatedLayouts = files.associateWith { generateForFile(it, outputLayoutDir) },
            extraFiles = generatedFiles,
        )
    }

    private fun generateSizeLayouts(outputLayoutDir: File): Set<File> {
        val stubSizes = listOf(ValidSize.Wrap, ValidSize.Match)
        return mapInCrossProduct(stubSizes, stubSizes) { width, height ->
            val fileName = "size_${width.resourceName}_${height.resourceName}.xml"
            generateFile(outputLayoutDir, fileName) {
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
            val fileName = makeComplexResourceName(width, height) + ".xml"
            generateFile(outputLayoutDir, fileName) {
                val root = createElement("RelativeLayout")
                appendChild(root)
                root.attributes.apply {
                    setNamedItemNS(androidId("@id/relativeLayout"))
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
                    if (width == ValidSize.Fixed) {
                        setNamedItemNS(androidAttr("layout_alignLeft", "@id/sizeView"))
                        setNamedItemNS(androidAttr("layout_alignRight", "@id/sizeView"))
                    }
                }
            }
        }.toSet()

    private fun generateForFile(file: File, outputLayoutDir: File): LayoutProperties {
        val document = parseLayoutTemplate(file)
        val generatedFiles = mapConfiguration { width, height ->
            outputLayoutDir.resolve(
                makeSimpleResourceName(file, width, height) + ".xml"
            ).also {
                writeGeneratedLayout(generateSimpleLayout(document, width, height), it)
            }
        }
        return LayoutProperties(
            mainViewId = extractMainViewId(document),
            generatedFiles = generatedFiles
        )
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

    private inline fun generateFile(
        outputLayoutDir: File,
        filename: String,
        builder: Document.() -> Unit
    ): File {
        val document = documentBuilder.newDocument()
        val file = outputLayoutDir.resolve(filename)
        builder(document)
        writeGeneratedLayout(document, file)
        return file
    }
}

internal data class GeneratedFiles(
    val generatedLayouts: Map<File, LayoutProperties>,
    val extraFiles: Set<File>
)

internal data class LayoutProperties(
    val mainViewId: String,
    val generatedFiles: List<File>
)

internal enum class ValidSize(val androidValue: String, val resourceName: String) {
    Wrap("wrap_content", "wrap"),
    Fixed("wrap_content", "fixed"),
    Match("match_parent", "match"),
    Expand("0dp", "expand"),
}

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
