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

package androidx.compose.foundation.lint

import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isNotRemembered
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
 * [Detector] that checks `MutableInteractionSource` calls to make sure that if they are called
 * inside a Composable body, they are `remember`ed.
 */
class UnrememberedMutableInteractionSourceDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(
        FoundationNames.Interaction.MutableInteractionSource.shortName
    )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(FoundationNames.Interaction.PackageName)) return

        if (node.isNotRemembered()) {
            context.report(
                UnrememberedMutableInteractionSource,
                node,
                context.getNameLocation(node),
                "Creating a MutableInteractionSource during composition without using " +
                    "`remember`"
            )
        }
    }

    companion object {
        val UnrememberedMutableInteractionSource = Issue.create(
            "UnrememberedMutableInteractionSource",
            "Creating a MutableInteractionSource during composition without using " +
                "`remember`",
            "MutableInteractionSource instances created during composition need to be " +
                "`remember`ed, otherwise they will be recreated during recomposition, and lose " +
                "their state. Either hoist the MutableInteractionSource to an object that is not " +
                "created during composition, or wrap the MutableInteractionSource in a call to " +
                "`remember`.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                UnrememberedMutableInteractionSourceDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
