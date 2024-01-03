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

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.Package
import androidx.compose.lint.PackageName
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression

@Suppress("UnstableApiUsage")
class SuspiciousCompositionLocalModifierReadDetector : Detector(), SourceCodeScanner {

    private val NodeLifecycleCallbacks = listOf("onAttach", "onDetach")

    override fun getApplicableMethodNames(): List<String> =
        listOf(Names.Ui.Node.CurrentValueOf.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.Node.PackageName)) return
        reportIfAnyParentIsNodeLifecycleCallback(context, node, node)
    }

    private tailrec fun reportIfAnyParentIsNodeLifecycleCallback(
        context: JavaContext,
        node: UElement?,
        usage: UCallExpression
    ) {
        if (node == null) {
            return
        } else if (node is UMethod) {
            if (node.containingClass.isClConsumerNode()) {
                if (node.name in NodeLifecycleCallbacks) {
                    report(context, usage) { localBeingRead ->
                        val action = node.name.removePrefix("on")
                            .replaceFirstChar { it.lowercase() }

                        "Reading $localBeingRead in ${node.name} will only access the " +
                            "CompositionLocal's value when the modifier is ${action}ed. " +
                            "To be notified of the latest value of the CompositionLocal, read " +
                            "the value in one of the modifier's other callbacks."
                    }
                } else if (node.isConstructor) {
                    report(context, usage) {
                        "CompositionLocals cannot be read in modifiers before the node is attached."
                    }
                }
            }
            return
        } else if (node is KotlinUFunctionCallExpression && node.isLazyDelegate()) {
            report(context, usage) { localBeingRead ->
                "Reading $localBeingRead lazily will only access the CompositionLocal's value " +
                    "once. To be notified of the latest value of the CompositionLocal, read " +
                    "the value in one of the modifier's callbacks."
            }
            return
        }

        reportIfAnyParentIsNodeLifecycleCallback(context, node.uastParent, usage)
    }

    private inline fun report(
        context: JavaContext,
        usage: UCallExpression,
        message: (compositionLocalName: String) -> String
    ) {
        val localBeingRead = usage.getArgumentForParameter(1)?.sourcePsi?.text
            ?: "a composition local"

        context.report(
            SuspiciousCompositionLocalModifierRead,
            context.getLocation(usage),
            message(localBeingRead)
        )
    }

    private fun PsiClass?.isClConsumerNode(): Boolean =
        this?.implementsListTypes
            ?.any { it.canonicalText == ClConsumerModifierNode } == true

    private fun KotlinUFunctionCallExpression.isLazyDelegate(): Boolean =
        resolve()?.run { isInPackageName(Package("kotlin")) && name == "lazy" } == true

    companion object {
        private const val ClConsumerModifierNode =
            "androidx.compose.ui.node.CompositionLocalConsumerModifierNode"

        val SuspiciousCompositionLocalModifierRead = Issue.create(
            "SuspiciousCompositionLocalModifierRead",
            "CompositionLocals should not be read in Modifier.onAttach() or Modifier.onDetach()",
            "Jetpack Compose is unable to send updated values of a CompositionLocal when it's " +
                "read in a Modifier.Node's initializer and onAttach() or onDetach() callbacks. " +
                "Modifier.Node's callbacks are not aware of snapshot reads, and their lifecycle " +
                "callbacks are not invoked on every recomposition. If you read a " +
                "CompositionLocal in onAttach() or onDetach(), you will only get the " +
                "CompositionLocal's value once at the moment of the read, which may lead to " +
                "unexpected behaviors. We recommend instead reading CompositionLocals at " +
                "time-of-use in callbacks that apply your Modifier's behavior, like measure() " +
                "for LayoutModifierNode, draw() for DrawModifierNode, and so on. To observe the " +
                "value of the CompositionLocal manually, extend from the ObserverNode interface " +
                "and place the read inside an observeReads {} block within the " +
                "onObservedReadsChanged() callback.",
            Category.CORRECTNESS, 3, Severity.ERROR,
            Implementation(
                SuspiciousCompositionLocalModifierReadDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            )
        )
    }
}
