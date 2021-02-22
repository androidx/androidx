/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.EnumSet

/**
 * Warns when a developer uses both idle + battery charging constraints in WorkManager.
 */
class IdleBatteryChargingConstraintsDetector : Detector(), SourceCodeScanner {
    companion object {

        private const val DESCRIPTION = "Constraints may not be met for some devices"

        val ISSUE = Issue.create(
            id = "IdleBatteryChargingConstraints",
            briefDescription = DESCRIPTION,
            explanation = """
                Some devices are never considered charging and idle at the same time.
                Consider removing one of these constraints.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                IdleBatteryChargingConstraintsDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("setRequiresDeviceIdle")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        var reported = false
        if (context.evaluator.isMemberInClass(
                method,
                "androidx.work.Constraints.Builder"
            ) && node.isArgumentTrue(context)
        ) {
            val name = node.identifierName()
            val sourcePsi = node.receiver?.sourcePsi
            if (sourcePsi != null) {
                // We need to walk both backwards to the nearest block expression and look for
                // setRequiresCharging(true) on Constraints
                val blockExpression = sourcePsi.toUElement()?.getParentOfType<UBlockExpression>()
                val visitor = object : AbstractUastVisitor() {
                    override fun visitCallExpression(node: UCallExpression): Boolean {
                        val variableName = node.identifierName()
                        if (node.methodName == "setRequiresCharging" &&
                            node.isArgumentTrue(context) &&
                            // Same variable name
                            name == variableName &&
                            node.receiverType?.canonicalText == "androidx.work.Constraints.Builder"
                        ) {
                            if (!reported) {
                                reported = true
                                context.report(ISSUE, context.getLocation(node), DESCRIPTION)
                            }
                        }
                        return true
                    }
                }
                // Note: We need to navigate the sourcePsi for call expressions.
                // blockExpression.accept(...) will NOT do what you think it does.
                blockExpression?.sourcePsi?.toUElement()?.accept(visitor)
            }
        }
    }

    fun UCallExpression.isArgumentTrue(context: JavaContext): Boolean {
        if (valueArgumentCount > 0) {
            val value = ConstantEvaluator.evaluate(context, valueArguments.first())
            return value == true
        }
        return false
    }

    fun UCallExpression.identifierName(): String? {
        var current = receiver
        while (current != null && current !is USimpleNameReferenceExpression) {
            current = (current as? UQualifiedReferenceExpression)?.receiver
        }
        if (current != null && current is USimpleNameReferenceExpression) {
            return current.identifier
        }
        return null
    }
}
