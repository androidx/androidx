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

package androidx.compose.material3.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement

/**
 * [Detector] that checks the ordering of parameters in Composable functions according to the
 * Compose Material 3 API guidelines.
 */
class ComposableParameterOrderingDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        if (context.isTestSource) return UElementHandler.NONE
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (
            !packageName.startsWith("androidx.compose.material3") || packageName.contains("samples")
        )
            return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                if (!node.hasAnnotation("androidx.compose.runtime.Composable")) return
                if (
                    node.hasAnnotation("kotlin.Deprecated") ||
                        node.hasAnnotation("java.lang.Deprecated")
                )
                    return

                // Check if this method has a deprecated sibling overload (same name) and check if
                // parameters are preserved
                var siblingNonTrailingSize = 0
                val containingClass = node.getContainingUClass()
                if (containingClass != null) {
                    val siblings =
                        containingClass.methods.filter {
                            it != node.javaPsi && it.name == node.name
                        }
                    val deprecatedSibling = siblings.firstNotNullOfOrNull { sibling ->
                        val uSibling = sibling.toUElement() as? UMethod
                        val isDeprecated =
                            uSibling != null &&
                                (uSibling.hasAnnotation("kotlin.Deprecated") ||
                                    uSibling.hasAnnotation("java.lang.Deprecated"))
                        if (isDeprecated) uSibling else null
                    }
                    if (deprecatedSibling != null) {
                        val siblingParams = deprecatedSibling.uastParameters
                        val siblingNonTrailing = siblingParams.filter { !isTrailingSlot(it) }
                        val activeParams = node.uastParameters
                        val isPreserved =
                            siblingNonTrailing.indices.all { i ->
                                i < activeParams.size &&
                                    activeParams[i].name == siblingNonTrailing[i].name
                            }
                        if (isPreserved) {
                            siblingNonTrailingSize = siblingNonTrailing.size
                        }
                    }
                }

                // Ignore non-public APIs
                val sourcePsi = node.sourcePsi
                if (sourcePsi is KtModifierListOwner) {
                    val isPrivateOrInternal =
                        sourcePsi.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                            sourcePsi.hasModifier(KtTokens.INTERNAL_KEYWORD)
                    if (isPrivateOrInternal) return
                }
                if (containingClass != null) {
                    val classSourcePsi = containingClass.sourcePsi
                    if (classSourcePsi is KtModifierListOwner) {
                        val isClassPrivateOrInternal =
                            classSourcePsi.hasModifier(KtTokens.PRIVATE_KEYWORD) ||
                                classSourcePsi.hasModifier(KtTokens.INTERNAL_KEYWORD)
                        if (isClassPrivateOrInternal) return
                    }
                }

                val parameters = node.uastParameters
                if (parameters.isEmpty()) return

                var previousTier = ParameterTier.REQUIRED_INPUT
                var previousParamName = ""
                var previousIndex = -1
                var hasSeenStateParam = false

                parameters.forEachIndexed { index, parameter ->
                    val isLast = index == parameters.lastIndex
                    val currentTier = getParameterTier(parameter, isLast, hasSeenStateParam)
                    if (parameter.name.equals("state", ignoreCase = true)) {
                        hasSeenStateParam = true
                    }

                    if (currentTier.rank < previousTier.rank) {
                        val isJustified =
                            previousIndex != -1 && previousIndex < siblingNonTrailingSize
                        if (!isJustified) {
                            context.report(
                                ISSUE,
                                parameter as UElement,
                                context.getNameLocation(parameter),
                                "Parameter '$previousParamName' (tier ${previousTier.name}) should come after '${parameter.name}' (tier ${currentTier.name})",
                            )
                        }
                    }

                    // Special check for modifier: must be the first optional parameter
                    // If current is modifier, and previous was optional (tier > 1) and not modifier
                    // itself.
                    if (
                        currentTier == ParameterTier.MODIFIER &&
                            previousTier.rank > ParameterTier.REQUIRED_INPUT.rank &&
                            previousTier != ParameterTier.MODIFIER
                    ) {
                        context.report(
                            ISSUE,
                            parameter as UElement,
                            context.getNameLocation(parameter),
                            "Modifier parameter should be the first optional parameter",
                        )
                    }

                    // Update tracking variables only if we don't have out of order to prevent
                    // cascade?
                    // Actually, standard is to update, so we report relative to previous.
                    previousTier = currentTier
                    previousParamName = parameter.name
                    previousIndex = index
                }
            }
        }
    }

    private fun getParameterTier(
        parameter: UParameter,
        isLast: Boolean,
        hasSeenStateParam: Boolean,
    ): ParameterTier {
        val ktParameter = parameter.sourcePsi as? KtParameter
        val name = parameter.name
        val lowerName = name.lowercase()
        val type = parameter.type
        val typeFqn = type.canonicalText

        // 1. Modifier (Tier 2)
        if (lowerName == "modifier" && typeFqn == "androidx.compose.ui.Modifier") {
            return ParameterTier.MODIFIER
        }

        // 2. InteractionSource or Trailing Slot (Tier 7)
        if (
            lowerName == "interactionsource" &&
                typeFqn == "androidx.compose.foundation.interaction.MutableInteractionSource"
        ) {
            return ParameterTier.INTERACTION_TRAILING
        }
        if (isLast && isFunctionType(type) && !lowerName.startsWith("on")) {
            return ParameterTier.INTERACTION_TRAILING
        }

        // 3. Required inputs & callbacks (Tier 1)
        val hasDefault = ktParameter?.hasDefaultValue() ?: false
        if (!hasDefault) {
            return ParameterTier.REQUIRED_INPUT
        }

        // 4. State controllers (Tier 3)
        val isState = lowerName.endsWith("state") || typeFqn.endsWith("State")
        if (isState) {
            if (hasSeenStateParam) {
                return ParameterTier.AUXILIARY_BEHAVIOR
            }
            return ParameterTier.STATE_CONTROLLER
        }

        // 5. Primary status flags (Tier 4)
        val primaryStatusFlags = listOf("enabled", "checked", "selected", "expanded")
        if (lowerName in primaryStatusFlags) {
            return ParameterTier.PRIMARY_BEHAVIOR_FLAG
        }

        // 6. Visual styling & layout (Tier 6)
        val stylingNames =
            listOf("shape", "colors", "elevation", "border", "windowinsets", "textstyle", "style")
        val isStyling =
            lowerName in stylingNames ||
                lowerName.endsWith("shape") ||
                lowerName.endsWith("color") ||
                lowerName.endsWith("colors") ||
                lowerName.endsWith("elevation") ||
                lowerName.contains("border") ||
                lowerName.endsWith("padding") ||
                lowerName.endsWith("spacing") ||
                lowerName.startsWith("contentpadding") ||
                lowerName.endsWith("arrangement") ||
                lowerName.endsWith("alignment") ||
                lowerName.endsWith("height") ||
                lowerName.endsWith("width") ||
                typeFqn.contains("BorderStroke", ignoreCase = true) ||
                typeFqn.contains("Arrangement") ||
                typeFqn.contains("Alignment") ||
                typeFqn.contains("WindowInsets")
        if (isStyling) {
            return ParameterTier.VISUAL_STYLING
        }

        // 6.5 Auxiliary behavior modifiers (Tier 7)
        if (
            lowerName.endsWith("behavior") ||
                typeFqn.endsWith("Behavior") ||
                typeFqn.contains("Behavior")
        ) {
            return ParameterTier.AUXILIARY_BEHAVIOR
        }

        // 7. Platform window configuration (Tier 7)
        if (
            lowerName == "properties" ||
                lowerName.endsWith("properties") ||
                typeFqn.endsWith("Properties") ||
                typeFqn.contains("Properties")
        ) {
            return ParameterTier.PLATFORM_WINDOW_CONFIG
        }

        // 8. Slots & component configs (Tier 5) - Catch-all for other optional params
        return ParameterTier.SLOT_CONFIG
    }

    private fun isFunctionType(type: PsiType): Boolean {
        val canonicalText = type.canonicalText
        return canonicalText.startsWith("kotlin.jvm.functions.Function") ||
            canonicalText.contains("Function")
    }

    private fun isTrailingSlot(parameter: UParameter): Boolean {
        val name = parameter.name
        if (name == "content" || name == "interactionSource") return true
        val type = parameter.type
        if (isFunctionType(type)) return true
        return false
    }

    enum class ParameterTier(val rank: Int, val description: String) {
        REQUIRED_INPUT(1, "Required inputs & callbacks"),
        MODIFIER(2, "Modifier"),
        STATE_CONTROLLER(3, "State controllers"),
        PRIMARY_BEHAVIOR_FLAG(4, "Primary status flags"),
        SLOT_CONFIG(5, "Slots & component configs"),
        VISUAL_STYLING(6, "Visual styling & layout"),
        AUXILIARY_BEHAVIOR(6, "Auxiliary behavior modifiers"),
        PLATFORM_WINDOW_CONFIG(7, "Platform window configuration"),
        INTERACTION_TRAILING(8, "Interaction & Trailing slot"),
    }

    companion object {
        val ISSUE =
            Issue.create(
                id = "ComposableParameterOrdering",
                briefDescription = "Composable parameters are out of order",
                explanation =
                    """
                    Composable parameters should be ordered according to the Compose Material 3 API guidelines:
                    1. Required inputs & callbacks (e.g. value, onClick)
                    2. Modifier (modifier: Modifier = Modifier) - first optional parameter
                    3. State controllers (e.g. state: SliderState)
                    4. Primary status flags (e.g. enabled, checked, selected, expanded)
                    5. Slots & component configs (e.g. title, valueRange)
                    6. Visual styling & layout / Auxiliary behavior modifiers (e.g. shape, colors, scrollBehavior)
                    7. Platform window configuration (e.g. properties: DialogProperties)
                    8. InteractionSource & Trailing slot (e.g. interactionSource, content)

                    For API evolutions where parameter ordering must remain suboptimal to maintain binary
                    compatibility with previous versions, use `@Suppress("ComposableParameterOrdering")`
                    on the Composable function.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        ComposableParameterOrderingDetector::class.java,
                        Scope.JAVA_FILE_SCOPE,
                    ),
            )
    }
}
