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

@file:Suppress("UnstableApiUsage")

package androidx.compose.material3.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.uast.UImportStatement

/**
 * [Detector] that checks for androidx.compose.material imports, since it in most cases combing
 * androidx.compose.material and androidx.compose.material3 will cause issues / unintended UI.
 */
class MaterialImportDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UImportStatement::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        // Currently we only check for imports, not FQN references - if a developer is
        // explicitly doing androidx.compose.material.Button() in their code it's probably
        // intentional.
        override fun visitImportStatement(node: UImportStatement) {
            val reference = node.importReference ?: return
            val importString = reference.asSourceString()

            if (
                // Wildcard reference - so the import string is exactly androidx.compose.material
                importString == MaterialPackage ||
                // The prefix is androidx.compose.material - ignore material3* and other prefixes
                importString.contains("$MaterialPackage.")
            ) {
                // Ignore explicitly allowed imports
                if (AllowlistedSubpackages.any { importString.contains(it) }) return
                if (AllowlistedImports.any { importString == it }) return

                context.report(
                    UsingMaterialAndMaterial3Libraries,
                    reference,
                    context.getLocation(reference),
                    "Using a material import while also using the material3 library"
                )
            }
        }
    }

    companion object {
        val UsingMaterialAndMaterial3Libraries = Issue.create(
            "UsingMaterialAndMaterial3Libraries",
            "material and material3 are separate, incompatible design system libraries",
            "material and material3 are separate design system libraries that are " +
                "incompatible with each other, as they have their own distinct theming systems. " +
                "Using components from both libraries concurrently can cause issues: for example " +
                "material components will not pick up the correct content color from a material3 " +
                "container, and vice versa.",
            Category.CORRECTNESS, 3, Severity.WARNING,
            Implementation(
                MaterialImportDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private const val MaterialPackage = "androidx.compose.material"

private val AllowlistedSubpackages = listOf(
    // material-icons is a separate library that is compatible with both
    "$MaterialPackage.icons",
    // material-ripple is a separate library that is compatible with both
    "$MaterialPackage.ripple",
    // TODO: b/261760718 - remove this when pullrefresh is added to m3
    // pullrefresh currently only exists in m2, so there is no alternative for m3, so temporarily
    // ignore
    "$MaterialPackage.pullrefresh"
)

// TODO: b/261760718 - remove this when pullrefresh is added to m3
private val AllowlistedImports = listOf(
    "$MaterialPackage.ExperimentalMaterialApi"
)
