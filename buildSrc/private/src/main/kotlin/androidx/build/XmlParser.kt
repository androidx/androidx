/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build

import java.io.StringReader
import java.util.StringTokenizer
import org.apache.xerces.jaxp.SAXParserImpl.JAXPSAXParser
import org.dom4j.Document
import org.dom4j.DocumentException
import org.dom4j.DocumentFactory
import org.dom4j.io.SAXReader
import org.xml.sax.InputSource
import org.xml.sax.XMLReader

/** Parses an xml string */
@Throws(DocumentException::class)
internal fun parseXml(text: String, namespaceUris: Map<String, String>): Document {
    val docFactory = DocumentFactory()
    docFactory.xPathNamespaceURIs = namespaceUris
    // Ensure that we're consistently using JAXP parser.
    val xmlReader = JAXPSAXParser()
    return parseXml(docFactory, xmlReader, text)
}

// Copied from org.dom4j.DocumentHelper with modifications to allow SAXReader configuration.
@Throws(DocumentException::class)
private fun parseXml(
    documentFactory: DocumentFactory,
    xmlReader: XMLReader,
    text: String,
): Document {
    val reader = SAXReader.createDefault()
    reader.documentFactory = documentFactory
    reader.xmlReader = xmlReader
    val encoding = getEncoding(text)
    val source = InputSource(StringReader(text))
    source.encoding = encoding
    val result = reader.read(source)
    if (result.xmlEncoding == null) {
        result.xmlEncoding = encoding
    }
    return result
}

// Copied from org.dom4j.DocumentHelper.
private fun getEncoding(text: String): String? {
    var result: String? = null
    val xml = text.trim { it <= ' ' }
    if (xml.startsWith("<?xml")) {
        val end = xml.indexOf("?>")
        val sub = xml.substring(0, end)
        val tokens = StringTokenizer(sub, " =\"'")
        while (tokens.hasMoreTokens()) {
            val token = tokens.nextToken()
            if ("encoding" == token) {
                if (tokens.hasMoreTokens()) {
                    result = tokens.nextToken()
                }
                break
            }
        }
    }
    return result
}
