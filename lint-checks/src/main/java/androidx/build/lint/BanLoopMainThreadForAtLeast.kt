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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

class BanLoopMainThreadForAtLeast : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("loopMainThreadForAtLeast")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.test.espresso.UiController")) {
            val incident =
                Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("Uses loopMainThreadForAtLeast()")
                    .scope(node)
            context.report(incident)
        }
    }

    companion object {
        val ISSUE =
            Issue.create(
                "BanLoopMainThreadForAtLeast",
                "Uses loopMainThreadForAtLeast() method",
                "Use of loopMainThreadForAtLeast() is not allowed, please use a callback " +
                    "or another way to make more reliable code. See more details at " +
                    "go/androidx/testability#calling-instrumentationwaitforidlesync-as-a-synchronization-barrier",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(
                    BanLoopMainThreadForAtLeast::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}
