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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.metadata.AppFunctionComponentsMetadataDocument
import androidx.appfunctions.compiler.core.metadata.AppFunctionDataTypeMetadataDocument
import androidx.appfunctions.compiler.core.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.compiler.core.metadata.AppFunctionNamedDataTypeMetadataDocument
import androidx.appfunctions.compiler.core.metadata.AppFunctionParameterMetadataDocument
import androidx.appfunctions.compiler.core.metadata.AppFunctionResponseMetadataDocument
import org.w3c.dom.Document
import org.w3c.dom.Element

internal fun Document.createElementWithTextNode(elementName: String, text: String): Element =
    createElement(elementName).apply { appendChild(createTextNode(text)) }

internal fun AppFunctionMetadataDocument.toXmlElement(doc: Document, elementName: String): Element =
    doc.createElement(elementName).apply {
        appendChild(doc.createElementWithTextNode("id", id))

        appendChild(
            doc.createElementWithTextNode("enabledByDefault", isEnabledByDefault.toString())
        )

        if (!description.isEmpty()) {
            appendChild(doc.createElementWithTextNode("description", description))
        }

        parameters?.let {
            for (param in it) {
                appendChild(param.toXmlElement(doc, "parameters"))
            }
        }

        response?.let { appendChild(it.toXmlElement(doc, "response")) }

        schemaCategory?.let { appendChild(doc.createElementWithTextNode("schemaCategory", it)) }

        schemaName?.let { appendChild(doc.createElementWithTextNode("schemaName", it)) }

        schemaVersion?.let {
            appendChild(doc.createElementWithTextNode("schemaVersion", it.toString()))
        }
    }

internal fun AppFunctionComponentsMetadataDocument.toXmlElement(
    doc: Document,
    elementName: String,
): Element =
    doc.createElement(elementName).apply {
        for (dataType in dataTypes) {
            appendChild(dataType.toXmlElement(doc, "dataTypes"))
        }
        appendChild(doc.createElementWithTextNode("id", id))
    }

private fun AppFunctionDataTypeMetadataDocument.toXmlElement(
    doc: Document,
    elementName: String,
): Element =
    doc.createElement(elementName).apply {
        for (property in allOf) {
            appendChild(property.toXmlElement(doc, "allOf"))
        }
        dataTypeReference?.let {
            appendChild(doc.createElementWithTextNode("dataTypeReference", it))
        }

        appendChild(doc.createElementWithTextNode("id", id))

        appendChild(doc.createElementWithTextNode("isNullable", isNullable.toString()))

        itemType?.let { appendChild(it.toXmlElement(doc, "itemType")) }

        objectQualifiedName?.let {
            appendChild(doc.createElementWithTextNode("objectQualifiedName", it))
        }

        if (!description.isEmpty()) {
            appendChild(doc.createElementWithTextNode("description", description))
        }

        for (property in properties) {
            appendChild(property.toXmlElement(doc, "properties"))
        }

        for (property in required) {
            appendChild(doc.createElementWithTextNode("required", property))
        }

        appendChild(doc.createElementWithTextNode("type", type.toString()))
    }

private fun AppFunctionNamedDataTypeMetadataDocument.toXmlElement(
    doc: Document,
    elementName: String,
): Element =
    doc.createElement(elementName).apply {
        appendChild(dataTypeMetadata.toXmlElement(doc, "dataTypeMetadata"))
        appendChild(doc.createElementWithTextNode("id", id))
        appendChild(doc.createElementWithTextNode("name", name))
    }

private fun AppFunctionResponseMetadataDocument.toXmlElement(
    doc: Document,
    elementName: String,
): Element =
    doc.createElement(elementName).apply {
        appendChild(doc.createElementWithTextNode("id", id))
        appendChild(valueType.toXmlElement(doc, "valueType"))
    }

private fun AppFunctionParameterMetadataDocument.toXmlElement(
    doc: Document,
    elementName: String,
): Element =
    doc.createElement(elementName).apply {
        appendChild(dataTypeMetadata.toXmlElement(doc, "dataTypeMetadata"))
        appendChild(doc.createElementWithTextNode("id", id))
        appendChild(doc.createElementWithTextNode("isRequired", isRequired.toString()))
        appendChild(doc.createElementWithTextNode("name", name))
    }
