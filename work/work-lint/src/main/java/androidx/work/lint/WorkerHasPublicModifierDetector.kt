/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass

class WorkerHasPublicModifierDetector : Detector(), SourceCodeScanner {
    companion object {
        private const val DESCRIPTION =
            "ListenableWorkers constructed using the default WorkerFactories need to be public"

        val ISSUE = Issue.create(
            id = "WorkerHasAPublicModifier",
            briefDescription = DESCRIPTION,
            explanation = """
                When you define a ListenableWorker which is constructed using the 
                default WorkerFactory, the ListenableWorker sub-type needs to be public.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                WorkerHasPublicModifierDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }

    override fun getApplicableMethodNames(): List<String> = listOf("setWorkerFactory")

    override fun applicableSuperClasses() = listOf(
        "androidx.work.ListenableWorker"
    )

    private var hasCustomWorkerFactory = false
    private val workers = mutableListOf<Pair<UClass, Location>>()

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.work.Configuration.Builder")) {
            hasCustomWorkerFactory = true
        }
    }

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (declaration.hasModifier(JvmModifier.ABSTRACT) || declaration.isInterface) {
            // Exempt base types from analysis
            return
        }

        if (!declaration.hasModifier(JvmModifier.PUBLIC)) {
            workers += Pair(declaration, context.getNameLocation(declaration))
        }
    }

    override fun afterCheckRootProject(context: Context) {
        if (!hasCustomWorkerFactory && workers.isNotEmpty()) {
            for ((declaration, location) in workers) {
                context.report(
                    issue = ISSUE,
                    location = location,
                    message = "${declaration.qualifiedName} needs to be public"
                )
            }
        }
    }
}
