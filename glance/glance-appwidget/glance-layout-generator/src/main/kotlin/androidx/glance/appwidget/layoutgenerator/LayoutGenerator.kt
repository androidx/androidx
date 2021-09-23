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
    fun generateAllFiles(files: List<File>, outputResourcesDir: File): Map<File, LayoutProperties> {
        val outputLayoutDir = outputResourcesDir.resolve("layout")
        outputLayoutDir.mkdirs()
        return files.associate {
            it to generateForFile(it, outputLayoutDir)
        }
    }

    private fun generateForFile(file: File, outputLayoutDir: File): LayoutProperties {
        val document = parseLayoutTemplate(file)
        ValidSize.values().forEach { width ->
            ValidSize.values().forEach { height ->
                val simpleLayout = generateSimpleLayout(document, width, height)
                writeGeneratedLayout(
                    simpleLayout,
                    outputLayoutDir.resolve("${makeSimpleResourceName(file, width, height)}.xml")
                )
                val complexLayout = generateComplexLayout(document, width, height)
                writeGeneratedLayout(
                    complexLayout,
                    outputLayoutDir.resolve("${makeComplexResourceName(file, width, height)}.xml")
                )
            }
        }
        return LayoutProperties(mainViewId = extractMainViewId(document))
    }

    /**
     * Generate a simple layout.
     *
     * A simple layout only contains the view itself, set up for a given width and height.
     * On Android R-, simple layouts are non-resizable.
     */
    fun generateSimpleLayout(
        document: Document,
        width: ValidSize,
        height: ValidSize
    ): Document {
        val generated = documentBuilder.newDocument()
        val root = generated.importNode(document.documentElement, true)
        generated.appendChild(generated.createComment(LicenseComment))
        generated.appendChild(root)
        root.attributes.apply {
            setNamedItem(generated.androidNamespace)
            if (root.androidId == null) {
                setNamedItemNS(generated.androidId("@id/glanceView"))
            }
            setNamedItemNS(generated.androidWidth(width))
            setNamedItemNS(generated.androidHeight(height))
            if (width == ValidSize.Expand || height == ValidSize.Expand) {
                setNamedItemNS(generated.androidWeight("1"))
            }
        }
        return generated
    }

    /**
     * Generate a complex layout.
     *
     * A complex layout contains a RelativeLayout containing the target view and a TextView,
     * which will be used to resize the target view.
     *
     * Complex layouts are always resizable.
     *
     * The complex layouts follow the following pattern:
     *
     * ```
     * <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
     *     android:id="@id/relativeLayout"
     *     android:layout_height="match_parent"
     *     android:layout_width="wrap_content">
     *
     *   <TextView
     *       android:id="@id/sizeView"
     *       android:layout_height="match_parent"
     *       android:layout_width="wrap_content"/>
     *
     *   <TargetView
     *       android:id="@id/glanceView"
     *       android:layout_height="wrap_content"
     *       android:layout_width="wrap_content"
     *       android:layout_alignBottom="@id/sizeView"
     *       android:layout_alignLeft="@id/sizeView"
     *       android:layout_alignRight="@id/sizeView"
     *       android:layout_alignTop="@id/sizeView" />
     * </RelativeLayout>
     * ```
     *
     * The width and height of the target view are always set to `wrap_content`, so
     * `wrap_content` on the `TextView` works properly.
     *
     * The width and height on the `RelativeLayout` are set to be what we want to achieve. If the
     * desired dimension is `Expand`, a weight of 1 is specified. This implies we cannot let the
     * developer control the proportion of the space used by a particular view, but there is no
     * way to control this programmatically, and allowing even a few standard values would
     * increase drastically the number of generated layouts.
     *
     * The width and height of the `TextView` are `match_parent` by default, unless the desired
     * dimension is `wrap_content`, in which case it is also set to `wrap_content`.
     */
    fun generateComplexLayout(
        document: Document,
        width: ValidSize,
        height: ValidSize
    ): Document {
        val generated = documentBuilder.newDocument()
        val root = generated.createElement("RelativeLayout")
        generated.appendChild(generated.createComment(LicenseComment))
        generated.appendChild(root)
        root.attributes.apply {
            setNamedItemNS(generated.androidId("@id/relativeLayout"))
            setNamedItemNS(generated.androidWidth(width))
            setNamedItemNS(generated.androidHeight(height))
            if (width == ValidSize.Expand || height == ValidSize.Expand) {
                setNamedItemNS(generated.androidWeight("1"))
            }
        }
        val textView = generated.createElement("TextView")
        root.appendChild(textView)
        val textWidth = if (width != ValidSize.Wrap) ValidSize.Match else ValidSize.Wrap
        val textHeight = if (height != ValidSize.Wrap) ValidSize.Match else ValidSize.Wrap
        textView.attributes.apply {
            setNamedItemNS(generated.androidId("@id/sizeView"))
            setNamedItemNS(generated.androidWidth(textWidth))
            setNamedItemNS(generated.androidHeight(textHeight))
        }
        val mainNode = generated.importNode(document.documentElement, true)
        root.appendChild(mainNode)
        mainNode.attributes.apply {
            if (mainNode.androidId == null) {
                setNamedItemNS(generated.androidId("@id/glanceView"))
            }
            setNamedItemNS(generated.androidWidth(ValidSize.Wrap))
            setNamedItemNS(generated.androidHeight(ValidSize.Wrap))
            setNamedItemNS(generated.androidAttr("layout_alignLeft", "@id/sizeView"))
            setNamedItemNS(generated.androidAttr("layout_alignRight", "@id/sizeView"))
            setNamedItemNS(generated.androidAttr("layout_alignTop", "@id/sizeView"))
            setNamedItemNS(generated.androidAttr("layout_alignBottom", "@id/sizeView"))
        }
        return generated
    }
}

internal data class LayoutProperties(
    val mainViewId: String
)

internal enum class ValidSize(val androidValue: String, val resourceName: String) {
    Wrap("wrap_content", "wrap"),
    Match("match_parent", "match"),
    Expand("0dp", "expand")
}

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

internal fun Document.androidWeight(value: String) = androidAttr("layout_weight", value)

internal val Document.androidNamespace
    get() = createAttribute("xmlns:android").apply {
        textContent = AndroidNS
    }
