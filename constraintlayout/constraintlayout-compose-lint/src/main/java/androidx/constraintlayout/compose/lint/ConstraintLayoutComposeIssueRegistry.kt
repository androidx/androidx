/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

private const val CL_COMPOSE_NEW_ISSUE = "new?component=323867&template=1023345"

class ConstraintLayoutComposeIssueRegistry : IssueRegistry() {
    override val api = 16

    override val minApi = CURRENT_API

    override val issues =
        listOf(
            ConstraintLayoutDslDetector.IncorrectReferencesDeclarationIssue,
            ConstraintLayoutDslDetector.IncorrectMatchParentUsageIssue,
            ConstraintLayoutDslDetector.IncorrectChainMarginsUsageIssue
        )

    override val vendor =
        Vendor(
            feedbackUrl = "https://issuetracker.google.com/issues/$CL_COMPOSE_NEW_ISSUE",
            identifier = "androidx.constraintlayout.compose",
            vendorName = "Android Open Source Project",
        )
}
