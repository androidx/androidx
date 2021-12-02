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
import androidx.compose.lint.invokedInComposableBodyAndNotRemembered
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * [Detector] that checks `getBackStackEntry` calls to make sure that if they are called inside a
 * Composable body, they are `remember`ed.
 */
class UnrememberedGetBackStackEntryDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        GetBackStackEntry.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(PackageName)) return

        if (node.invokedInComposableBodyAndNotRemembered()) {
            context.report(
                UnrememberedGetBackStackEntry,
                node,
                context.getNameLocation(node),
                "Calling getBackStackEntry during composition without using `remember`"
            )
        }
    }

    companion object {
        val UnrememberedGetBackStackEntry = Issue.create(
            "UnrememberedGetBackStackEntry",
            "Calling getBackStackEntry during composition with using `remember`",
            "Backstack entries retrieved during composition need to be `remember`ed, otherwise " +
                "they will be retrieved from the navController again, and be changed. Either " +
                "hoist the state to an object that is not created during composition, or wrap " +
                "the state in a call to `remember`.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                UnrememberedGetBackStackEntryDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val PackageName = Package("androidx.navigation")
private val GetBackStackEntry = Name(PackageName, "getBackStackEntry")
