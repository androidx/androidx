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
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
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
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Generates AppFunction's index xml file for the legacy AppSearch indexer to index.
 *
 * The generator would write an XML file as `/assets/app_functions.xml`. The file would be packaged
 * into the APK's asset when assembled. So that the AppSearch indexer can look up the asset and
 * inject metadata into platform AppSearch database accordingly.
 *
 * The new indexer will index additional properties based on the schema defined in SDK instead of
 * the pre-defined one in AppSearch.
 */
class AppFunctionLegacyIndexXmlProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        generateLegacyIndexXml(AppFunctionSymbolResolver(resolver).resolveAnnotatedAppFunctions())
        return emptyList()
    }

    /**
     * Generates AppFunction's legacy index xml files for v1 indexer in App Search.
     *
     * @param appFunctionsByClass a collection of functions annotated with @AppFunction grouped by
     *   their enclosing classes.
     */
    private fun generateLegacyIndexXml(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
    ) {
        if (appFunctionsByClass.isEmpty()) {
            return
        }
        val appFunctionMetadataList =
            appFunctionsByClass.flatMap {
                it.createAppFunctionMetadataList().map { it.toAppFunctionMetadataDocument() }
            }
        writeXmlFile(appFunctionMetadataList, appFunctionsByClass)
    }

    private fun writeXmlFile(
        appFunctionMetadataList: List<AppFunctionMetadataDocument>,
        appFunctionsByClass: List<AnnotatedAppFunctions>,
    ) {
        val xmlDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xmlDocument = xmlDocumentBuilder.newDocument().apply { xmlStandalone = true }

        val appFunctionsElement = xmlDocument.createElement(XmlElement.APP_FUNCTIONS_ELEMENTS_TAG)
        xmlDocument.appendChild(appFunctionsElement)

        for (appFunctionMetadata in appFunctionMetadataList) {
            appFunctionsElement.appendChild(
                xmlDocument.createAppFunctionElement(appFunctionMetadata)
            )
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

    private fun Document.createAppFunctionElement(
        appFunctionMetadata: AppFunctionMetadataDocument
    ): Element =
        createElement(XmlElement.APP_FUNCTION_ITEM_TAG).apply {
            appendChild(
                createElementWithTextNode(XmlElement.APP_FUNCTION_ID_TAG, appFunctionMetadata.id)
            )

            val schemaDetail = appFunctionMetadata.schema
            if (schemaDetail != null) {
                appendChild(
                    createElementWithTextNode(
                        XmlElement.APP_FUNCTION_SCHEMA_CATEGORY_TAG,
                        schemaDetail.schemaCategory,
                    )
                )
                appendChild(
                    createElementWithTextNode(
                        XmlElement.APP_FUNCTION_SCHEMA_NAME_TAG,
                        schemaDetail.schemaName,
                    )
                )
                appendChild(
                    createElementWithTextNode(
                        XmlElement.APP_FUNCTION_SCHEMA_VERSION_TAG,
                        schemaDetail.schemaVersion.toString(),
                    )
                )
            }
            appendChild(
                createElementWithTextNode(
                    XmlElement.APP_FUNCTION_ENABLE_BY_DEFAULT_TAG,
                    appFunctionMetadata.isEnabledByDefault.toString(),
                )
            )
        }

    private fun Document.createElementWithTextNode(elementName: String, text: String): Element =
        createElement(elementName).apply { appendChild(createTextNode(text)) }

    private companion object {
        private const val XML_PACKAGE_NAME = "assets"
        private const val XML_FILE_NAME = "app_functions"
        private const val XML_EXTENSION = "xml"

        private object XmlElement {
            const val APP_FUNCTIONS_ELEMENTS_TAG = "appfunctions"
            const val APP_FUNCTION_ITEM_TAG = "appfunction"
            const val APP_FUNCTION_ID_TAG = "function_id"
            const val APP_FUNCTION_SCHEMA_CATEGORY_TAG = "schema_category"
            const val APP_FUNCTION_SCHEMA_NAME_TAG = "schema_name"
            const val APP_FUNCTION_SCHEMA_VERSION_TAG = "schema_version"
            const val APP_FUNCTION_ENABLE_BY_DEFAULT_TAG = "enabled_by_default"
        }
    }
}
