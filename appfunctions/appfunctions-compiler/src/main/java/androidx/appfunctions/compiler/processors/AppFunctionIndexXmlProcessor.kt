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

import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AnnotatedAppFunctions
import androidx.appfunctions.compiler.core.AppFunctionSymbolResolver
import androidx.appfunctions.compiler.core.createElementWithTextNode
import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.compiler.core.metadata.CompileTimeAppFunctionMetadata
import androidx.appfunctions.compiler.core.toXmlElement
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

/**
 * Generates AppFunction's index xml file with all properties of [CompileTimeAppFunctionMetadata]
 * for the AppSearch indexer to index.
 *
 * The generator would write an XML file as `/assets/app_functions_dynamic_schema.xml`. The file
 * would be packaged into the APK's asset when assembled, so that the AppSearch indexer can look up
 * the asset and inject metadata into platform AppSearch database accordingly.
 */
class AppFunctionIndexXmlProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val appFunctionSymbolResolver = AppFunctionSymbolResolver(resolver)
        val resolvedAnnotatedSerializableProxies =
            ResolvedAnnotatedSerializableProxies(
                appFunctionSymbolResolver.resolveAllAnnotatedSerializableProxiesFromModule()
            )
        generateIndexXml(
            appFunctionSymbolResolver.getAnnotatedAppFunctionsFromAllModules(),
            resolvedAnnotatedSerializableProxies,
        )
        return emptyList()
    }

    /**
     * Generates AppFunction's index xml files for indexer in App Search.
     *
     * @param appFunctionsByClass a collection of functions annotated with @AppFunction
     * @param resolvedAnnotatedSerializableProxies a collection of resolved annotated serializable
     *   proxies
     */
    private fun generateIndexXml(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) {
        if (appFunctionsByClass.isEmpty()) {
            return
        }
        writeXmlFile(appFunctionsByClass, resolvedAnnotatedSerializableProxies)
    }

    private fun writeXmlFile(
        appFunctionsByClass: List<AnnotatedAppFunctions>,
        resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies,
    ) {
        val appFunctionMetadataList =
            appFunctionsByClass.flatMap {
                it.createAppFunctionMetadataList(resolvedAnnotatedSerializableProxies)
            }

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

        val componentElement =
            AppFunctionComponentsMetadata(aggregatedDataTypes)
                .toAppFunctionComponentsMetadataDocument()
                .toXmlElement(doc = xmlDocument, COMPONENT_ITEM_TAG)
        appFunctionsElement.appendChild(componentElement)

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
                    *appFunctionsByClass.flatMap { it.getSourceFiles() }.toTypedArray(),
                ),
                XML_PACKAGE_NAME,
                XML_FILE_NAME,
                XML_EXTENSION,
            )
            .use { stream -> transformer.transform(DOMSource(xmlDocument), StreamResult(stream)) }
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

    private companion object {
        const val XML_PACKAGE_NAME = "assets"
        const val XML_FILE_NAME = "app_functions_v2"
        const val XML_EXTENSION = "xml"
        const val APP_FUNCTIONS_ELEMENTS_TAG = "appfunctions"
        const val APP_FUNCTION_ITEM_TAG = "appfunction"
        const val COMPONENT_ITEM_TAG = "AppFunctionComponentMetadataDocument"
        const val APP_FUNCTION_ID_TAG = "functionId"
    }
}
