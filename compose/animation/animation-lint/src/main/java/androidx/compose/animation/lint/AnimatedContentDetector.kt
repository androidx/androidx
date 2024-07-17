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

package androidx.compose.animation.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression

/**
 * [Detector] that checks `AnimatedContent` usages for correctness.
 *
 * AnimatedContent provides a value for targetState (`T`) in the `content` lambda. It is always an
 * error to not use this value, as AnimatedContent works by emitting content with a value
 * corresponding to the `from` and `to` states - if this value is not read, then `AnimatedContent`
 * will end up animating in and out the same content on top of each other.
 *
 * Likewise, `contentKey` should also use the provided targetState (`T`) to calculate its result.
 */
class AnimatedContentDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(AnimatedContent.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Animation.PackageName)) return

        var contentLambdaExpression: ULambdaExpression? = null
        var contentKeyLambdaExpression: ULambdaExpression? = null

        // We check for unused lambda parameter in the `content` lambda and in the `contentKey`
        // lambda, so we first need to capture the corresponding lambdas.
        computeKotlinArgumentMapping(node, method).orEmpty().forEach { (expression, parameter) ->
            when (parameter.name) {
                "content" -> {
                    if (expression is ULambdaExpression) {
                        contentLambdaExpression = expression
                    }
                }
                "contentKey" -> {
                    if (expression is ULambdaExpression) {
                        contentKeyLambdaExpression = expression
                    }
                }
            }
        }

        // Unused lambda parameter check for `content` lambda
        contentLambdaExpression?.let { lambdaArgument ->
            findAndReportUnusedTargetStateIssue(
                lambdaArgument = lambdaArgument,
                node = node,
                context = context,
                issue = UnusedContentLambdaTargetStateParameter
            )
        }

        // Unused lambda parameter check for `contentKey` lambda
        contentKeyLambdaExpression?.let { lambdaArgument ->
            findAndReportUnusedTargetStateIssue(
                lambdaArgument = lambdaArgument,
                node = node,
                context = context,
                issue = UnusedTargetStateInContentKeyLambda
            )
        }
    }

    private fun findAndReportUnusedTargetStateIssue(
        lambdaArgument: ULambdaExpression,
        node: UCallExpression,
        context: JavaContext,
        issue: Issue
    ) {
        lambdaArgument.findUnreferencedParameters().forEach { unreferencedParameter ->
            val location =
                unreferencedParameter.parameter?.let { context.getLocation(it) }
                    ?: context.getLocation(lambdaArgument)
            val name = unreferencedParameter.name
            context.report(
                issue = issue,
                scope = node,
                location = location,
                message = "Target state parameter `$name` is not used"
            )
        }
    }

    companion object {
        val UnusedContentLambdaTargetStateParameter =
            Issue.create(
                id = "UnusedContentLambdaTargetStateParameter",
                briefDescription =
                    "AnimatedContent calls should use the provided `T` parameter in the content lambda",
                explanation =
                    "`content` lambda in AnimatedContent works as a lookup function that returns the " +
                        "corresponding content based on the parameter (a state of type `T`). It is " +
                        "important for this lambda to return content *specific* to the input parameter, " +
                        "so that the different contents can be properly animated. Not using the input " +
                        "parameter to the content lambda will result in the same content for different " +
                        "input (i.e. target state) and therefore an erroneous transition between the " +
                        "exact same content.`",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        AnimatedContentDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                    )
            )

        val UnusedTargetStateInContentKeyLambda =
            Issue.create(
                id = "UnusedTargetStateInContentKeyLambda",
                briefDescription =
                    "`contentKey` lambda in AnimatedContent should always use " +
                        "the provided `T` parameter.",
                explanation =
                    "In `AnimatedContent`, the `contentKey` lambda may be used when the " +
                        "`targetState` is expected to mutate frequently but not all mutations are " +
                        "desired to be considered a target state change. So `contentKey` is expected to " +
                        "always use the given `targetState` parameter to calculate its result.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        AnimatedContentDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                    )
            )
    }
}

private val AnimatedContent = Name(Names.Animation.PackageName, "AnimatedContent")
