/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lint.gradle

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/** Collection of Gradle lint check issues. */
class GradleIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API

    override val issues =
        listOf(
            DiscouragedGradleMethodDetector.EAGER_CONFIGURATION_ISSUE,
            DiscouragedGradleMethodDetector.PROJECT_ISOLATION_ISSUE,
            DiscouragedGradleMethodDetector.TO_STRING_ON_PROVIDER_ISSUE,
            DiscouragedGradleMethodDetector.PERFORMANCE_ISSUE,
            FilePropertyDetector.FILE_PROPERTY_ISSUE,
            InternalApiUsageDetector.INTERNAL_GRADLE_ISSUE,
            InternalApiUsageDetector.INTERNAL_AGP_ISSUE,
            InternalApiUsageDetector.INTERNAL_KGP_ISSUE,
            WithPluginClasspathUsageDetector.ISSUE,
            WithTypeWithoutConfigureEachUsageDetector.ISSUE,
        )

    override val vendor =
        Vendor(
            // TODO: Update component (or the issue template)
            feedbackUrl = "https://issuetracker.google.com/issues/new?component=1147525",
            identifier = "androidx.lint:lint-gradle",
            vendorName = "Android Open Source Project",
        )
}
