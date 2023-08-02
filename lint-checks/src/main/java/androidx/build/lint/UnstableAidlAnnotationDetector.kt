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

package androidx.build.lint

import androidx.build.lint.aidl.AidlDefinitionDetector
import androidx.build.lint.aidl.getLocation
import androidx.com.android.tools.idea.lang.aidl.psi.AidlAnnotationElement
import androidx.com.android.tools.idea.lang.aidl.psi.AidlDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlInterfaceDeclaration
import androidx.com.android.tools.idea.lang.aidl.psi.AidlParcelableDeclaration
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

/**
 * Annotation used by the AIDL compiler to emit arbitrary Java annotations in generated source.
 *
 * This detector's functionality requires that Stable AIDL files are _not_ passed to Lint. See the
 * `Project.configureLintForAidl()` extension function in `LintConfiguration.kt` for implementation.
 */
private const val JAVA_PASSTHROUGH = "JavaPassthrough"

class UnstableAidlAnnotationDetector : AidlDefinitionDetector() {

    override fun visitAidlParcelableDeclaration(context: Context, node: AidlParcelableDeclaration) {
        checkDeclaration(context, node, node.annotationElementList)
    }

    override fun visitAidlInterfaceDeclaration(context: Context, node: AidlInterfaceDeclaration) {
        checkDeclaration(context, node, node.annotationElementList)
    }

    private fun checkDeclaration(
        context: Context,
        node: AidlDeclaration,
        annotations: List<AidlAnnotationElement>
    ) {
        var passthruAnnotations: List<AidlAnnotationElement> = annotations.filter {
                annotationElement ->
            annotationElement.qualifiedName.name.equals(JAVA_PASSTHROUGH)
        }

        // The AIDL lexer doesn't handle @JavaPassthrough correctly, so look through the previous
        // elements until we either find it or hit another declaration.
        if (passthruAnnotations.isEmpty()) {
            passthruAnnotations = node.filterPrevSiblingUntilNull { element ->
                when (element) {
                    is AidlAnnotationElement -> element.qualifiedName.name.equals(JAVA_PASSTHROUGH)
                    is AidlDeclaration -> null
                    else -> false
                }
            }
        }

        // Find a JavaPassthrough annotation whose parameter is a RequiresOptIn marker.
        val isAnnotated = passthruAnnotations.any { passthruAnnotation ->
            val annotationName = passthruAnnotation.expression?.text?.toString()
            if (annotationName?.startsWith("\"@") == true) {
                // Attempt to load the fully-qualified class name as a PsiClass.
                val project = passthruAnnotation.project
                val psiClass = JavaPsiFacade.getInstance(project).findClass(
                    annotationName.substring(2, annotationName.length - 1),
                    GlobalSearchScope.projectScope(project)
                )
                // Determine if the class is annotated with RequiresOptIn.
                psiClass?.annotations?.any { psiAnnotation ->
                    // Either androidx.annotation or kotlin version is fine here.
                    psiAnnotation.hasQualifiedName("RequiresOptIn")
                } ?: false
            } else {
                false
            }
        }

        if (!isAnnotated) {
            context.report(ISSUE, node.getLocation(),
                "Unstable AIDL files must be annotated with `@RequiresOptIn` marker")
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "RequireUnstableAidlAnnotation",
            "Unstable AIDL files must be annotated",
            "AIDL files that are not managed by the Stable AIDL plugin must be " +
                "annotated with a `@RequiresOptIn` marker to indicate they must not be used in " +
                "production code. See go/androidx-api-guidelines#annotating-unstable-ipc for " +
                "details on creating marker annotations and migrating to Stable AIDL.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(UnstableAidlAnnotationDetector::class.java, Scope.OTHER_SCOPE)
        )
    }
}

/**
 * Collects siblings in the "previous" direction, keeping them when the predicate returns true,
 * dropping them when the predicate returns false, and stopping when the predicate returns null.
 */
internal inline fun <reified T> PsiElement.filterPrevSiblingUntilNull(
    predicate: (PsiElement) -> Boolean?
): List<T> {
    val output = mutableListOf<T>()
    var sibling = this.prevSibling
    while (sibling != null) {
        val result = predicate(sibling)
        if (result == true && sibling is T) {
            output += sibling
        } else if (result == null) {
            break
        }
        sibling = sibling.prevSibling
    }
    return output
}
