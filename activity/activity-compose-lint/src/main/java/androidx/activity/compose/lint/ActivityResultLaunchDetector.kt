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

package androidx.activity.compose.lint

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
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet

/**
 * [Detector] that checks `launch` calls to make sure they don't happen inside the body of a
 * composable function / lambda.
 */
class ActivityResultLaunchDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        Launch.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(PackageName)) return

        if (node.isInvokedWithinComposable()) {
            context.report(
                LaunchDuringComposition,
                node,
                context.getNameLocation(node),
                "Calls to `launch` should happen inside of a SideEffect and not during composition"
            )
        }
    }

    companion object {
        val LaunchDuringComposition = Issue.create(
            "LaunchDuringComposition",
            "Calls to `launch` should happen inside of a SideEffect and not during composition",
            "Calling `launch` during composition is incorrect. Doing so will cause launch to be " +
                "called multiple times resulting in a RuntimeException. Instead, use `SideEffect`" +
                " and `launch` inside of the suspending block. The block will only run after a " +
                "successful composition.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                ActivityResultLaunchDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}

private val PackageName = Package("androidx.activity.compose")
private val Launch = Name(PackageName, "launch")
