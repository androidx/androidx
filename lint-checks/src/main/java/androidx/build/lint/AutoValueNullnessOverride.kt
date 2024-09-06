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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.getUMethod
import com.android.tools.lint.detector.api.isJava
import com.android.tools.lint.model.LintModelMavenName
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UClass

/**
 * Enforces that the workaround for b/237064488 is applied, see issue definition for more detail.
 */
class AutoValueNullnessOverride : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return ClassChecker(context)
    }

    private inner class ClassChecker(val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            if (!node.hasAnnotation("com.google.auto.value.AutoValue")) return

            val classCoordinates = context.findMavenCoordinate(node.javaPsi)

            // Narrow to the relevant methods
            val missingOverrides =
                node.allMethods.filter {
                    // Abstract getters are the ones used by the autovalue for builder generation
                    it.isAbstractGetter() &&
                        it.isNullable() &&
                        node.isSuperMethodWithoutOverride(it) &&
                        isFromDifferentCompilation(it, classCoordinates)
                }

            if (missingOverrides.isEmpty()) return

            // Add overrides that are just copies of the parent source code
            val insertionText =
                missingOverrides
                    .mapNotNull {
                        it.getUMethod()?.asSourceString()?.let { parentMethod ->
                            "\n@Override\n$parentMethod"
                        }
                    }
                    .joinToString("\n")
            val fix =
                if (isJava(node.language) && insertionText.isNotBlank()) {
                    fix()
                        .replace()
                        // Find the opening of the class body and insert after that
                        .pattern("\\{()")
                        .with(insertionText)
                        .reformat(true)
                        .shortenNames()
                        .range(context.getLocation(node, LocationType.ALL))
                        .build()
                } else {
                    null
                }

            val methodNames = missingOverrides.joinToString(", ") { "${it.name}()" }
            val incident =
                Incident(context)
                    .issue(ISSUE)
                    .message("Methods need @Nullable overrides for AutoValue: $methodNames")
                    .location(context.getNameLocation(node))
                    .fix(fix)

            context.report(incident)
        }

        private fun PsiMethod.isAbstractGetter() =
            parameterList.isEmpty && modifierList.hasModifierProperty("abstract")

        /** Checks if the method return type uses the JSpecify @Nullable. */
        private fun PsiMethod.isNullable() =
            returnType?.hasAnnotation("org.jspecify.annotations.Nullable") == true

        /**
         * Checks that the method is defined in a different class and that the method is not also
         * defined by a lower class in the hierarchy.
         */
        private fun UClass.isSuperMethodWithoutOverride(method: PsiMethod) =
            method.containingClass?.qualifiedName != qualifiedName &&
                // This searches starting with the class and then goes to the parent. So if it finds
                // a matching method that isn't this method, there's an override lower down.
                findMethodBySignature(method, true) == method

        /**
         * Checks if [member] has different maven coordinates than the [reference] coordinates, or
         * if this is in a test context. Tests are in a different compilation from the main source
         * but have the same maven coordinates, so to be safe always flag them.
         */
        private fun isFromDifferentCompilation(member: PsiMember, reference: LintModelMavenName?) =
            context.findMavenCoordinate(member.containingClass!!) != reference ||
                context.isTestSource
    }

    companion object {
        val ISSUE =
            Issue.create(
                "AutoValueNullnessOverride",
                "AutoValue classes must override @Nullable methods inherited from other projects",
                """
                    Due to a javac bug in JDK 21 and lower, AutoValue cannot see type-use nullness
                    annotations from other compilations. @AutoValue classes that inherit @Nullable
                    methods must provide an override so the AutoValue compiler doesn't make the
                    value non-null. See b/237064488 for more information.
                """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    AutoValueNullnessOverride::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
