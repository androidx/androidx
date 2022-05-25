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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

class BanConcurrentHashMap : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UImportStatement::class.java,
        UQualifiedReferenceExpression::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler = object :
        UElementHandler() {

        // Detect fully qualified reference if not imported.
        override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
            if (node.selector is USimpleNameReferenceExpression) {
                val name = node.selector as USimpleNameReferenceExpression
                if (CONCURRENT_HASHMAP == name.identifier) {
                    val incident = Incident(context)
                        .issue(ISSUE)
                        .location(context.getLocation(node))
                        .message("Detected ConcurrentHashMap usage.")
                        .scope(node)
                    context.report(incident)
                }
            }
        }

        // Detect import.
        override fun visitImportStatement(node: UImportStatement) {
            if (node.importReference != null) {
                var resolved = node.resolve()
                if (node.resolve() is PsiField) {
                    resolved = (resolved as PsiField).containingClass
                } else if (resolved is PsiMethod) {
                    resolved = resolved.containingClass
                }
                if (resolved is PsiClass &&
                    CONCURRENT_HASHMAP_QUALIFIED_NAME == resolved.qualifiedName
                ) {
                    val incident = Incident(context)
                        .issue(ISSUE)
                        .location(context.getLocation(node))
                        .message("Detected ConcurrentHashMap usage.")
                        .scope(node)
                    context.report(incident)
                }
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "BanConcurrentHashMap",
            "ConcurrentHashMap usage is not allowed",
            "ConcurrentHashMap has an issue on Android’s Lollipop release that can lead to lost" +
                " updates under thread contention.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                BanConcurrentHashMap::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
        const val CONCURRENT_HASHMAP_QUALIFIED_NAME = "java.util.concurrent.ConcurrentHashMap"
        const val CONCURRENT_HASHMAP = "ConcurrentHashMap"
    }
}
