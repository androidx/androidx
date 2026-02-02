/*
 * Copyright 2023 The Android Open Source Project
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
import org.jetbrains.uast.UCallExpression

class BanThreadSleep : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("sleep")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "java.lang.Thread")) {
            val incident =
                Incident(context)
                    .issue(ISSUE)
                    .location(context.getNameLocation(node))
                    .message("Uses Thread.sleep()")
                    .scope(node)
            context.report(incident)
        }
    }

    companion object {
        val ISSUE =
            Issue.create(
                "BanThreadSleep",
                "Uses Thread.sleep() method",
                "Use of Thread.sleep() is not allowed, please use a callback " +
                    "or another way to make more reliable code. See more details at " +
                    "go/androidx/testability#calling-threadsleep-as-a-synchronization-barrier",
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(BanThreadSleep::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }
}
