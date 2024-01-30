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

package androidx.appcompat.res

import androidx.appcompat.BaseMethodDeprecationDetector
import com.android.tools.lint.client.api.TYPE_INT
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

// Flags usage of Context.getDrawable and Resources.getDrawable and suggests converting them
// to either AppCompatResources.getDrawable or ResourcesCompat.getDrawable
@Suppress("UnstableApiUsage")
class DrawableLoadingDetector : BaseMethodDeprecationDetector(
    NOT_USING_COMPAT_LOADING,
    // Suggest using ContextCompat.getDrawable
    DeprecationCondition(
        MethodLocation("android.content.Context", "getDrawable", TYPE_INT),
        "Use `AppCompatResources.getDrawable()`"
    ),
    // Suggest using ResourcesCompat.getDrawable for one-parameter Resources.getDrawable calls
    DeprecationCondition(
        MethodLocation("android.content.res.Resources", "getDrawable", TYPE_INT),
        "Use `ResourcesCompat.getDrawable()`"
    ),
    // Suggest using ResourcesCompat.getDrawable for two-parameter Resources.getDrawable calls
    DeprecationCondition(
        MethodLocation(
            "android.content.res.Resources", "getDrawable", TYPE_INT,
            "android.content.res.Resources.Theme"
        ),
        "Use `ResourcesCompat.getDrawable()`"
    )
) {
    companion object {
        internal val NOT_USING_COMPAT_LOADING: Issue = Issue.create(
            "UseCompatLoadingForDrawables",
            "Should not call `Context.getDrawable` or `Resources.getDrawable` directly",
            "Use Compat loading of drawables",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(DrawableLoadingDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
