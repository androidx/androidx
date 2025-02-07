/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.metadata.AppFunctionMetadata
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.KProperty1
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Generates AppFunction's index xml file with all properties of [AppFunctionMetadata] for the
 * AppSearch indexer to index.
 *
 * The generator would write an XML file as `/assets/app_functions_dynamic_schema.xml`. The file
 * would be packaged into the APK's asset when assembled, so that the AppSearch indexer can look up
 * the asset and inject metadata into platform AppSearch database accordingly.
 */
class AppFunctionIndexXmlProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        generateIndexXml(AppFunctionSymbolResolver(resolver).resolveAnnotatedAppFunctions())
        return emptyList()
    }

    /**
     * Generates AppFunction's index xml files for indexer in App Search.
     *
     * @param appFunctionsByClass a collection of functions annotated with @AppFunction grouped by
     *   their enclosing classes.
     */
    private fun generateIndexXml(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
    ) {
        if (appFunctionsByClass.isEmpty()) {
            return
        }
        writeXmlFile(appFunctionsByClass)
    }

    private fun writeXmlFile(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
    ) {
        val appFunctionMetadataList =
            appFunctionsByClass.flatMap {
                it.createAppFunctionMetadataList().map { it.toAppFunctionMetadataDocument() }
            }

        val xmlDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xmlDocument = xmlDocumentBuilder.newDocument().apply { xmlStandalone = true }

        val appFunctionsElement = xmlDocument.createElement(APP_FUNCTIONS_ELEMENTS_TAG)
        xmlDocument.appendChild(appFunctionsElement)

        for (appFunctionMetadata in appFunctionMetadataList) {
            val appFunctionElement =
                xmlDocument.createElementWithInstance(
                    APP_FUNCTION_ITEM_TAG,
                    appFunctionMetadata,
                    // Below properties are named differently in platform's
                    // AppFunctionStaticMetadata GD hence we encode them in XML accordingly.
                    customTagNames = mapOf("isEnabledByDefault" to "enabledByDefault"),
                    // Irrelevant properties that do not need to be encoded in XML.
                    skipProperties = setOf("namespace")
                )
            appFunctionElement.appendChild(
                xmlDocument.createElementWithTextNode(APP_FUNCTION_ID_TAG, appFunctionMetadata.id)
            )
            appFunctionsElement.appendChild(appFunctionElement)
        }

        val transformer =
            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                setOutputProperty(OutputKeys.VERSION, "1.0")
                setOutputProperty(OutputKeys.STANDALONE, "yes")
            }

        codeGenerator
            .createNewFile(
                Dependencies(
                    aggregating = true,
                    *appFunctionsByClass.flatMap { it.getSourceFiles() }.toTypedArray()
                ),
                XML_PACKAGE_NAME,
                XML_FILE_NAME,
                XML_EXTENSION
            )
            .use { stream -> transformer.transform(DOMSource(xmlDocument), StreamResult(stream)) }
    }

    /**
     * Creates an XML element from [instance], including nested structures and collections.
     *
     * This function recursively converts a data class instance into an XML element, handling nested
     * data classes and collections appropriately. For non-data-class values, it creates text nodes.
     *
     * @param elementName The name of the root XML element to create.
     * @param instance The instance to convert into an XML structure.
     * @param customTagNames Mapping of property names to customized tag names when creating nodes.
     * @param skipProperties Property names to skip when creating XML elements.
     * @return The created XML element representing the instance.
     */
    private fun Document.createElementWithInstance(
        elementName: String,
        instance: Any,
        customTagNames: Map<String, String>,
        skipProperties: Set<String>,
    ): Element {
        if (instance.isPrimitiveType()) {
            return createElementWithTextNode(elementName, instance.toString())
        }

        val doc = this
        val element = createElement(elementName)

        for (property in instance::class.members.filterIsInstance<KProperty1<Any, *>>()) {

            if (property.name in skipProperties) continue

            val value = property.get(instance) ?: continue
            val propertyName = customTagNames[property.name] ?: property.name

            when {
                value is List<*> ->
                    value
                        .filterNotNull()
                        .map { item ->
                            doc.createElementWithInstance(
                                propertyName,
                                item,
                                customTagNames,
                                skipProperties
                            )
                        }
                        .forEach(element::appendChild)
                else ->
                    element.appendChild(
                        doc.createElementWithInstance(
                            propertyName,
                            value,
                            customTagNames,
                            skipProperties
                        )
                    )
            }
        }

        return element
    }

    private fun Any.isPrimitiveType(): Boolean {
        return this is Byte ||
            this is Short ||
            this is Int ||
            this is Long ||
            this is Float ||
            this is Double ||
            this is Char ||
            this is Boolean ||
            this is String
    }

    private fun Document.createElementWithTextNode(elementName: String, text: String): Element =
        createElement(elementName).apply { appendChild(createTextNode(text)) }

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
        const val XML_FILE_NAME = "app_functions_dynamic_schema"
        const val XML_EXTENSION = "xml"
        const val APP_FUNCTIONS_ELEMENTS_TAG = "appfunctions"
        const val APP_FUNCTION_ITEM_TAG = "appfunction"
        const val APP_FUNCTION_ID_TAG = "functionId"
    }
}
