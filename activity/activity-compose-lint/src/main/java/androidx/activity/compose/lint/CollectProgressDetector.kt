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

package androidx.activity.compose.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.findUnreferencedParameters
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
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression

class CollectProgressDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(PredictiveBackHandler.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(PackageName)) {
            // Find the back lambda
            val backLambda =
                computeKotlinArgumentMapping(node, method)
                    .orEmpty()
                    .asSequence()
                    .filter { (_, parameter) -> parameter.name == "onBack" }
                    .mapNotNull { (key, _) -> key as? ULambdaExpression }
                    .firstOrNull() ?: return

            // If the parameter is not referenced, immediately trigger the warning
            val unreferencedParameter = backLambda.findUnreferencedParameters().firstOrNull()
            if (unreferencedParameter != null) {
                val location =
                    unreferencedParameter.parameter?.let { context.getLocation(it) }
                        ?: context.getLocation(backLambda)
                val name = unreferencedParameter.name
                context.report(
                    NoCollectCallFound,
                    node,
                    location,
                    "You must call collect() on Flow $name"
                )
            } else {
                // If the parameter is referenced, we need to make sure it calls collect()
                val lambdaExpression = backLambda.sourcePsi as? KtLambdaExpression
                // Find all of the reference inside of the lambda
                val references =
                    lambdaExpression
                        ?.functionLiteral
                        ?.collectDescendantsOfType<KtSimpleNameExpression>()
                // Make sure one of the references calls collect
                val matchingReferences =
                    references
                        ?.filter {
                            it.getReferencedName() == Collect.shortName ||
                                it.getReferencedName() == CollectIndexed.shortName ||
                                it.getReferencedName() == CollectLatest.shortName
                        }
                        .orEmpty()
                // If no references call collect(), trigger the warning
                if (matchingReferences.isEmpty()) {
                    val parameter = references?.firstOrNull()
                    val location =
                        parameter?.let { context.getLocation(it) }
                            ?: context.getLocation(backLambda)
                    val name = lambdaExpression?.name
                    context.report(
                        NoCollectCallFound,
                        node,
                        location,
                        "You must call collect() on Flow $name"
                    )
                }
            }
        }
    }

    companion object {
        val NoCollectCallFound =
            Issue.create(
                "NoCollectCallFound",
                "You must call collect on the given progress flow when using PredictiveBackHandler",
                "You must call collect on the progress in the onBack function. The collect call " +
                    "is what properly splits the callback so it knows what to do when the back " +
                    "gestures is started vs when it is completed. Failing to call collect will cause " +
                    "all code in the block to run when the gesture is started.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    CollectProgressDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}

private val PackageName = Package("androidx.activity.compose")
private val PredictiveBackHandler = Name(PackageName, "PredictiveBackHandler")
private val CoroutinesPackage = Package("kotlinx.coroutines.flow.collect")
private val Collect = Name(CoroutinesPackage, "collect")
private val CollectIndexed = Name(CoroutinesPackage, "collectIndexed")
private val CollectLatest = Name(CoroutinesPackage, "collectLatest")
