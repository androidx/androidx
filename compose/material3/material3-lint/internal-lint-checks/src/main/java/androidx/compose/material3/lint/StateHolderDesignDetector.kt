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
import java.util.EnumSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastVisibility
import org.jetbrains.uast.toUElement

/**
 * [Detector] that verifies state holder classes (Colors, Elevation, State) follow the design
 * guidelines:
 * 1. Annotated with @Stable or @Immutable.
 * 2. Provide a public constructor or a public copy() function.
 */
class StateHolderDesignDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        val packageName = context.uastFile?.packageName ?: return UElementHandler.NONE
        if (!packageName.startsWith("androidx.compose.material3")) return UElementHandler.NONE

        return object : UElementHandler() {
            override fun visitClass(node: UClass) {
                val name = node.name ?: return
                if (
                    !name.endsWith("Colors") &&
                        !name.endsWith("Elevation") &&
                        !name.endsWith("State")
                )
                    return

                // Target public classes
                val ktClass = node.sourcePsi as? KtClassOrObject ?: return
                val isPublic =
                    !ktClass.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
                        !ktClass.hasModifier(KtTokens.INTERNAL_KEYWORD) &&
                        !ktClass.hasModifier(KtTokens.PROTECTED_KEYWORD)
                if (!isPublic) return

                // 1. Check stability annotation
                val hasStable = node.hasAnnotation("androidx.compose.runtime.Stable")
                val hasImmutable = node.hasAnnotation("androidx.compose.runtime.Immutable")
                if (!hasStable && !hasImmutable) {
                    val fixStable =
                        fix()
                            .name("Annotate with @Stable")
                            .annotate(
                                source = "@androidx.compose.runtime.Stable",
                                context = context,
                                element = node.sourcePsi,
                                replace = false,
                            )
                            .build()
                    val fixImmutable =
                        fix()
                            .name("Annotate with @Immutable")
                            .annotate(
                                source = "@androidx.compose.runtime.Immutable",
                                context = context,
                                element = node.sourcePsi,
                                replace = false,
                            )
                            .build()
                    val compositeFix =
                        fix().name("Add stability annotation").alternatives(fixStable, fixImmutable)

                    context.report(
                        STABILITY_ISSUE,
                        node as UElement,
                        context.getNameLocation(node),
                        "State holder class '$name' should be annotated with @Stable or @Immutable",
                        compositeFix,
                    )
                }

                // 2. Check constructor or copy()
                if (!node.isInterface && !name.endsWith("Elevation")) {
                    val hasPublicConstructor =
                        node.constructors.any { constructor ->
                            val uConstructor = constructor.toUElement() as? UMethod
                            uConstructor != null &&
                                uConstructor.visibility == UastVisibility.PUBLIC &&
                                !isInternalKotlinMember(uConstructor)
                        }

                    val hasPublicCopy =
                        node.methods.any { method ->
                            val uMethod = method.toUElement() as? UMethod
                            uMethod != null &&
                                method.name == "copy" &&
                                uMethod.visibility == UastVisibility.PUBLIC &&
                                !isInternalKotlinMember(uMethod)
                        }

                    val hasConstructorFn =
                        hasPublicConstructorFunction(context, context.uastFile, name)

                    if (!hasPublicConstructor && !hasPublicCopy && !hasConstructorFn) {
                        context.report(
                            CONSTRUCTOR_ISSUE,
                            node as UElement,
                            context.getNameLocation(node),
                            "State holder class '$name' must provide a public constructor, a public copy() function, or a public non-composable factory function to allow creation/modification outside of composables",
                        )
                    }
                }
            }
        }
    }

    private fun isInternalKotlinMember(uMethod: UMethod): Boolean {
        val sourcePsi = uMethod.sourcePsi as? KtDeclaration ?: return false
        return sourcePsi.hasModifier(KtTokens.INTERNAL_KEYWORD)
    }

    private fun hasPublicConstructorFunction(
        context: JavaContext,
        uFile: UFile?,
        className: String,
    ): Boolean {
        if (uFile == null) return false
        for (clazz in uFile.classes) {
            for (method in clazz.methods) {
                val uMethod = method.toUElement() as? UMethod ?: continue
                if (
                    uMethod.name == className &&
                        !uMethod.isConstructor &&
                        uMethod.visibility == UastVisibility.PUBLIC &&
                        !isInternalKotlinMember(uMethod) &&
                        !isComposableFunction(uMethod)
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun isComposableFunction(uMethod: UMethod): Boolean {
        return uMethod.annotations.any { annotation ->
            annotation.qualifiedName == "androidx.compose.runtime.Composable"
        }
    }

    companion object {
        val STABILITY_ISSUE =
            Issue.create(
                id = "StateHolderMissingStabilityAnnotation",
                briefDescription = "State holder missing stability annotation",
                explanation =
                    """
                    Classes representing state, colors, or elevation (e.g., `*State`, `*Colors`, `*Elevation`)
                    must be annotated with `@Stable` or `@Immutable` to allow the Compose compiler to
                    optimize recompositions when these objects are passed as parameters.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        StateHolderDesignDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        val CONSTRUCTOR_ISSUE =
            Issue.create(
                id = "StateHolderMissingConstructorOrCopy",
                briefDescription = "State holder missing public constructor or copy()",
                explanation =
                    """
                    State holder classes must be constructable or copyable outside of a `@Composable` context
                    to enable testing and usage in previews. Provide at least one public constructor or a
                    public `copy(...)` function.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        StateHolderDesignDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
