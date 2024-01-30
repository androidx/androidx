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

package androidx.appcompat.app

import androidx.appcompat.BaseMethodDeprecationDetector
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

// Flags usage of Activity.setActionBar and suggests converting it to
// AppCompatActivity.setSupportActionBar
@Suppress("UnstableApiUsage")
class SetActionBarDetector : BaseMethodDeprecationDetector(
    USING_CORE_ACTION_BAR,
    // Suggest using AppCompatActivity.setSupportActionBar
    DeprecationCondition(
        MethodLocation("android.app.Activity", "setActionBar", "android.widget.Toolbar"),
        "Use `AppCompatActivity.setSupportActionBar`",
        SubClassOf("androidx.appcompat.app.AppCompatActivity")
    )
) {
    companion object {
        internal val USING_CORE_ACTION_BAR: Issue = Issue.create(
            "UseSupportActionBar",
            "Should not call `Activity.setActionBar` if you extend `AppCompatActivity`",
            "Use `AppCompatActivity.setSupportActionBar`",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(SetActionBarDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
