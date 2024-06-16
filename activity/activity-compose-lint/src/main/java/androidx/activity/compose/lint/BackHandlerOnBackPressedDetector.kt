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

class BackHandlerOnBackPressedDetector : Detector(), Detector.UastScanner, SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> =
        listOf(PredictiveBackHandler.shortName, BackHandler.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(ComposePackageName)) {
            // Find the back lambda
            val backLambda =
                computeKotlinArgumentMapping(node, method)
                    .orEmpty()
                    .filter { (_, parameter) -> parameter.name == OnBack }
                    .keys
                    .filterIsInstance<ULambdaExpression>()
                    .firstOrNull() ?: return

            // If the parameter is not referenced, immediately trigger the warning
            val unreferencedParameter = backLambda.findUnreferencedParameters().firstOrNull()
            if (unreferencedParameter == null) {
                // If the parameter is referenced, we need to make sure it doesn't call
                // onBackPressed
                val lambdaExpression = backLambda.sourcePsi as? KtLambdaExpression
                // Find all of the reference inside of the lambda
                val references =
                    lambdaExpression
                        ?.functionLiteral
                        ?.collectDescendantsOfType<KtSimpleNameExpression>()
                // Check for references to OnBackPressed
                val matchingReferences =
                    references
                        ?.filter { it.getReferencedName() == OnBackPressed.shortName }
                        .orEmpty()
                // If references call onBackPressed(), trigger the warning
                if (matchingReferences.isNotEmpty()) {
                    matchingReferences.forEach { reference ->
                        val location = reference.let { context.getLocation(it) }
                        context.report(
                            InvalidOnBackPressed,
                            node,
                            location,
                            "Should not call onBackPressed inside of BackHandler"
                        )
                    }
                }
            }
        }
    }

    companion object {
        val InvalidOnBackPressed =
            Issue.create(
                    id = "OnBackPressedInsideOfBackHandler",
                    briefDescription =
                        "Do not call onBackPressed() within" + "BackHandler/PredictiveBackHandler",
                    explanation =
                        """You should not used OnBackPressedCallback for non-UI cases. If you
                |add a callback, you have to handle back completely in the callback.
            """,
                    category = Category.CORRECTNESS,
                    severity = Severity.WARNING,
                    implementation =
                        Implementation(
                            BackHandlerOnBackPressedDetector::class.java,
                            EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                        )
                )
                .addMoreInfo(
                    "https://developer.android.com/guide/navigation/custom-back/" +
                        "predictive-back-gesture#ui-logic"
                )
    }
}

private val ComposePackageName = Package("androidx.activity.compose")
private val PredictiveBackHandler = Name(ComposePackageName, "PredictiveBackHandler")
private val BackHandler = Name(ComposePackageName, "BackHandler")
private val ActivityPackageName = Package("androidx.activity")
private val OnBackPressed = Name(ActivityPackageName, "onBackPressed")
private val OnBack = "onBack"
