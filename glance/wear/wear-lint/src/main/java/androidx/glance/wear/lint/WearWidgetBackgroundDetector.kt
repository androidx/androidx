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
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElement

/**
 * A [Detector] that enforces that all Wear Widgets specify a valid background within
 * [WearWidgetDocument] to maintain brand compliance and prevent rendering defects.
 */
class WearWidgetBackgroundDetector : Detector(), Detector.UastScanner {

    override fun getApplicableConstructorTypes(): List<String> = listOf(WEAR_WIDGET_DOCUMENT)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        // Add null check here, although the background parameter is non-nullable in Kotlin, to
        // avoid crashing the Lint engine during live editing in the IDE when the user is actively
        // typing and hasn't yet provided the argument. The Kotlin compiler will naturally flag the
        // missing argument as an error.
        val backgroundArg = node.findBackgroundArgument(context) ?: return

        if (backgroundArg.isEmptyBrushReference()) {
            context.report(
                EMPTY_BACKGROUND_ISSUE,
                backgroundArg,
                context.getLocation(backgroundArg),
                "WearWidgetDocument background cannot be an empty WearWidgetBrush reference",
            )
            return
        }

        if (backgroundArg.isBlackOrTransparent(context)) {
            context.report(
                INVALID_BACKGROUND_ISSUE,
                backgroundArg,
                context.getLocation(backgroundArg),
                "WearWidgetDocument background cannot be black or transparent",
            )
            return
        }
    }

    companion object {
        internal const val GLANCE_WEAR_PACKAGE = "androidx.glance.wear"
        internal const val WEAR_WIDGET_DOCUMENT = "$GLANCE_WEAR_PACKAGE.WearWidgetDocument"
        internal const val WEAR_WIDGET_BRUSH = "$GLANCE_WEAR_PACKAGE.WearWidgetBrush"
        internal const val WEAR_WIDGET_BRUSH_COMPANION = "$WEAR_WIDGET_BRUSH.Companion"
        internal const val COMPOSE_COLOR = "androidx.compose.ui.graphics.Color"
        internal const val COMPOSE_COLOR_COMPANION = "$COMPOSE_COLOR.Companion"
        // Required because UAST resolves top-level Kotlin factory functions to their generated 'Kt'
        // classes
        internal const val COMPOSE_COLOR_KT = COMPOSE_COLOR + "Kt"
        internal const val ANDROID_COLOR = "android.graphics.Color"
        internal const val REMOTE_COLOR =
            "androidx.compose.remote.creation.compose.state.RemoteColor"
        internal const val REMOTE_COLOR_COMPANION = "$REMOTE_COLOR.Companion"
        internal val EMPTY_BRUSH_CLASSES = setOf(WEAR_WIDGET_BRUSH, WEAR_WIDGET_BRUSH_COMPANION)
        internal val COLOR_CLASSES =
            setOf(
                COMPOSE_COLOR,
                COMPOSE_COLOR_COMPANION,
                COMPOSE_COLOR_KT,
                ANDROID_COLOR,
                REMOTE_COLOR,
                REMOTE_COLOR_COMPANION,
            )
        internal val INVALID_COLOR_NAMES =
            setOf("black", "getBlack", "transparent", "getTransparent")
        internal val UNWRAP_EXTENSION_NAMES =
            setOf("rc", "getRc", "toInt", "toLong", "toUInt", "toULong", "toArgb")

        internal const val UINT_MASK = 0xFFFFFFFFL
        internal const val OPAQUE_BLACK_MASK = 0xFF000000L
        internal const val ALPHA_SHIFT = 24

        /**
         * The maximum depth to unwrap expressions, resolve references, and trace variable
         * initializers. This acts as a safeguard to prevent infinite recursion or Lint timeouts if
         * a developer accidentally creates a cyclic reference or deeply nested expression.
         */
        internal const val MAX_UNWRAP_DEPTH = 5

        @JvmField
        val EMPTY_BACKGROUND_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetEmptyBackground",
                briefDescription = "Wear Widgets should specify a valid background",
                explanation =
                    """
                    Wear Widgets should explicitly specify a valid background within WearWidgetDocument(...).
                    If an empty WearWidgetBrush is provided, a default background color will be applied.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(WearWidgetBackgroundDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        @JvmField
        val INVALID_BACKGROUND_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetInvalidBackground",
                briefDescription = "Wear Widgets should not use a black or transparent background",
                explanation =
                    """
                    Wear Widgets should explicitly specify a valid, non-transparent and non-black background within WearWidgetDocument(...).
                    This is to ensure the widget container remains visible and legible across different watches.
                    """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                    Implementation(WearWidgetBackgroundDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}

private fun UCallExpression.findBackgroundArgument(context: JavaContext): UExpression? {
    val method = resolve() ?: return null
    return context.evaluator.computeArgumentMapping(this, method).findArg("background")
}

private fun Map<UExpression, PsiParameter>.findArg(paramName: String): UExpression? =
    entries.firstOrNull { it.value.name == paramName }?.key

private fun UExpression.isEmptyBrushReference(): Boolean {
    val unwrapped = resolveAndUnwrap()
    val resolved = (unwrapped as? UReferenceExpression)?.resolve() ?: return false
    return resolved.getQualifiedClassName() in WearWidgetBackgroundDetector.EMPTY_BRUSH_CLASSES
}

/**
 * Evaluates whether the given [UExpression] represents a solid black or transparent
 * [WearWidgetBrush].
 *
 * This method verifies that the unwrapped expression is the WearWidgetBrush.color() top-level
 * extension function, and extracts its RemoteColor argument for evaluation.
 */
private fun UExpression.isBlackOrTransparent(context: JavaContext): Boolean {
    // The resolveAndUnwrap() operation here typically yields one of two main outcomes:
    // 1. A UCallExpression: This occurs if the background brush was constructed via a method
    // call (e.g., WearWidgetBrush.color(...)). This outcome is also reached if the brush was
    // first assigned to a local variable. It will be unwrapped to color(...)
    // 2. A UReferenceExpression: This occurs if the brush cannot be traced to a specific
    // builder call, such as when the brush is passed as a function parameter, references an
    // external property, etc. This is beyond the scope of a standard Lint check.
    val colorCall = resolveAndUnwrap() as? UCallExpression ?: return false

    if (colorCall.methodName != "color") return false

    val containingClass = colorCall.resolve()?.containingClass?.qualifiedName ?: return false
    if (!containingClass.startsWith(WearWidgetBackgroundDetector.GLANCE_WEAR_PACKAGE)) return false

    val colorArg = colorCall.valueArguments.lastOrNull() ?: return false
    return colorArg.isBlackOrTransparentColor(context)
}

/**
 * Evaluates whether the given [UExpression] represents a black or transparent color (resolving
 * standard Color constants, hex values, and RemoteColor wrapper variants).
 */
private fun UExpression.isBlackOrTransparentColor(context: JavaContext): Boolean =
    when (val unwrapped = resolveAndUnwrap()) {
        is UCallExpression -> unwrapped.isBlackOrTransparentCall(context)
        is UReferenceExpression -> unwrapped.isBlackOrTransparentReference()
        else -> unwrapped.isBlackOrTransparentConstant(context)
    }

/**
 * Analyzes color constructor, wrapper, or component builder calls to determine if they construct a
 * black or transparent color.
 */
private fun UCallExpression.isBlackOrTransparentCall(context: JavaContext): Boolean {
    val resolvedMethod = resolve() ?: return false
    val returnType = getExpressionType()
    val containingClassName = resolvedMethod.getQualifiedClassName()

    val isColorClass =
        containingClassName in WearWidgetBackgroundDetector.COLOR_CLASSES ||
            (returnType != null &&
                WearWidgetBackgroundDetector.COLOR_CLASSES.any {
                    context.evaluator.typeMatches(returnType, it)
                }) ||
            WearWidgetBackgroundDetector.COLOR_CLASSES.any {
                context.evaluator.isMemberInClass(resolvedMethod, it)
            }

    if (!isColorClass) return false

    return when (this.methodName) {
        "rgb",
        "argb" -> isBlackOrTransparentRgbCall(context, resolvedMethod)
        "hsv" -> isBlackOrTransparentHsvCall(context, resolvedMethod)
        // Ignore copy() calls as they are difficult to statically evaluate
        "copy" -> false
        else -> {
            // Fallback for wrapper constructor calls (e.g. RemoteColor(Color.Black))
            valueArguments.any { arg -> arg.isBlackOrTransparentColor(context) }
        }
    }
}

/**
 * Checks if the reference resolves to static black or transparent fields or property getters within
 * the Compose or legacy Android graphics APIs.
 */
private fun UReferenceExpression.isBlackOrTransparentReference(): Boolean {
    val resolvedSymbol = resolve() ?: return false
    val className = resolvedSymbol.getQualifiedClassName() ?: return false

    if (className !in WearWidgetBackgroundDetector.COLOR_CLASSES) return false

    val name = (resolvedSymbol as? PsiMember)?.name ?: return false
    return WearWidgetBackgroundDetector.INVALID_COLOR_NAMES.any {
        name.equals(it, ignoreCase = true)
    }
}

/**
 * Performs fallback checks against evaluated numeric constants. This catches raw literal values
 * that evaluate to opaque black or are fully transparent.
 */
private fun UExpression.isBlackOrTransparentConstant(context: JavaContext): Boolean {
    // Evaluate the expression and exit early if it's not a Number
    val evaluated = ConstantEvaluator.evaluate(context, this) ?: return false

    if (evaluated is Float || evaluated is Double) return false

    val masked = getMaskedColorLong(evaluated) ?: return false

    return masked == WearWidgetBackgroundDetector.OPAQUE_BLACK_MASK ||
        (masked ushr WearWidgetBackgroundDetector.ALPHA_SHIFT) == 0L
}

/**
 * Uses a bitmask to handle sign extension when converting negative Int values (like 0xFF000000) to
 * Long during evaluation.
 */
private fun getMaskedColorLong(evaluated: Any): Long? {
    val longValue =
        when (evaluated) {
            is Number -> evaluated.toLong()
            is UInt -> evaluated.toLong()
            is ULong -> evaluated.toLong()
            else -> return null
        }
    return longValue and WearWidgetBackgroundDetector.UINT_MASK
}

/**
 * Evaluates the arguments of a RemoteColor.rgb() builder call to determine if the resulting color
 * evaluates to opaque black or is fully transparent.
 */
private fun UCallExpression.isBlackOrTransparentRgbCall(
    context: JavaContext,
    resolvedMethod: PsiMethod,
): Boolean {
    val mapping = context.evaluator.computeArgumentMapping(this, resolvedMethod)

    val r = mapping.findArg("red").evaluateFloatArg(context)
    val g = mapping.findArg("green").evaluateFloatArg(context)
    val b = mapping.findArg("blue").evaluateFloatArg(context)
    val a = mapping.findArg("alpha").evaluateFloatArg(context, defaultValue = 1.0f)

    val isTransparent = a == 0f
    val isBlack = r == 0f && g == 0f && b == 0f && a == 1.0f
    return isTransparent || isBlack
}

/**
 * Evaluates the arguments of a RemoteColor.hsv() builder call to determine if the resulting color
 * evaluates to opaque black (zero brightness) or is fully transparent.
 */
private fun UCallExpression.isBlackOrTransparentHsvCall(
    context: JavaContext,
    resolvedMethod: PsiMethod,
): Boolean {
    val mapping = context.evaluator.computeArgumentMapping(this, resolvedMethod)

    val v = mapping.findArg("value").evaluateFloatArg(context)
    val a = mapping.findArg("alpha").evaluateFloatArg(context, defaultValue = 1.0f)

    val isTransparent = a == 0f
    val isBlack = v == 0f && a == 1.0f
    return isTransparent || isBlack
}

/** Helper to safely evaluate an argument to a Float, providing a fallback default if missing. */
private fun UExpression?.evaluateFloatArg(
    context: JavaContext,
    defaultValue: Float? = null,
): Float? =
    if (this == null) defaultValue
    else (ConstantEvaluator.evaluate(context, this) as? Number)?.toFloat()

/**
 * Resolves the qualified name of the class associated with this [PsiElement].
 *
 * If this element is a class, it returns its qualified name. If it is a class member (such as a
 * method, constructor, or field), it returns the qualified name of its containing class.
 */
private fun PsiElement?.getQualifiedClassName(): String? =
    when (this) {
        is PsiClass -> qualifiedName
        is PsiMember -> containingClass?.qualifiedName
        else -> null
    }

/**
 * Unwraps qualified reference expressions by extracting and returning their innermost selector
 * portion.
 */
private fun UExpression.unwrapQualified(): UExpression =
    if (this is UQualifiedReferenceExpression) selector else this

/**
 * Unwraps trailing `.rc` extension properties, `.rc()` conversions, or primitive type conversions
 * (like `.toInt()`) by extracting the underlying expression.
 *
 * Examples:
 * - `Color.Black.rc` unwraps to `Color.Black`
 * - `(0xFF000000).toInt()` unwraps to `0xFF000000`
 */
private fun UExpression.unwrapExtensions(): UExpression {
    val current = this.skipParenthesizedExprDown()

    // 1. Handle qualified references. In UAST, method calls on objects (e.g.,
    // 0xFF000000.toInt())
    // or property accesses (Color.Black.rc) are often represented as a qualified reference
    // where the right side (selector) is the method call or property name.
    if (current is UQualifiedReferenceExpression) {
        val selector = current.selector
        val selectorName =
            when (selector) {
                is UCallExpression -> selector.methodName
                is UReferenceExpression -> selector.resolvedName
                else -> null
            }
        if (selectorName in WearWidgetBackgroundDetector.UNWRAP_EXTENSION_NAMES) {
            return current.receiver
        }
    }

    // 2. Handle direct method calls. UAST can sometimes flatten qualified references
    // or represent Kotlin extension functions (like .toInt()) and properties (compiled
    // as Java getters like getRc()) directly as UCallExpressions.
    if (
        current is UCallExpression &&
            current.methodName in WearWidgetBackgroundDetector.UNWRAP_EXTENSION_NAMES
    ) {
        return current.valueArguments.firstOrNull() ?: current.receiver ?: current
    }
    return current
}

/**
 * Iteratively unwraps extensions, qualified references, and traces variables back to their
 * initializers until the core expression is found. This seamlessly unrolls nested structures
 * regardless of order.
 */
private fun UExpression.resolveAndUnwrap(): UExpression {
    var current = this
    var previous: UExpression? = null
    var visited: MutableSet<PsiElement>? = null

    var depth = 0
    while (current !== previous && depth < WearWidgetBackgroundDetector.MAX_UNWRAP_DEPTH) {
        depth++
        previous = current
        current = current.skipParenthesizedExprDown().unwrapExtensions().unwrapQualified()

        val resolved = (current as? UReferenceExpression)?.resolve()
        if (resolved != null) {
            if (visited == null) visited = mutableSetOf()

            // Track visited variables to completely prevent infinite recursion cycles
            if (visited.add(resolved)) {
                current = (resolved.toUElement() as? UVariable)?.uastInitializer ?: current
            }
        }
    }
    return current
}
