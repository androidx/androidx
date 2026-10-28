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

package androidx.compose.lint

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
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement

/**
 * Lint [Detector] that reports errors for usages of @RestrictTo. This should not be used in core
 * Compose libraries. In most cases the recommendation is to expose proper public APIs between
 * module boundaries instead. In some rare cases, @RequiresOptIn annotations may be used, but they
 * are not a 1:1 replacement for @RestrictTo and should be used with caution.
 *
 * Typedef annotation classes (annotated with @IntDef, @StringDef, or @LongDef) are exempt, as the
 * AndroidX API guidelines require these to be annotated with @RestrictTo.
 */
class BanRestrictToDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UAnnotation::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitAnnotation(node: UAnnotation) {
                if (node.qualifiedName != RestrictToFqn) return
                if (isOnTypedefAnnotationClass(node)) return
                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "`@RestrictTo` should not be used in core Compose libraries",
                )
            }
        }

    /**
     * Returns whether [annotation] is applied to a typedef annotation class, i.e. an annotation
     * class annotated with one of [TypedefFqns].
     */
    private fun isOnTypedefAnnotationClass(annotation: UAnnotation): Boolean {
        val annotatedClass = annotation.uastParent as? UClass ?: return false
        return annotatedClass.isAnnotationType &&
            annotatedClass.uAnnotations.any { it.qualifiedName in TypedefFqns }
    }

    companion object {
        private const val RestrictToFqn = "androidx.annotation.RestrictTo"

        /**
         * Typedef annotations. The AndroidX API guidelines require annotation classes annotated
         * with these to also be annotated with `@RestrictTo`.
         */
        private val TypedefFqns =
            setOf(
                "androidx.annotation.IntDef",
                "androidx.annotation.StringDef",
                "androidx.annotation.LongDef",
            )

        val ISSUE =
            Issue.create(
                id = "BanRestrictTo",
                briefDescription = "`@RestrictTo` should not be used in core Compose libraries",
                explanation =
                    "`@RestrictTo` should not be used in core Compose libraries. In most cases " +
                        "the recommendation is to expose proper public APIs between module " +
                        "boundaries instead. In some rare cases, `@RequiresOptIn` annotations " +
                        "may be used, but they are not a 1:1 replacement for `@RestrictTo` and " +
                        "should be used with caution.",
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        BanRestrictToDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE),
                    ),
            )
    }
}
