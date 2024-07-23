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

package androidx.lifecycle.lint

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
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.util.isConstructorCall

/** [Detector] that checks if a view model is being constructed directly in a composable. */
class ViewModelConstructorInComposableDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UCallExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (!node.isInvokedWithinComposable()) return
                if (!node.isConstructorCall()) return

                val containingClass = node.resolve()?.containingClass ?: return
                if (containingClass.inheritsFrom(FqViewModelName)) {
                    context.report(
                        ISSUE,
                        node,
                        context.getNameLocation(node),
                        "Constructing a view model in a composable"
                    )
                }
            }
        }
    }

    companion object {
        private val FqViewModelName = Name(Package("androidx.lifecycle"), "ViewModel")

        val ISSUE =
            Issue.create(
                "ViewModelConstructorInComposable",
                "Constructing a view model in a composable",
                "View models should not be constructed directly inside composable" +
                    " functions. Instead you should use the lifecycle viewmodel extension" +
                    "functions e.g. viewModel<MyViewModel>()",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ViewModelConstructorInComposableDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
