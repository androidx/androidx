/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.compose.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isInvokedWithinComposable
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

/**
 * [Detector] that checks `composable` calls to make sure that they are not called inside a
 * Composable body.
 */
class ComposableDestinationInComposeScopeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(Composable.shortName, Navigation.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(PackageName)) return

        if (node.isInvokedWithinComposable()) {
            if (method.name == Composable.shortName) {
                context.report(
                    ComposableDestinationInComposeScope,
                    node,
                    context.getNameLocation(node),
                    "Using composable inside of a compose scope"
                )
            } else {
                context.report(
                    ComposableNavGraphInComposeScope,
                    node,
                    context.getNameLocation(node),
                    "Using navigation inside of a compose scope"
                )
            }
        }
    }

    companion object {
        val ComposableDestinationInComposeScope =
            Issue.create(
                "ComposableDestinationInComposeScope",
                "Building composable destination in compose scope",
                "Composable destinations should only be constructed directly within a " +
                    "NavGraphBuilder scope. Composable destinations cannot be nested, and you " +
                    "should use the `navigation` function to create a nested graph instead.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ComposableDestinationInComposeScopeDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
        val ComposableNavGraphInComposeScope =
            Issue.create(
                "ComposableNavGraphInComposeScope",
                "Building navigation graph in compose scope",
                "Composable destinations should only be constructed directly within a " +
                    "NavGraphBuilder scope.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ComposableDestinationInComposeScopeDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}

private val PackageName = Package("androidx.navigation.compose")
private val Composable = Name(PackageName, "composable")
private val Navigation = Name(PackageName, "navigation")
