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
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.w3c.dom.Element
import java.util.EnumSet

/**
 * A [Detector] which ensures that every `ComponentInitializer` is accompanied by a corresponding
 * entry in the `AndroidManifest.xml`.
 */
class EnsureInitializerMetadataDetector : Detector(), SourceCodeScanner, XmlScanner {
    // all declared components
    private val components = mutableMapOf<UClass, Location>()
    // all reachable components
    // Synthetic access
    val reachable = mutableSetOf<String>()

    companion object {
        private const val DESCRIPTION = "Every Initializer needs to be accompanied by a " +
                "corresponding <meta-data> entry in the AndroidManifest.xml file."

        val ISSUE = Issue.create(
            id = "EnsureInitializerMetadata",
            briefDescription = DESCRIPTION,
            explanation = """
                When a library defines a Initializer, it needs to be accompanied by a \
                corresponding <meta-data> entry in the AndroidManifest.xml file.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                EnsureInitializerMetadataDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST)
            )
        )
    }

    override fun applicableSuperClasses() = listOf("androidx.startup.Initializer")

    override fun getApplicableElements() = listOf("meta-data")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val name = declaration.qualifiedName

        if (name == "androidx.startup.Initializer") {
            // This is the component initializer itself.
            return
        }

        if (!declaration.isInterface) {
            val location = context.getLocation(declaration.javaPsi)
            components[declaration] = location
        }

        // Check every dependencies() method for reachable Initializer's
        val method = declaration.methods.first {
            it.name == "dependencies" && it.parameterList.isEmpty
        }
        val visitor = object : AbstractUastVisitor() {
            override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {
                val qualifiedName = (node.type as? PsiClassReferenceType)?.resolve()?.qualifiedName
                if (qualifiedName != null) {
                    reachable += qualifiedName
                }
                return true
            }
        }

        method.accept(visitor)
    }

    override fun visitElement(context: XmlContext, element: Element) {
        // Track all <meta-data> elements with value androidx.startup
        val name = element.getAttributeNS(ANDROID_URI, ATTR_NAME)
        val value = element.getAttributeNS(ANDROID_URI, ATTR_VALUE)
        // There does not seem to be a way to evaluate resources defined in the manifest.
        // Figure out if there is a better way.
        if (value == "androidx.startup" || value == "@string/androidx_startup") {
            reachable += name
        }
    }

    override fun afterCheckRootProject(context: Context) {
        for ((declaration, location) in components) {
            if (declaration.qualifiedName !in reachable) {
                context.report(issue = ISSUE, location = location, message = DESCRIPTION)
            }
        }
    }
}
