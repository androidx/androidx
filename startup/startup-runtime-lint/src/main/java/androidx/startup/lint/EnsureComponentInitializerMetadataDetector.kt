/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.startup.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import org.jetbrains.uast.UClass
import org.w3c.dom.Element
import java.util.EnumSet

/**
 * A [Detector] which ensures that every `ComponentInitializer` is accompanied by a corresponding
 * entry in the `AndroidManifest.xml`.
 */
class EnsureComponentInitializerMetadataDetector : Detector(), SourceCodeScanner, XmlScanner {
    // Keeps track of all the declared components
    private val components = mutableSetOf<String>()

    companion object {
        private const val DESCRIPTION = "Every ComponentInitializer needs to be accompanied by a " +
                "corresponding <meta-data> entry in the AndroidManifest.xml file."

        val ISSUE = Issue.create(
            id = "EnsureComponentInitializerMetadata",
            briefDescription = DESCRIPTION,
            explanation = """
                When a library defines a ComponentInitializer, it needs to be accompanied by a \
                corresponding <meta-data> entry in the AndroidManifest.xml file.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                EnsureComponentInitializerMetadataDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST)
            )
        )
    }

    override fun applicableSuperClasses() = listOf("androidx.startup.ComponentInitializer")

    override fun getApplicableElements() = listOf("meta-data")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val name = declaration.qualifiedName

        if (name == "androidx.startup.ComponentInitializer") {
            // This is the component initializer itself.
            return
        }

        if (!declaration.isInterface && declaration.qualifiedName !in components) {
            val location = context.getLocation(declaration.javaPsi)
            context.report(
                issue = ISSUE,
                location = location,
                message = DESCRIPTION
            )
        }
    }

    override fun visitElement(context: XmlContext, element: Element) {
        // Track all <meta-data> elements with value androidx.startup
        val name = element.getAttributeNS(ANDROID_URI, ATTR_NAME)
        val value = element.getAttributeNS(ANDROID_URI, ATTR_VALUE)
        // There does not seem to be a way to evaluate resources defined in the manifest.
        // Figure out if there is a better way.
        if (value == "androidx.startup" || value == "@string/androidx_startup") {
            components.add(name)
        }
    }
}
