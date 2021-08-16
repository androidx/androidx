/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.work.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.TOOLS_URI
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.jetbrains.uast.UClass
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.EnumSet

class RemoveWorkManagerInitializerDetector : Detector(), SourceCodeScanner, XmlScanner {
    private var removedDefaultInitializer = false
    private var location: Location? = null
    private var applicationImplementsConfigurationProvider = false

    companion object {

        private const val DESCRIPTION = "Remove androidx.work.WorkManagerInitializer from " +
            "your AndroidManifest.xml when using on-demand initialization."

        val ISSUE = Issue.create(
            id = "RemoveWorkManagerInitializer",
            briefDescription = DESCRIPTION,
            explanation = """
                If an `android.app.Application` implements `androidx.work.Configuration.Provider`,
                the default `androidx.startup.InitializationProvider` needs to be removed from the
                AndroidManifest.xml file.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                RemoveWorkManagerInitializerDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST)
            )
        )

        private const val ATTR_NODE = "node"

        fun NodeList?.find(fn: (node: Node) -> Boolean): Node? {
            if (this == null) {
                return null
            } else {
                for (i in 0 until this.length) {
                    val node = this.item(i)
                    if (fn(node)) {
                        return node
                    }
                }
                return null
            }
        }
    }

    override fun getApplicableElements() = listOf("application")

    override fun applicableSuperClasses() = listOf("androidx.work.Configuration.Provider")

    override fun visitElement(context: XmlContext, element: Element) {
        // Check providers
        val providers = element.getElementsByTagName("provider")
        val provider = providers.find { node ->
            val name = node.attributes.getNamedItemNS(ANDROID_URI, ATTR_NAME)?.textContent
            name == "androidx.startup.InitializationProvider"
        }
        if (provider != null) {
            location = context.getLocation(provider)
            val remove = provider.attributes.getNamedItemNS(TOOLS_URI, ATTR_NODE)
            if (remove?.textContent == "remove") {
                removedDefaultInitializer = true
            }
        }
        // Check metadata
        val metadataElements = element.getElementsByTagName("meta-data")
        val metadata = metadataElements.find { node ->
            val name = node.attributes.getNamedItemNS(ANDROID_URI, ATTR_NAME)?.textContent
            name == "androidx.work.WorkManagerInitializer"
        }
        if (metadata != null && !removedDefaultInitializer) {
            location = context.getLocation(metadata)
            val remove = metadata.attributes.getNamedItemNS(TOOLS_URI, ATTR_NODE)
            if (remove?.textContent == "remove") {
                removedDefaultInitializer = true
            }
        }
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (context.evaluator.inheritsFrom(
                declaration.javaPsi,
                "android.app.Application",
                false
            )
        ) {
            applicationImplementsConfigurationProvider = true
        }
    }

    override fun afterCheckRootProject(context: Context) {
        val location = location ?: Location.create(context.file)
        if (applicationImplementsConfigurationProvider) {
            if (!removedDefaultInitializer) {
                context.report(
                    issue = ISSUE,
                    location = location,
                    message = DESCRIPTION
                )
            }
        }
    }
}
