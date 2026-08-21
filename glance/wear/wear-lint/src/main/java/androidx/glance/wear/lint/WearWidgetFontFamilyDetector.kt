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

package androidx.glance.wear.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UCallExpression

/**
 * A [Detector] that flags redundant `fontFamily` assignments in Wear Widgets.
 *
 * Because the Wear Widget host displays all text elements strictly in system font, custom
 * `fontFamily` configurations act as a complete no-op. This detector identifies these ignored
 * parameters to prevent dead code and developer confusion.
 */
class WearWidgetFontFamilyDetector : Detector(), Detector.UastScanner {

    override fun getApplicableMethodNames(): List<String> = listOf(REMOTE_TEXT, COPY, MERGE)

    override fun getApplicableConstructorTypes(): List<String> = listOf(REMOTE_TEXT_STYLE)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = node.methodName ?: return
        val isTarget =
            when (methodName) {
                REMOTE_TEXT -> isRemoteTextMethod(context, method)
                COPY,
                MERGE -> isRemoteTextStyleMember(context, method)
                else -> false
            }
        if (!isTarget) return

        checkFontFamilyArgument(context, node, method)
    }

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        if (!isRemoteTextStyleMember(context, constructor)) return

        checkFontFamilyArgument(context, node, constructor)
    }

    private fun isRemoteTextMethod(context: JavaContext, method: PsiMethod): Boolean {
        val packageName = context.evaluator.getPackage(method)?.qualifiedName
        if (
            packageName == WEAR_REMOTE_MATERIAL3_PACKAGE ||
                packageName == COMPOSE_REMOTE_LAYOUT_PACKAGE
        ) {
            return true
        }
        val containingClass = method.containingClass?.qualifiedName ?: return false
        return containingClass.startsWith("$WEAR_REMOTE_MATERIAL3_PACKAGE.") ||
            containingClass.startsWith("$COMPOSE_REMOTE_LAYOUT_PACKAGE.")
    }

    private fun isRemoteTextStyleMember(context: JavaContext, method: PsiMethod) =
        context.evaluator.isMemberInSubClassOf(method, REMOTE_TEXT_STYLE, false)

    private fun checkFontFamilyArgument(
        context: JavaContext,
        node: UCallExpression,
        methodOrConstructor: PsiMethod,
    ) {
        val mapping = context.evaluator.computeArgumentMapping(node, methodOrConstructor)
        val argExpr =
            mapping.entries.firstOrNull { it.value.name == FONT_FAMILY_PARAM }?.key ?: return

        val sourcePsi = argExpr.sourcePsi
        val targetPsi =
            sourcePsi as? KtValueArgument
                ?: sourcePsi?.parent as? KtValueArgument
                ?: sourcePsi
                ?: return

        val issueLocation = context.getLocation(targetPsi)
        val removalLocation = computeRemovalRange(context, targetPsi) ?: issueLocation

        val fix: LintFix =
            fix().name(QUICK_FIX_NAME).replace().range(removalLocation).with("").build()

        context.report(
            issue = CUSTOM_FONT_FAMILY_ISSUE,
            location = issueLocation,
            message = REPORT_MESSAGE,
            quickfixData = fix,
        )
    }

    private fun computeRemovalRange(context: JavaContext, targetPsi: PsiElement): Location? {
        val source = context.getContents() ?: return null
        val start = targetPsi.textRange.startOffset
        val end = targetPsi.textRange.endOffset

        if (start < 0 || end > source.length || start > end) return null

        // 1. Scan forward to see if there is a comma
        var scanForward = end
        while (scanForward < source.length && source[scanForward].isWhitespace()) {
            scanForward++
        }

        if (scanForward < source.length && source[scanForward] == ',') {
            val commaPos = scanForward

            // Scan past the comma to find the next non-whitespace character
            var afterComma = commaPos + 1
            while (afterComma < source.length && source[afterComma].isWhitespace()) {
                afterComma++
            }

            val nextChar = if (afterComma < source.length) source[afterComma] else '\u0000'
            if (nextChar == ')' || nextChar == '}' || nextChar == ']') {
                // Case A: Last parameter WITH a trailing comma.
                // Delete backward to consume the preceding newline/indentation, the param, and the
                // trailing comma.
                var scanBackward = start - 1
                while (scanBackward >= 0 && source[scanBackward].isWhitespace()) {
                    scanBackward--
                }
                return Location.create(context.file, source, scanBackward + 1, commaPos + 1)
            } else {
                // Case B: Middle parameter or first parameter.
                // Delete the param, the trailing comma, and the trailing newline/indentation.
                return Location.create(context.file, source, start, afterComma)
            }
        }

        // 2. Case C: Last parameter WITHOUT a trailing comma.
        // Delete backward to consume the preceding comma, newline/indentation, and the param.
        var scanBackward = start - 1
        while (scanBackward >= 0) {
            val c = source[scanBackward]
            if (c == ',') {
                return Location.create(context.file, source, scanBackward, end)
            }
            if (c == '(' || c == '{' || c == '[' || !c.isWhitespace()) break
            scanBackward--
        }

        // 3. Fallback (Single argument with no commas)
        return Location.create(context.file, source, start, end)
    }

    companion object {
        private const val REMOTE_TEXT = "RemoteText"
        private const val COPY = "copy"
        private const val MERGE = "merge"
        private const val FONT_FAMILY_PARAM = "fontFamily"

        private const val REMOTE_TEXT_STYLE =
            "androidx.compose.remote.creation.compose.text.RemoteTextStyle"
        private const val WEAR_REMOTE_MATERIAL3_PACKAGE = "androidx.wear.compose.remote.material3"
        private const val COMPOSE_REMOTE_LAYOUT_PACKAGE =
            "androidx.compose.remote.creation.compose.layout"

        private const val ISSUE_ID = "WearWidgetCustomFontFamily"
        private const val BRIEF_DESCRIPTION = "Redundant fontFamily parameter in Wear Widget"
        private const val REPORT_MESSAGE =
            "The fontFamily parameter is a no-op in Wear widgets and should be removed."
        private const val EXPLANATION =
            "The Wear Widget host displays all text elements strictly in system font. " +
                "Modifying fontFamily parameters in Remote Compose layouts serves as a no-op, " +
                "which leads to redundant code configurations."

        private const val QUICK_FIX_NAME = "Remove fontFamily parameter"

        @JvmField
        val CUSTOM_FONT_FAMILY_ISSUE: Issue =
            Issue.create(
                id = ISSUE_ID,
                briefDescription = BRIEF_DESCRIPTION,
                explanation = EXPLANATION,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(WearWidgetFontFamilyDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
