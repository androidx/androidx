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

package androidx.wear.protolayout.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/** Issue Registry containing ProtoLayout specific lint Issues. */
@Suppress("UnstableApiUsage")
class ProtoLayoutIssueRegistry : IssueRegistry() {
    override val api = 16
    override val minApi = CURRENT_API
    override val issues =
        listOf(
            ProtoLayoutMinSchemaDetector.ISSUE,
            ResponsiveLayoutDetector.PRIMARY_LAYOUT_ISSUE,
            ResponsiveLayoutDetector.EDGE_CONTENT_LAYOUT_ISSUE
        )
    override val vendor =
        Vendor(
            feedbackUrl = "https://issuetracker.google.com/issues/new?component=1112273",
            identifier = "androidx.wear.protolayout",
            vendorName = "Android Open Source Project",
        )
}
