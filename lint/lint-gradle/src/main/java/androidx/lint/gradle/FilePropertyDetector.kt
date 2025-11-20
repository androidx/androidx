/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lint.gradle

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

/**
 * Detect usages of Property<File>.
 *
 * It is always better to use RegularFileProperty or DirectoryProperty as it is enforcing the use
 * directory vs file.
 */
class FilePropertyDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                node.returnType?.let { checkType(it, node) }
                node.uastParameters.forEach { parameter -> checkType(parameter.type, parameter) }
            }

            private fun checkType(type: PsiType, node: UElement) {
                when {
                    isDiscouragedPropertyType(type) -> reportIssue(node)
                    type is PsiClassType -> handlePsiClassType(type, node)
                }
            }

            private fun handlePsiClassType(type: PsiClassType, node: UElement) {
                val resolvedClass = type.resolve() ?: return
                if (resolvedClass.superTypes.any { isDiscouragedPropertyType(it) }) {
                    reportIssue(node)
                }
                type.parameters.forEach { parameter -> checkType(parameter, node) }
            }

            private fun isDiscouragedPropertyType(type: PsiType): Boolean {
                return type.canonicalText in discouragedTypes
            }

            private fun reportIssue(node: UElement) {
                val incident =
                    Incident(context)
                        .issue(FILE_PROPERTY_ISSUE)
                        .location(context.getNameLocation(node))
                        .message(
                            "`Property<File>` is discouraged. Use `RegularFileProperty` or `DirectoryProperty`."
                        )
                        .scope(node)
                context.report(incident)
            }
        }

    companion object {
        val FILE_PROPERTY_ISSUE: Issue =
            Issue.create(
                id = "FilePropertyDetector",
                briefDescription = "Avoid using Property<File>",
                explanation =
                    """
                `Property<File>` is discouraged. Use `RegularFileProperty` for files or
                `DirectoryProperty` for directories to enforce better type safety.
            """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(FilePropertyDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        private val discouragedTypes =
            setOf(
                "org.gradle.api.provider.Property<java.io.File>",
                "org.gradle.api.provider.Property<File>",
            )
    }
}
