/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.lint

import androidx.compose.lint.isComposable
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that flags usage of standard Compose composition locals (like LocalDensity)
 * inside @RemoteComposable functions.
 */
class RemoteCompositionLocalDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!node.isComposable) return
                // This detector currently only inspects methods explicitly annotated with
                // @RemoteComposable.
                // Ideally, we should also track composable targets if @RemoteComposable is implicit
                // without the RemoteComposable target marker.
                if (!node.isRemoteComposable) return

                node.accept(
                    object : AbstractUastVisitor() {
                        override fun visitSimpleNameReferenceExpression(
                            node: USimpleNameReferenceExpression
                        ): Boolean {
                            val resolved = node.resolve()
                            if (resolved != null) {
                                val fqn = getFqn(resolved)
                                if (fqn != null && fqn in ForbiddenLocalsFqn) {
                                    reportIssue(context, node, fqn)
                                }
                            }
                            return super.visitSimpleNameReferenceExpression(node)
                        }
                    }
                )
            }
        }

    // Resolves the fully qualified name (FQN) of the declaration element.
    // Handles Kotlin package-level properties which resolve to KtProperty elements in source AST,
    // or compiled JVM PsiMethod/PsiField elements in library binaries (under name*Kt classes).
    private fun getFqn(element: PsiElement): String? {
        if (element is KtProperty) {
            return element.fqName?.asString()
        }
        if (element is PsiMethod) {
            val containingClass = element.containingClass ?: return null
            return containingClass.qualifiedName + "." + element.name
        }
        if (element is PsiField) {
            val containingClass = element.containingClass ?: return null
            return containingClass.qualifiedName + "." + element.name
        }
        return null
    }

    private fun reportIssue(context: JavaContext, node: UElement, fqn: String) {
        val message =
            when (fqn) {
                LocalDensityFqn,
                LocalDensityGetterFqn ->
                    "Using `LocalDensity` in a `@RemoteComposable` will bake static values into the document. " +
                        "Use `LocalRemoteDensity.current` instead to support dynamic host density."
                LocalConfigurationFqn,
                LocalConfigurationGetterFqn ->
                    "Using `LocalConfiguration` in a `@RemoteComposable` will bake a static value into the document at capture time. " +
                        "Configuration changes at runtime on the player will not be reflected. Instead of querying configuration dynamically, " +
                        "use responsive layout modifiers (e.g. RemoteModifier.fillMaxSize()) or constraints."
                LocalContextFqn,
                LocalContextGetterFqn ->
                    "Using `LocalContext` in a `@RemoteComposable` will bake static values into the document at capture time. " +
                        "The captured context will not represent the host player's context. For loading resources, use host-resolved " +
                        "alternatives like named RemoteString expressions fed with values from app resources by the host device to the player."
                else -> {
                    val name = fqn.substringAfterLast('.')
                    "Using `$name` in a `@RemoteComposable` will bake static values into the document. " +
                        "This value will be resolved at the time of document capture."
                }
            }
        context.report(RemoteCompositionLocalUsage, node, context.getLocation(node), message)
    }

    companion object {
        private val PackageName = "androidx.compose.ui.platform"
        private val LocalDensityFqn = "$PackageName.LocalDensity"
        private val LocalContextFqn = "$PackageName.LocalContext"
        private val LocalConfigurationFqn = "$PackageName.LocalConfiguration"

        // Compiled getter JVM FQNs
        private val LocalDensityGetterFqn = "$PackageName.CompositionLocalsKt.getLocalDensity"
        private val LocalContextGetterFqn =
            "$PackageName.AndroidCompositionLocals_androidKt.getLocalContext"
        private val LocalConfigurationGetterFqn =
            "$PackageName.AndroidCompositionLocals_androidKt.getLocalConfiguration"

        private val ForbiddenLocalsFqn =
            setOf(
                LocalDensityFqn,
                LocalDensityGetterFqn,
                LocalConfigurationFqn,
                LocalConfigurationGetterFqn,
                LocalContextFqn,
                LocalContextGetterFqn,
            )

        val RemoteCompositionLocalUsage =
            Issue.create(
                "RemoteCompositionLocalUsage",
                "Usage of standard Compose CompositionLocals in Remote Compose",
                "Using standard Compose CompositionLocals (such as LocalDensity, LocalContext, " +
                    "or LocalConfiguration) inside a @RemoteComposable is not recommended. " +
                    "Their values are statically resolved at capture time and baked into the document, " +
                    "so they will not adapt to the player device at runtime. " +
                    "Use Remote Compose equivalents (e.g. LocalRemoteDensity) or responsive layout primitives instead.",
                Category.CORRECTNESS,
                5,
                Severity.WARNING,
                Implementation(
                    RemoteCompositionLocalDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}
