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
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
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

/** Generator for AppFunction XML files. */
class AppFunctionXmlGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    /**
     * Generates an XML file containing the AppFunction metadata.
     *
     * @param entryPoint The [AnnotatedAppFunctionEntryPoint] containing the app functions to be
     *   included in the XML.
     * @param resolvedAnnotatedSerializableProxies A collection of resolved annotated serializable
     *   proxies.
     * @param appFunctionSerializablesDescriptionMap A map containing descriptions of AppFunction
     *   serializables.
     * @param packageName The package name where the XML file should be generated.
     * @param fileName The name of the generated XML file.
     * @param outputLocation An optional custom path to save the XML file in addition to the
     *   standard KSP output.
     */
    fun generateXml(
        entryPoint: AnnotatedAppFunctionEntryPoint,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        appFunctionSerializablesDescriptionMap: Map<String, String>,
        packageName: String,
        fileName: String,
        outputLocation: String? = null,
    ) {
        val appFunctionMetadataList =
            entryPoint.appFunctions.map {
                it.createAppFunctionMetadata(
                    enclosingClass = entryPoint.serviceDeclaration,
                    resolvedAnnotatedSerializableProxies = resolvedAnnotatedSerializableProxies,
                    sharedDataTypeDescriptionMap = appFunctionSerializablesDescriptionMap,
                )
            }
        writeXml(
            appFunctionMetadataList = appFunctionMetadataList,
            dependencies =
                Dependencies(
                    aggregating = true,
                    sources = entryPoint.getSourceFiles().toTypedArray(),
                ),
            packageName = packageName,
            fileName = fileName,
            outputLocation = outputLocation,
        )
    }

    /**
     * Generates an XML file containing the AppFunction metadata.
     *
     * @param appFunctionsByClass A list of [AnnotatedAppFunctions] to be included in the XML.
     * @param resolvedAnnotatedSerializableProxies A collection of resolved annotated serializable
     *   proxies.
     * @param appFunctionSerializablesDescriptionMap A map containing descriptions of AppFunction
     *   serializables.
     * @param packageName The package name where the XML file should be generated.
     * @param fileName The name of the generated XML file.
     * @param outputLocation An optional custom path to save the XML file in addition to the
     *   standard KSP output.
     */
    fun generateXml(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
        appFunctionSerializablesDescriptionMap: Map<String, String>,
        packageName: String,
        fileName: String,
        outputLocation: String? = null,
    ) {
        val appFunctionMetadataList =
            appFunctionsByClass.flatMap {
                it.createAppFunctionMetadataList(
                    resolvedAnnotatedSerializableProxies,
                    appFunctionSerializablesDescriptionMap,
                )
            }
        writeXml(
            appFunctionMetadataList = appFunctionMetadataList,
            dependencies =
                Dependencies(
                    aggregating = true,
                    *appFunctionsByClass.flatMap { it.getSourceFiles() }.toTypedArray(),
                ),
            packageName = packageName,
            fileName = fileName,
            outputLocation = outputLocation,
        )
    }

    private fun writeXml(
        appFunctionMetadataList:
            List<androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata>,
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        outputLocation: String? = null,
    ) {
        val xmlDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xmlDocument = xmlDocumentBuilder.newDocument().apply { xmlStandalone = true }

        val appFunctionsElement = xmlDocument.createElement(APP_FUNCTIONS_ELEMENTS_TAG)
        xmlDocument.appendChild(appFunctionsElement)

        val aggregatedDataTypes: MutableMap<String, AppFunctionDataTypeMetadata> = mutableMapOf()
        for (appFunctionMetadata in appFunctionMetadataList) {
            appFunctionMetadata.components.dataTypes.forEach { (objectKey, dataTypeMetadata) ->
                aggregatedDataTypes.putIfAbsent(objectKey, dataTypeMetadata)
            }
            val sanitizedAppFunctionMetadata =
                appFunctionMetadata.copy(components = AppFunctionComponentsMetadata())

            val appFunctionElement =
                sanitizedAppFunctionMetadata
                    .toAppFunctionMetadataDocument()
                    .toXmlElement(xmlDocument, APP_FUNCTION_ITEM_TAG)
            appFunctionElement.appendChild(
                xmlDocument.createElementWithTextNode(
                    APP_FUNCTION_ID_TAG,
                    sanitizedAppFunctionMetadata.id,
                )
            )
            appFunctionsElement.appendChild(appFunctionElement)
        }

        if (aggregatedDataTypes.isNotEmpty()) {
            val componentElement =
                AppFunctionComponentsMetadata(aggregatedDataTypes)
                    .toAppFunctionComponentsMetadataDocument()
                    .toXmlElement(doc = xmlDocument, COMPONENT_ITEM_TAG)
            appFunctionsElement.appendChild(componentElement)
        }

        val transformer =
            TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                setOutputProperty(OutputKeys.VERSION, "1.0")
                setOutputProperty(OutputKeys.STANDALONE, "yes")
            }

        if (outputLocation != null) {
            try {
                XmlFileResolver.RESOLVER.getWriteStream(
                        filePath = Path(outputLocation).resolve("$fileName.$XML_EXTENSION"),
                        logger,
                    )
                    .use { stream ->
                        transformer.transform(DOMSource(xmlDocument), StreamResult(stream))
                    }
            } catch (e: IOException) {
                throw ProcessingException(
                    "Failed to create AppFunctions XML file at: $outputLocation",
                    null,
                    e,
                )
            }
        }

        codeGenerator.createNewFile(dependencies, packageName, fileName, XML_EXTENSION).use { stream
            ->
            transformer.transform(DOMSource(xmlDocument), StreamResult(stream))
        }
    }

    companion object {
        const val XML_EXTENSION = "xml"
        const val APP_FUNCTIONS_ELEMENTS_TAG = "appfunctions"
        const val APP_FUNCTION_ITEM_TAG = "appfunction"
        const val COMPONENT_ITEM_TAG = "AppFunctionComponentMetadataDocument"
        const val APP_FUNCTION_ID_TAG = "functionId"
    }
}
