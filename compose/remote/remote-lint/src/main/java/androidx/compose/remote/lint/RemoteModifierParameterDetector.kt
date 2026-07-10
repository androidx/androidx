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

package androidx.compose.remote.lint

import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isComposable
import androidx.compose.lint.returnsUnit
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod

/**
 * [Detector] that checks Composable functions with RemoteModifier parameters for consistency with
 * guidelines of Remote Compose.
 *
 * For functions with one / more RemoteModifier parameters, the first RemoteModifier parameter must:
 * - Be named `modifier`
 * - Have a type of `RemoteModifier`
 * - Either have no default value, or have a default value of `RemoteModifier`
 * - If optional, be the first optional parameter in the parameter list
 */
class RemoteModifierParameterDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitMethod(node: UMethod) {
                // Ignore non-composable functions
                if (!node.isComposable) return

                // Ignore functions that are not annotated with @RemoteComposable
                if (!node.isRemoteComposable) return

                // Ignore non-unit composable functions
                if (!node.returnsUnit) return

                val modifierParameter =
                    node.uastParameters.firstOrNull { parameter ->
                        parameter.sourcePsi is KtParameter &&
                            parameter.type.inheritsFrom(
                                RemoteNames.CreationCompose.Modifier.RemoteModifier
                            )
                    } ?: return

                // Need to strongly type this or else Kotlinc cannot resolve overloads for
                // getNameLocation
                val modifierParameterElement: UElement = modifierParameter

                val source = modifierParameter.sourcePsi as KtParameter

                val modifierName = RemoteNames.CreationCompose.Modifier.RemoteModifier.shortName

                if (modifierParameter.name != RemoteModifierParameterName) {
                    context.report(
                        RemoteModifierParameter,
                        modifierParameterElement,
                        context.getNameLocation(modifierParameterElement),
                        "$modifierName parameter should be named $RemoteModifierParameterName",
                        LintFix.create()
                            .replace()
                            .name("Change name to $RemoteModifierParameterName")
                            .text(modifierParameter.name)
                            .with(RemoteModifierParameterName)
                            .autoFix()
                            .build(),
                    )
                }

                if (
                    modifierParameter.type.canonicalText !=
                        RemoteNames.CreationCompose.Modifier.RemoteModifier.javaFqn
                ) {
                    context.report(
                        RemoteModifierParameter,
                        modifierParameterElement,
                        context.getNameLocation(modifierParameterElement),
                        "$modifierName parameter should have a type of $modifierName",
                        LintFix.create()
                            .replace()
                            .range(context.getLocation(modifierParameterElement))
                            .name("Change type to $modifierName")
                            .text(source.typeReference!!.text)
                            .with(modifierName)
                            .autoFix()
                            .build(),
                    )
                }

                if (source.hasDefaultValue()) {
                    val defaultValue = source.defaultValue!!
                    // If the default value is not a reference expression, then it isn't
                    // RemoteModifier anyway and we can just report an error
                    val referenceExpression = source.defaultValue as? KtNameReferenceExpression
                    if (referenceExpression?.getReferencedName() != modifierName) {
                        context.report(
                            RemoteModifierParameter,
                            modifierParameterElement,
                            context.getNameLocation(modifierParameterElement),
                            "Optional $modifierName parameter should have a default value " +
                                "of `$modifierName`",
                            LintFix.create()
                                .replace()
                                .range(context.getLocation(modifierParameterElement))
                                .name("Change default value to $modifierName")
                                .text(defaultValue.text)
                                .with(modifierName)
                                .autoFix()
                                .build(),
                        )
                    }
                    val index = node.uastParameters.indexOf(modifierParameter)
                    val optionalParameterIndex =
                        node.uastParameters.indexOfFirst { parameter ->
                            (parameter.sourcePsi as? KtParameter)?.hasDefaultValue() == true
                        }
                    if (index != optionalParameterIndex) {
                        context.report(
                            RemoteModifierParameter,
                            modifierParameterElement,
                            context.getNameLocation(modifierParameterElement),
                            "$modifierName parameter should be the first optional parameter",
                            // Hard to make a lint fix for this and keep parameter formatting, so
                            // ignore it
                        )
                    }
                }
            }
        }

    companion object {
        val RemoteModifierParameter =
            Issue.create(
                "RemoteModifierParameter",
                "Guidelines for RemoteModifier parameters in a Remote Composable function",
                "The first (or only) RemoteModifier parameter in a Remote Composable function should follow the " +
                    "following rules:" +
                    "\n- Be named `$RemoteModifierParameterName`" +
                    "\n- Have a type of `${RemoteNames.CreationCompose.Modifier.RemoteModifier.shortName}`" +
                    "\n- Either have no default value, or have a default value of " +
                    "`${RemoteNames.CreationCompose.Modifier.RemoteModifier.shortName}`" +
                    "\n- If optional, be the first optional parameter in the parameter list",
                Category.CORRECTNESS,
                3,
                Severity.WARNING,
                Implementation(
                    RemoteModifierParameterDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}

private const val RemoteModifierParameterName = "modifier"
