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
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import java.util.EnumSet

class SpecifyJobSchedulerIdRangeIssueDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE = Issue.create(
            id = "SpecifyJobSchedulerIdRange",
            briefDescription = "Specify a range of JobScheduler ids",
            explanation = """
                When using `JobScheduler` APIs directly, `WorkManager` requires that developers \
                specify a range of `JobScheduler` ids that are safe for `WorkManager` to use \
                so the `id`s do not collide. \

                For more information look at \
                `androidx.work.Configuration.Builder.setJobSchedulerJobIdRange(int, int)`.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                SpecifyJobSchedulerIdRangeIssueDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )

        private val WELL_KNOWN_JOB_SERVICES = listOf(
            "android.app.job.JobService",
            "androidx.work.impl.background.systemjob.SystemJobService"
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("setJobSchedulerJobIdRange")
    }

    override fun applicableSuperClasses(): List<String> {
        return listOf("android.app.job.JobService")
    }

    private var hasOtherJobServices = false
    private var specifiesJobIdRange = false
    private var location: Location? = null

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (hasOtherJobServices) {
            return
        }

        val name = declaration.qualifiedName

        if (name !in WELL_KNOWN_JOB_SERVICES) {
            hasOtherJobServices = true
            // Keep track of location
            location = context.getLocation(declaration.javaPsi)
        }
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.evaluator.isMemberInClass(method, "androidx.work.Configuration.Builder")) {
            specifiesJobIdRange = true
        }
    }

    override fun afterCheckRootProject(context: Context) {
        if (hasOtherJobServices && !specifiesJobIdRange) {
            context.report(
                issue = ISSUE,
                location = location ?: Location.create(context.file),
                message = "Specify a valid range of job id's for `WorkManager` to use."
            )
        }
    }
}
