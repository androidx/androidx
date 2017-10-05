/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.pom

import com.google.gson.annotations.SerializedName
import org.jdom2.Document
import org.jdom2.Element

/**
 * Represents a '<dependency>' XML node of a POM file.
 *
 * See documentation of the content at https://maven.apache.org/pom.html#Dependencies
 */
data class PomDependency(
        @SerializedName("groupId")
        val groupId: String? = null,

        @SerializedName("artifactId")
        val artifactId: String? = null,

        @SerializedName("version")
        var version: String? = null,

        @SerializedName("classifier")
        val classifier: String? = null,

        @SerializedName("type")
        val type: String? = null,

        @SerializedName("scope")
        val scope: String? = null,

        @SerializedName("systemPath")
        val systemPath: String? = null,

        @SerializedName("optional")
        val optional: String? = null) {

    companion object {

        /**
         * Creates a new [PomDependency] from the given XML [Element].
         */
        fun fromXmlElement(node: Element, properties: Map<String, String>) : PomDependency {
            var groupId : String? = null
            var artifactId : String? = null
            var version : String? = null
            var classifier : String? = null
            var type : String? = null
            var scope : String? = null
            var systemPath : String? = null
            var optional : String? = null

            for (childNode in node.children) {
                when (childNode.name) {
                    "groupId" -> groupId = XmlUtils.resolveValue(childNode.value, properties)
                    "artifactId" -> artifactId = XmlUtils.resolveValue(childNode.value, properties)
                    "version" -> version = XmlUtils.resolveValue(childNode.value, properties)
                    "classifier" -> classifier = XmlUtils.resolveValue(childNode.value, properties)
                    "type" -> type = XmlUtils.resolveValue(childNode.value, properties)
                    "scope" -> scope = XmlUtils.resolveValue(childNode.value, properties)
                    "systemPath" -> systemPath = XmlUtils.resolveValue(childNode.value, properties)
                    "optional" -> optional = XmlUtils.resolveValue(childNode.value, properties)
                }
            }

            return PomDependency(
                    groupId = groupId,
                    artifactId = artifactId,
                    version = version,
                    classifier = classifier,
                    type = type,
                    scope = scope,
                    systemPath = systemPath,
                    optional = optional)
        }

    }

    init {
        if (version != null) {
            version = version!!.toLowerCase()
        }
    }

    /**
     * Whether this dependency should be skipped from the rewriting process
     */
    fun shouldSkipRewrite() : Boolean {
        return scope != null && scope.toLowerCase() == "test"
    }

    /**
     * Returns a new dependency created by taking all the items from the [input] dependency and then
     * overwriting these with all of its non-null items.
     */
    fun rewrite(input: PomDependency) : PomDependency {
        return PomDependency(
            groupId = groupId ?: input.groupId,
            artifactId = artifactId ?: input.artifactId,
            version = version ?: input.version,
            classifier = classifier ?: input.classifier,
            type = type ?: input.type,
            scope = scope ?: input.scope,
            systemPath = systemPath ?: input.systemPath,
            optional = optional ?: input.optional
        )
    }

    /**
     * Transforms the current data into XML '<dependency>' node.
     */
    fun toXmlElement(document: Document) : Element {
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
}