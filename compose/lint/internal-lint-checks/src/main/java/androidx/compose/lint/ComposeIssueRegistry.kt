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

package androidx.compose.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class ComposeIssueRegistry : IssueRegistry() {
    override val minApi = CURRENT_API
    override val api = 11
    override val issues get(): List<Issue> {
        return listOf(
            ListIteratorDetector.ISSUE,
            ModifierInspectorInfoDetector.ISSUE,
            UnnecessaryLambdaCreationDetector.ISSUE,
        )
    }
    override val vendor = Vendor(
        vendorName = "Jetpack Compose",
        identifier = "compose:lint:internal-lint-checks",
        feedbackUrl = "https://issuetracker.google.com/issues/new?component=612128"
    )
}
