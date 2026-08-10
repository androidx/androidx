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

package androidx.navigation3.ui.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.uast.UClass

/**
 * Lint [Detector] to check that classes implementing Scene or OverlayScene are either a data class
 * or explicitly override equals and hashCode.
 */
class SceneEqualsHashCodeDetector : Detector(), SourceCodeScanner {

    override fun applicableSuperClasses(): List<String> =
        listOf("androidx.navigation3.scene.Scene", "androidx.navigation3.scene.OverlayScene")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val name = declaration.qualifiedName
        if (name in applicableSuperClasses()) {
            return
        }

        if (declaration.isInterface) {
            return
        }

        val ktClass = declaration.sourcePsi as? KtClass ?: return
        if (ktClass.isData()) return

        val overridesEquals =
            declaration.findMethodsByName("equals", false).any { method ->
                method.parameterList.parametersCount == 1
            }
        val overridesHashCode =
            declaration.findMethodsByName("hashCode", false).any { method ->
                method.parameterList.parametersCount == 0
            }

        if (!overridesEquals || !overridesHashCode) {
            context.report(
                ISSUE,
                declaration,
                context.getNameLocation(declaration),
                BRIEF_DESCRIPTION,
            )
        }
    }

    companion object {
        private const val BRIEF_DESCRIPTION =
            "Classes implementing `Scene` must either be a `data class` or explicitly override both `equals()` and `hashCode()`."
        private const val EXPLANATION =
            "Navigation transitions depend on stable `Scene` identities across recompositions in order to avoid unexpected transition behaviour. To preserve a `Scene`'s structural equality when reinstantiated across recompositions, you must either make this class a data class or explicitly override both `equals()` and `hashCode()`."
        val ISSUE =
            Issue.create(
                id = "SceneEqualsHashCode",
                briefDescription = BRIEF_DESCRIPTION,
                explanation = EXPLANATION,
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.WARNING,
                implementation =
                    Implementation(
                        SceneEqualsHashCodeDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
