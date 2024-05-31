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

package androidx.compose.animation.core.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass

private const val ANIMATION_CORE_PACKAGE = "androidx.compose.animation.core"
private const val GEOMETRY_PACKAGE = "androidx.compose.ui.geometry"
private const val UNIT_PACKAGE = "androidx.compose.ui.unit"
private const val ARC_ANIMATION_SPEC_NAME = "ArcAnimationSpec"
private const val ARC_KEYFRAMES_SPEC_NAME = "keyframesWithArcs"
private const val OFFSET_NAME = "Offset"
private const val INT_OFFSET_NAME = "IntOffset"
private const val DP_OFFSET_NAME = "DpOffset"
private const val ARC_SPEC_FQ_NAME = "$ANIMATION_CORE_PACKAGE.$ARC_ANIMATION_SPEC_NAME"
private const val OFFSET_FQ_NAME = "$GEOMETRY_PACKAGE.$OFFSET_NAME"
private const val INT_OFFSET_FQ_NAME = "$UNIT_PACKAGE.$INT_OFFSET_NAME"
private const val DP_OFFSET_FQ_NAME = "$UNIT_PACKAGE.$DP_OFFSET_NAME"
private val preferredArcAnimationTypes by
    lazy(LazyThreadSafetyMode.NONE) { setOf(OFFSET_FQ_NAME, INT_OFFSET_FQ_NAME, DP_OFFSET_FQ_NAME) }

/**
 * Lint to inform of the expected usage for `ArcAnimationSpec` (and its derivative)
 * `keyframesWithArcs`.
 */
class ArcAnimationSpecTypeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                when (node.classReference?.resolvedName) {
                    ARC_ANIMATION_SPEC_NAME -> detectTypeParameterInArcAnimation(node)
                }
            }

            private fun detectTypeParameterInArcAnimation(node: UCallExpression) {
                val typeArg = node.typeArguments.firstOrNull() ?: return
                val qualifiedTypeName = typeArg.canonicalText
                // Check that the given type to the call is one of: Offset, IntOffset, DpOffset
                if (preferredArcAnimationTypes.contains(qualifiedTypeName)) {
                    return
                }
                // Node class resolution might be slower, do last
                val fqClassName =
                    (node.classReference?.tryResolveUDeclaration() as? UClass)?.qualifiedName
                // Verify that the method calls are from the expected animation classes, otherwise,
                // skip
                // check
                if (fqClassName != ARC_SPEC_FQ_NAME) {
                    return
                }
                // Generate Lint
                context.report(
                    issue = ArcAnimationSpecTypeIssue,
                    scope = node,
                    location = context.getNameLocation(node),
                    message =
                        "Arc animation is intended for 2D values such as Offset, IntOffset or " +
                            "DpOffset.\nOtherwise, the animation might not be what you expect."
                )
            }
        }

    companion object {
        val ArcAnimationSpecTypeIssue =
            Issue.create(
                id = "ArcAnimationSpecTypeIssue",
                briefDescription =
                    "$ARC_ANIMATION_SPEC_NAME is " +
                        "designed for 2D values. Particularly, for positional values such as Offset.",
                explanation =
                    "$ARC_ANIMATION_SPEC_NAME is designed for" +
                        " 2D values. Particularly, for positional values such as Offset.\nTrying to use " +
                        "it for values of different dimensions (Float, Size, Color, etc.) will result " +
                        "in unpredictable animation behavior.",
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.INFORMATIONAL,
                implementation =
                    Implementation(
                        ArcAnimationSpecTypeDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE)
                    )
            )
    }
}
