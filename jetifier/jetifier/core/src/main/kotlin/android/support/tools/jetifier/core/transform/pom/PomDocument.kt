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

import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.utils.Log
import org.jdom2.Document
import org.jdom2.Element

/**
 * Wraps a single POM XML [ArchiveFile] with parsed metadata about transformation related sections.
 */
class PomDocument(val file: ArchiveFile, private val document: Document) {

    companion object {
        private const val TAG = "Pom"

        fun loadFrom(file: ArchiveFile) : PomDocument {
            val document = XmlUtils.createDocumentFromByteArray(file.data)
            val pomDoc = PomDocument(file, document)
            pomDoc.initialize()
            return pomDoc
        }
    }

    val dependencies : MutableSet<PomDependency> = mutableSetOf()
    private val properties : MutableMap<String, String> = mutableMapOf()
    private var dependenciesGroup : Element? = null
    private var hasChanged : Boolean = false

    private fun initialize() {
        val propertiesGroup = document.rootElement
                .getChild("properties", document.rootElement.namespace)
        if (propertiesGroup != null) {
            propertiesGroup.children
                .filterNot { it.value.isNullOrEmpty() }
                .forEach { properties[it.name] = it.value }
        }

        dependenciesGroup = document.rootElement
                .getChild("dependencies", document.rootElement.namespace) ?: return
        dependenciesGroup!!.children.mapTo(dependencies) {
            PomDependency.fromXmlElement(it, properties)
        }
    }

    /**
     * Validates that this document is consistent with the provided [rules].
     *
     * Currently it checks that all the dependencies that are going to be rewritten by the given
     * rules satisfy the minimal version requirements defined by the rules.
     */
    fun validate(rules: List<PomRewriteRule>) : Boolean {
        if (dependenciesGroup == null) {
            // Nothing to validate as this file has no dependencies section
            return true
        }

        return dependencies.all { dep -> rules.all { it.validateVersion(dep) } }
    }

    /**
     * Applies the given [rules] to rewrite the POM file.
     *
     * Changes are not saved back until requested.
     */
    fun applyRules(rules: List<PomRewriteRule>) {
        if (dependenciesGroup == null) {
            // Nothing to transform as this file has no dependencies section
            return
        }

        val newDependencies = mutableSetOf<PomDependency>()
        for (dependency in dependencies) {
            if (dependency.shouldSkipRewrite()) {
                continue
            }

            val rule = rules.firstOrNull { it.matches(dependency) }
            if (rule == null) {
                // No rule to rewrite => keep it
                newDependencies.add(dependency)
            } else {
                // Replace with new dependencies
                newDependencies.addAll(rule.to.mapTo(newDependencies){ it.rewrite(dependency) })
            }
        }

        if (newDependencies.isEmpty()) {
            // No changes
            return
        }

        dependenciesGroup!!.children.clear()
        newDependencies.forEach { dependenciesGroup!!.addContent(it.toXmlElement(document)) }
        hasChanged = true
    }

    /**
     * Saves any current pending changes back to the file if needed.
     */
    fun saveBackToFileIfNeeded() {
        if (!hasChanged) {
            return
        }

        file.data =  XmlUtils.convertDocumentToByteArray(document)
    }

    /**
     * Logs the information about the current file using info level.
     */
    fun logDocumentDetails() {
        Log.i(TAG, "POM file at: '%s'", file.relativePath)
        for ((groupId, artifactId, version) in dependencies) {
            Log.i(TAG, "- Dep: %s:%s:%s", groupId, artifactId, version)
        }
    }
}