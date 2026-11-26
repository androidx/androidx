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

package androidx.glance.wear.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

/**
 * A [Detector] that warns when [WearWidgetDocument] specifies an empty or missing composable
 * `background` while `edgeToEdge` is not enabled.
 */
class WearWidgetBackgroundDetector : Detector(), Detector.UastScanner {

    companion object {
        @JvmField
        val EMPTY_BACKGROUND_ISSUE: Issue =
            Issue.create(
                id = "WearWidgetEmptyBackground",
                briefDescription = "Wear Widgets should specify a non-empty background",
                explanation =
                    """
                    Wear Widgets should explicitly specify a non-empty `background` composable within `WearWidgetDocument(...)` when `edgeToEdge` is not enabled.
                    If `background` is omitted or `null`, a default surface color will be applied. If an empty lambda `{}` is provided, no background will be drawn.
                    """,
                category = Category.CORRECTNESS,
                priority = 5,
                severity = Severity.WARNING,
                implementation =
                    Implementation(WearWidgetBackgroundDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )
    }
}
