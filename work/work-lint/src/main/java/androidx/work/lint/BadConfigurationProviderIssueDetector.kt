/*
 * Copyright 2019 The Android Open Source Project
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
import org.jetbrains.uast.UClass

class BadConfigurationProviderIssueDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE = Issue.create(
            id = "BadConfigurationProvider",
            briefDescription = "Invalid WorkManager Configuration Provider",
            explanation = """
                An `android.app.Application` must implement `androidx.work.Configuration.Provider`
                for on-demand initialization.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                BadConfigurationProviderIssueDetector::class.java, Scope.JAVA_FILE_SCOPE
            )
        )
    }

    private var hasApplicableTypes = false
    private var correct = false
    private var location: Location? = null

    override fun applicableSuperClasses() = listOf(
        "android.app.Application",
        "androidx.work.Configuration.Provider"
    )

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (correct) {
            // Bail out early
            return
        }

        val name = declaration.qualifiedName
        if (name == "androidx.work.Configuration.Provider" || name == "android.app.Application") {
            // Exempt base types from analysis
            return
        }

        // Ignore abstract classes.
        if (context.evaluator.isAbstract(declaration)) {
            return
        }

        val isApplication = context.evaluator.inheritsFrom(
            declaration.javaPsi, "android.app.Application", true
        )

        val isProvider = context.evaluator.inheritsFrom(
            declaration.javaPsi, "androidx.work.Configuration.Provider", true
        )

        if (isApplication) {
            location = Location.create(context.file)
        }

        if (isProvider) {
            hasApplicableTypes = true
        }

        if (isApplication && isProvider) {
            correct = true
        }
    }

    override fun afterCheckRootProject(context: Context) {
        val location = location ?: return
        if (hasApplicableTypes && !correct) {
            context.report(
                issue = ISSUE,
                location = location,
                message = "Expected Application subtype to implement Configuration.Provider"
            )
        }
    }
}
