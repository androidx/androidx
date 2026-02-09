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

package androidx.compose.foundation.lint

import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.computeKotlinArgumentMapping
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks if `preserveFirstVisibleItem` is used with key parameters in
 * items/itemsIndexed calls within LazyColumn/LazyRow.
 */
class PreserveFirstVisibleItemDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(
            FoundationNames.Lazy.LazyColumn.shortName,
            FoundationNames.Lazy.LazyRow.shortName,
        )

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(FoundationNames.Lazy.PackageName)) {
            return
        }
        // Check if preserveFirstVisibleItem is explicitly declared
        val argumentMapping = computeKotlinArgumentMapping(node, method).orEmpty()
        
        val preserveFirstVisibleItemArg = argumentMapping
            .entries
            .firstOrNull { (_, param) -> param.name == "preserveFirstVisibleItem" }

        // Only warn if explicitly set
        if (preserveFirstVisibleItemArg == null) {
            return
        }
        // Find the content lambda
        val contentLambda = argumentMapping
            .filter { (_, parameter) -> parameter.name == "content" }
            .keys
            .filterIsInstance<ULambdaExpression>()
            .firstOrNull()
        
        if (contentLambda == null) {
            return
        }

        // Check for item/items/itemsIndexed calls without keys
        contentLambda.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(callNode: UCallExpression): Boolean {
                val methodName = callNode.methodName
                if (methodName == "items" || methodName == "itemsIndexed") {
                    val itemsArgMapping = computeKotlinArgumentMapping(callNode,
                        callNode.resolve() ?: return false).orEmpty()

                    val hasKeyParameter = itemsArgMapping.entries.any { (arg, param) ->
                        param.name == "key" && (arg !is ULiteralExpression || !arg.isNull)
                    }

                    if (!hasKeyParameter) {
                        context.report(
                            PreserveFirstVisibleItemRequiresKey,
                            callNode,
                            context.getLocation(callNode),
                            "`$methodName` must provide a `key` parameter when " +
                                "`preserveFirstVisibleItem` is explicitly set. The " +
                                "`preserveFirstVisibleItem` feature only works with keyed items."
                        )
                    }
                }
                return super.visitCallExpression(callNode)
            }
        })
    }

    companion object {
        val PreserveFirstVisibleItemRequiresKey = Issue.create(
            "PreserveFirstVisibleItemRequiresKey",
            "Specifying `preserveFirstVisibleItem` in LazyList requires providing `key` parameters",
            "The `preserveFirstVisibleItem` parameter only has a meaningful effect when item " +
                "keys are provided. It maintains scroll position based on these keys when items " +
                "are added or removed." +
                "\n\nProvide a stable and unique key for each item: " +
                "`items(list, key = { it.id }) { ... }`, or " +
                "`itemsIndexed(list, key = { _, item -> item.id }) { ... }` ",
            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            Implementation(
                PreserveFirstVisibleItemDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
