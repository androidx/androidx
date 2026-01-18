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

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isComposable
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
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.resolveToUElement

/**
 * [Detector] that checks calls to Flow operator functions (such as map) to make sure they don't
 * happen inside the body of a composable function / lambda. This detector defines an operator
 * function as any function with a receiver of Flow, and a return type of Flow, such as:
 *
 * fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> fun <T>
 * Flow<T>.drop(count: Int): Flow<T>
 */
class ComposableFlowOperatorDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolveToUElement() as? UMethod ?: return
                val source = node.sourcePsi as? KtCallExpression ?: return

                // We are calling a flow operator function
                if (source.isFlowOperator()) {
                    if (!method.isComposable && node.isInvokedWithinComposable()) {
                        context.report(
                            FlowOperatorInvokedInComposition,
                            node,
                            context.getNameLocation(node),
                            "Flow operator functions should not be invoked within composition",
                        )
                    }
                }
            }
        }

    companion object {
        val FlowOperatorInvokedInComposition =
            Issue.create(
                "FlowOperatorInvokedInComposition",
                "Flow operator functions should not be invoked within composition",
                "Calling a Flow operator function within composition will result in a new " +
                    "Flow being created every recomposition, which will reset collectAsState() and " +
                    "cause other related problems. Instead Flow operators should be called inside " +
                    "`remember`, or a side effect such as LaunchedEffect.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ComposableFlowOperatorDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}

/**
 * @return whether this [UMethod] is an extension function with a receiver of Flow (or a subtype),
 *   and a return type of Flow (or a subtype)
 */
private fun KtCallExpression.isFlowOperator(): Boolean {
    analyze(this) {
        val functionCallSymbol =
            resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol?.symbol
                ?: return false
        // Ignore non-extension functions
        if (!functionCallSymbol.isExtension) return false
        val returnType = functionCallSymbol.returnType
        // We check the symbol.receiverType instead of signature.receiverType to get the defined
        // (non substituted type). We want to ignore generic T.foo() extensions like flow.apply {}.
        val extensionReceiverType = functionCallSymbol.receiverType ?: return false

        return extensionReceiverType.isSubtypeOf(FlowName.classId) &&
            returnType.isSubtypeOf(FlowName.classId)
    }
}

private val FlowPackageName = Package("kotlinx.coroutines.flow")
private val FlowName = Name(FlowPackageName, "Flow")
