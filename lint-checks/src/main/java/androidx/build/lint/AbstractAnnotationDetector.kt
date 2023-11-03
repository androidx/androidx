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

package androidx.build.lint

import com.android.tools.lint.checks.ResourceTypeDetector.Companion.RESOURCE_TYPE
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UElement

/**
 * Copied from com/android/tools/lint/checks/AbstractAnnotationDetector.kt in Android Studio repo.
 */
abstract class AbstractAnnotationDetector : Detector(), SourceCodeScanner {
    protected fun report(
        context: JavaContext,
        issue: Issue,
        scope: UElement?,
        location: Location,
        message: String
    ) {
        report(context, issue, scope, location, message, null)
    }

    protected fun report(
        context: JavaContext,
        issue: Issue,
        scope: UElement?,
        location: Location,
        message: String,
        quickfixData: LintFix?
    ) {
        // In the IDE historically (until 2.0) many checks were covered by the
        // ResourceTypeInspection, and when suppressed, these would all be suppressed with the
        // id "ResourceType".
        //
        // Since then I've split this up into multiple separate issues, but we still want
        // to honor the older suppress id, so explicitly check for it here:
        val driver = context.driver
        if (issue !== RESOURCE_TYPE) {
            if (scope != null && driver.isSuppressed(context, RESOURCE_TYPE, scope)) {
                return
            }
        }

        context.report(issue, scope, location, message, quickfixData)
    }
}
