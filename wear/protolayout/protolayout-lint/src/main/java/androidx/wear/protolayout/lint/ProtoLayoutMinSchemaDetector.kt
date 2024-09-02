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

package androidx.wear.protolayout.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.ApiConstraint
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.VersionChecks.Companion.isPrecededByVersionCheckExit
import com.android.tools.lint.detector.api.VersionChecks.Companion.isWithinVersionCheckConditional
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.getContainingUVariable

// TODO: b/308552481 - Add support for empty Builder construction (right now only setters are
// annotated).

/**
 * The linter for detecting usage of ProtoLayout APIs (newer than schema 1.0) without properly
 * checking the availability of them. Currently it relies on SDK checks that correspond to each
 * ProtoLayout schema version.
 *
 * All **method** calls (where the callee has RequiresSchemaVersion annotation) are inspected to see
 * if the call site meets the condition for the schema version. This means the caller have to either
 * have a compatible RequiresSchemaVersion annotation (with greater than or equal version to the one
 * needed), or the call to be within the scope of SDK version that matches the schema version. For
 * example if the call is inside a SDK 34 version check, calls to schema versions including 1.300
 * and below are all allowed.
 *
 * Note that RequiresSchemaVersion annotations on class are ignored (except for Builder classes).
 */
class ProtoLayoutMinSchemaDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val method = node.resolve() ?: return
                checkMethodCall(context, node, method)
            }
        }

    internal fun checkMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // Does the callee have a RequiredSchemaVersion annotation?
        val calleeAnnotation = method.getSchemaVersionAnnotation(context) ?: return
        val callerSchemaAnnotation =
            (node.getContainingUMethod() ?: node.getContainingUVariable())
                ?.getSchemaVersionAnnotation(context)

        if (calleeAnnotation.major <= 1 && calleeAnnotation.minor <= 100) {
            // We only care about 1.2 and above
            return
        }

        // Check callers RequiresSchemaVersion annotation and TargetApi/RequiresApi annotation.
        if (
            !calleeAnnotation.isCoveredBySchemaAnnotation(callerSchemaAnnotation) &&
                !calleeAnnotation.isCoveredByTargetApi(context, node)
        ) {
            context.report(
                issue = ISSUE,
                location = context.getLocation(node),
                message =
                    "This API is not guaranteed to be available on the device" +
                        " (requires schema $calleeAnnotation).",
            )
        }
    }

    private fun PsiModifierListOwner.getSchemaVersionAnnotation(
        context: JavaContext
    ): SchemaAnnotation? {
        return if (this is UAnnotated) {
            findAnnotation(REQUIRES_SCHEMA_ANNOTATION)?.asSchemaAnnotation(context)
        } else {
            context.evaluator
                .getAnnotationInHierarchy(this, REQUIRES_SCHEMA_ANNOTATION)
                ?.asSchemaAnnotation(context)
        }
    }

    private fun UAnnotation.asSchemaAnnotation(context: JavaContext): SchemaAnnotation? {
        val majorValue = attributeValue(context, "major")
        val minorValue = attributeValue(context, "minor")
        return if (majorValue != null && minorValue != null) {
            SchemaAnnotation(majorValue, minorValue)
        } else {
            null
        }
    }

    private fun UAnnotation.attributeValue(context: JavaContext, attrName: String): Int? {
        val attrValue = findAttributeValue(attrName) ?: return null
        val value =
            ConstantEvaluator.evaluate(context, attrValue) ?: (attrValue as? UResolvable)?.resolve()
        return when (value) {
            is PsiField -> value.computeConstantValue() as Int?
            is Int -> value
            else -> null
        }
    }

    private data class SchemaAnnotation(val major: Int, val minor: Int) {
        fun isCoveredBySchemaAnnotation(other: SchemaAnnotation?): Boolean =
            other != null && (major < other.major || (major == other.major && minor <= other.minor))

        fun isCoveredByTargetApi(context: JavaContext, node: UCallExpression): Boolean {
            val minSdkConstraint = ApiConstraint.get(toMinSdk() ?: return false)
            return isWithinVersionCheckConditional(context, node, minSdkConstraint) ||
                isPrecededByVersionCheckExit(context, node, minSdkConstraint) ||
                hasAtLeastTargetApiAnnotation(context.evaluator, node, minSdkConstraint)
        }

        fun toMinSdk(): Int? =
            when (minor) {
                in Int.MIN_VALUE..100 -> null
                in 101..200 -> 33
                in 201..300 -> 34
                in 301..400 -> 35
                in 401..500 -> 36
                else -> Int.MAX_VALUE
            }

        override fun toString() = "$major.$minor"
    }

    companion object {
        @JvmField
        val ISSUE =
            Issue.create(
                id = "ProtoLayoutMinSchema",
                briefDescription =
                    "ProtoLayout feature is not guaranteed to be available " +
                        "on the target device API.",
                explanation =
                    """
            Using features that are not supported by an older ProtoLayout renderer/evaluator, can lead to unexpected rendering or invalid results (for expressions).

            Each Wear OS platform version has a guaranteed minimum ProtoLayout schema version.
            On API 33, all consumers for ProtoLayout support at least Schema version 1.2 (major=1, minor=200).
            On API 34, all consumers for ProtoLayout support at least Schema version 1.3 (major=1, minor=300).

            You can use those newer features through conditional Android API checks, or by increasing the minSdk for your project.
            You can also annotate your methods with @RequiresApi or @RequiresSchemaAnnotation if you know they require the
            corresponding version.
            Note that @RequiresSchemaVersion annotation on classes are mostly ignored (except for Builder classes).
            """,
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                androidSpecific = true,
                implementation =
                    Implementation(ProtoLayoutMinSchemaDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )

        const val REQUIRES_SCHEMA_ANNOTATION =
            "androidx.wear.protolayout.expression.RequiresSchemaVersion"
    }
}
