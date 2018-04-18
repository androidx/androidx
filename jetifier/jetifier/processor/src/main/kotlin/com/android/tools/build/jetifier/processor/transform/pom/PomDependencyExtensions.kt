/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.pom

import com.android.tools.build.jetifier.core.pom.PomDependency
import org.jdom2.Document
import org.jdom2.Element

/**
 * Transforms the current data into XML '<dependency>' node.
 */
fun PomDependency.toXmlElement(document: Document): Element {
    val node = Element("dependency")
    node.namespace = document.rootElement.namespace

    XmlUtils.addStringNodeToNode(node, "groupId", groupId)
    XmlUtils.addStringNodeToNode(node, "artifactId", artifactId)
    XmlUtils.addStringNodeToNode(node, "version", version)
    XmlUtils.addStringNodeToNode(node, "classifier", classifier)
    XmlUtils.addStringNodeToNode(node, "type", type)
    XmlUtils.addStringNodeToNode(node, "scope", scope)
    XmlUtils.addStringNodeToNode(node, "systemPath", systemPath)
    XmlUtils.addStringNodeToNode(node, "optional", optional)
    return node
}