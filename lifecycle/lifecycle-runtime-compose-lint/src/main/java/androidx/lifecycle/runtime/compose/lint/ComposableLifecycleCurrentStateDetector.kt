/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.lifecycle.runtime.compose.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInvokedWithinComposable
import com.android.tools.lint.client.api.UElementHandler
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
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.tryResolve

/**
 * [com.android.tools.lint.detector.api.Detector] that checks calls to Lifecycle.currentState to
 * make sure they don't happen inside the body of a composable function / lambda.
 *
 * Based on [androidx.compose.runtime.lint.ComposableStateFlowValueDetector].
 */
class ComposableLifecycleCurrentStateDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                // Look for a call to .currentState that comes from Lifecycle
                if (node.identifier != "currentState") return
                val method = node.tryResolve() as? PsiMethod ?: return
                if (method.containingClass?.inheritsFrom(LifecycleName) == true) {
                    if (node.isInvokedWithinComposable()) {
                        context.report(
                            LifecycleCurrentStateInComposition,
                            node,
                            context.getNameLocation(node),
                            "Lifecycle.currentState should not be called within composition",
                            fix()
                                .replace()
                                .text("currentState")
                                .with("currentStateAsState().value")
                                .imports("androidx.lifecycle.compose.currentStateAsState")
                                .build()
                        )
                    }
                }
            }
        }

    companion object {
        val LifecycleCurrentStateInComposition =
            Issue.Companion.create(
                "LifecycleCurrentStateInComposition",
                "Lifecycle.currentState should not be called within composition",
                "Calling Lifecycle.currentState within composition will not observe changes to the " +
                    "Lifecycle, so changes might not be reflected within the composition. Instead " +
                    "you should use lifecycle.currentStateAsState() to observe changes to the Lifecycle, " +
                    "and recompose when it changes.",
                Category.Companion.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ComposableLifecycleCurrentStateDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}

private val LifecyclePackageName = Package("androidx.lifecycle")
private val LifecycleName = Name(LifecyclePackageName, "Lifecycle")
