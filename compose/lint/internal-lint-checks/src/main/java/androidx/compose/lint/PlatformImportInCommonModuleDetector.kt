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

package androidx.compose.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getIoFile

/**
 * Lint [Detector] that catches platform-dependent imports in a common module.
 */
class PlatformImportInCommonModuleDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() =
        listOf(UImportStatement::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            // this only verifies imports and not fqname references
            // potentially we want to make sure to search for those too

            override fun visitImportStatement(node: UImportStatement) {
                val reference = node.importReference?.asRenderString() ?: return
                val isPlatformImport = PLATFORM_PACKAGES.any { platformPackage ->
                    (platformPackage == reference && node.isOnDemand) ||
                        reference.startsWith("$platformPackage.")
                }
                if (!isPlatformImport) return

                val file = node.getContainingUFile()?.getIoFile() ?: return
                val isInCommonModule = file.absolutePath.contains(COMMON_MAIN_PATH_PREFIX)
                if (!isInCommonModule) return

                val target = node.importReference!!
                context.report(
                    ISSUE,
                    target,
                    context.getLocation(target),
                    "Platform-dependent import in a common module"
                )
            }
        }

    companion object {
        val ISSUE = Issue.create(
            id = "PlatformImportInCommonModule",
            briefDescription = "Platform-dependent import in a common module",
            explanation = "Common Kotlin module cannot contain references to JVM or Android " +
                "classes, as it reduces future portability to other Kotlin targets. Instead of " +
                "referencing them directly, use expect/actual declarations.",
            category = Category.CORRECTNESS,
            priority = 5,
            severity = Severity.ERROR,
            implementation = Implementation(
                PlatformImportInCommonModuleDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private const val COMMON_MAIN_PATH_PREFIX = "src/commonMain"
        private val PLATFORM_PACKAGES = listOf(
            "java",
            "javax",
            "android"
        )
    }
}