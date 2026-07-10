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

@file:Suppress("UnstableApiUsage")

package androidx.wear.compose.remote.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiNamedElement
import java.util.EnumSet
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.tryResolve

/**
 * A [Detector] that warns when [androidx.wear.compose.material.LocalTextStyle] or
 * [androidx.wear.compose.material3.LocalTextStyle] is used in a function annotated with
 * `@RemoteComposable`.
 */
class LocalTextStyleDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                val identifier = node.identifier
                if (
                    identifier != WearComposeNames.Material.LocalTextStyle.shortName &&
                        !identifier.startsWith("IMPORT_ALIAS")
                ) {
                    return
                }

                // Walk up the tree to check if we are inside a @RemoteComposable function
                var parent = node.uastParent
                while (parent != null && parent !is UMethod) {
                    parent = parent.uastParent
                }
                val parentMethod = parent ?: return
                if (!parentMethod.isRemoteComposable) {
                    return
                }

                val resolved = node.tryResolve() as? PsiNamedElement ?: return
                if (
                    resolved.name != WearComposeNames.Material.LocalTextStyle.shortName &&
                        resolved.name != WearComposeNames.Material3.LocalTextStyle.shortName
                ) {
                    return
                }

                val packageName = (resolved.containingFile as? PsiClassOwner)?.packageName ?: return
                if (
                    packageName != WearComposeNames.Material.PackageName.javaPackageName &&
                        packageName != WearComposeNames.Material3.PackageName.javaPackageName
                ) {
                    return
                }

                context.report(
                    LocalTextStyleUsage,
                    node,
                    context.getNameLocation(node),
                    "Using $packageName.${resolved.name} " +
                        "inside a @${RemoteNames.CreationCompose.Layout.RemoteComposable.shortName} " +
                        "function has no effect.",
                    LintFix.create()
                        .replace()
                        .name(
                            "Replace with ${RemoteNames.WearCompose.Material3.LocalRemoteTextStyle.shortName}"
                        )
                        .all()
                        .with(RemoteNames.WearCompose.Material3.LocalRemoteTextStyle.shortName)
                        .imports(RemoteNames.WearCompose.Material3.LocalRemoteTextStyle.javaFqn)
                        .autoFix()
                        .build(),
                )
            }
        }

    companion object {
        val LocalTextStyleUsage =
            Issue.create(
                "LocalTextStyleUsage",
                "Using wear ${WearComposeNames.Material.LocalTextStyle.shortName} inside a " +
                    "@${RemoteNames.CreationCompose.Layout.RemoteComposable.shortName} function",
                "${WearComposeNames.Material.LocalTextStyle.shortName} from " +
                    "${WearComposeNames.Material.PackageName.javaPackageName} or " +
                    "${WearComposeNames.Material3.PackageName.javaPackageName} cannot be " +
                    "used in functions annotated with " +
                    "@${RemoteNames.CreationCompose.Layout.RemoteComposable.shortName}. " +
                    "Use ${RemoteNames.WearCompose.Material3.LocalRemoteTextStyle.javaFqn} instead.",
                Category.CORRECTNESS,
                5,
                Severity.WARNING,
                Implementation(
                    LocalTextStyleDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}
