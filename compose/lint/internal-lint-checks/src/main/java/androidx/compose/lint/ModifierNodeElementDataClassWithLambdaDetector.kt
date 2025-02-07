/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.uast.UClass

/**
 * Lint [Detector] to warn for ModifierNodeElement implementations that have lambda properties and
 * use a data class to implement equality. This will use structural equality to compare lambdas,
 * which can cause issues for function references: see [LambdaStructuralEqualityDetector].
 */
class ModifierNodeElementDataClassWithLambdaDetector : Detector(), SourceCodeScanner {
    override fun applicableSuperClasses(): List<String> =
        listOf(Names.Ui.Node.ModifierNodeElement.javaFqn)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val ktClass = declaration.sourcePsi as? KtClass ?: return
        if (!ktClass.isData()) return
        val constructor = ktClass.primaryConstructor
        // Just warn on the first lambda, to avoid duplicate warnings
        val lambdaParameter =
            constructor?.valueParameters?.find { parameter ->
                parameter.typeReference?.let { typeReference ->
                    analyze(typeReference) { typeReference.type.isFunctionType }
                } == true
            }
        lambdaParameter?.let {
            context.report(
                ISSUE,
                lambdaParameter,
                context.getLocation(lambdaParameter),
                BriefDescription
            )
        }
    }

    companion object {
        private const val BriefDescription =
            "ModifierNodeElement implementations using a data class with lambda properties will result in incorrect equals implementations"
        private const val Explanation =
            "ModifierNodeElement implementations using a data class with lambda properties will " +
                "result in equals implementations that check the structural equality of lambdas. " +
                "Checking structural equality on lambdas can lead to issues, as function " +
                "references (::lambda) do not consider their capture scope in their equals " +
                "implementation. This means that structural equality can return true, even if " +
                "the lambdas are different references with a different capture scope. Instead " +
                "of using a data class to implement ModifierNodeElement with lambda properties, " +
                "you should instead manually implement equals / hashcode."

        val ISSUE =
            Issue.create(
                "ModifierNodeElementDataClassWithLambda",
                BriefDescription,
                Explanation,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    ModifierNodeElementDataClassWithLambdaDetector::class.java,
                    // Too noisy for tests, and unlikely to cause issues
                    EnumSet.of(Scope.JAVA_FILE)
                )
            )
    }
}
