/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.metadata.AppFunctionMetadataDocument
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.Path
import org.w3c.dom.Document
import org.w3c.dom.Element

class AppFunctionLegacySchemaXmlGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    fun generateLegacyIndexXml(
        serviceEntryPoint: AnnotatedAppFunctionServiceEntryPoint,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) {
        val appFunctionMetadataList =
            serviceEntryPoint.appFunctions.map { appFunction ->
                appFunction
                    .createAppFunctionMetadata(
                        enclosingClass = serviceEntryPoint.serviceDeclaration,
                        resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                    )
                    .toAppFunctionMetadataDocument()
            }
        writeXmlFile(
            appFunctionMetadataList,
            Dependencies(
                aggregating = true,
                sources = serviceEntryPoint.getSourceFiles().toTypedArray(),
            ),
            serviceEntryPoint.appFunctionXmlFileName + V1_XML_SUFFIX,
            exportLocation = null,
        )
    }

    /**
     * Generates AppFunction's legacy index XML files for v1 indexer in App Search.
     *
     * @param appFunctionsByClass a collection of functions annotated with @AppFunction grouped by
     *   their enclosing classes.
     */
    fun generateLegacyIndexXml(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        exportLocation: String?,
    ) {
        val appFunctionMetadataList =
            appFunctionsByClass.flatMap { annotatedAppFunctions ->
                annotatedAppFunctions
                    .createAppFunctionMetadataList(resolvedAnnotatedSerializableProxies)
                    .map { it.toAppFunctionMetadataDocument() }
            }
        writeXmlFile(
            appFunctionMetadataList,
            Dependencies(
                aggregating = true,
                *appFunctionsByClass.flatMap { it.getSourceFiles() }.toTypedArray(),
            ),
            XML_FILE_NAME,
            exportLocation,
        )
    }

    private fun writeXmlFile(
        appFunctionMetadataList: List<AppFunctionMetadataDocument>,
        dependencies: Dependencies,
        fileName: String,
        exportLocation: String?,
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

        if (exportLocation != null) {
            try {
                XmlFileResolver.RESOLVER.getWriteStream(
                        filePath =
                            Path(exportLocation).resolve("${XML_FILE_NAME}.${XML_EXTENSION}"),
                        logger,
                    )
                    .use { stream ->
                        transformer.transform(DOMSource(xmlDocument), StreamResult(stream))
                    }
            } catch (e: IOException) {
                throw ProcessingException(
                    "Failed to create AppFunctions XML file at: $exportLocation",
                    null,
                    e,
                )
            }
        }

        codeGenerator.createNewFile(dependencies, XML_PACKAGE_NAME, fileName, XML_EXTENSION).use {
            stream ->
            transformer.transform(DOMSource(xmlDocument), StreamResult(stream))
        }
    }

    private fun Document.createAppFunctionElement(
        appFunctionMetadata: AppFunctionMetadataDocument
    ): Element =
        createElement(XmlElement.APP_FUNCTION_ITEM_TAG).apply {
            appendChild(
                createElementWithTextNode(XmlElement.APP_FUNCTION_ID_TAG, appFunctionMetadata.id)
            )

            val schemaName = appFunctionMetadata.schemaName
            val schemaCategory = appFunctionMetadata.schemaCategory
            val schemaVersion = appFunctionMetadata.schemaVersion
            if (schemaName != null && schemaCategory != null && schemaVersion != null) {
                appendChild(
                    createElementWithTextNode(
                        XmlElement.APP_FUNCTION_SCHEMA_CATEGORY_TAG,
                        schemaCategory,
                    )
                )
                appendChild(
                    createElementWithTextNode(XmlElement.APP_FUNCTION_SCHEMA_NAME_TAG, schemaName)
                )
                appendChild(
                    createElementWithTextNode(
                        XmlElement.APP_FUNCTION_SCHEMA_VERSION_TAG,
                        schemaVersion.toString(),
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

    private companion object {
        private const val XML_PACKAGE_NAME = "assets"
        private const val XML_FILE_NAME = "app_functions"
        private const val XML_EXTENSION = "xml"
        private const val V1_XML_SUFFIX = "-v1"

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
