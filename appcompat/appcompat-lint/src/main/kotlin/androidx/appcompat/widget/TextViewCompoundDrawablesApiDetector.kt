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

package androidx.appcompat.widget

import androidx.appcompat.BaseMethodDeprecationDetector
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

// Flags usage of TextView.setCompoundDrawableTintList and TextView.setCompoundDrawableTintMode and
// suggests converting them to either TextViewCompat.setCompoundDrawableTintList or
// TextViewCompat.setCompoundDrawableTintMode
@Suppress("UnstableApiUsage")
class TextViewCompoundDrawablesApiDetector : BaseMethodDeprecationDetector(
    NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_APIS,
    // Suggest using TextViewCompat.setCompoundDrawableTintList instead of
    // TextView.setCompoundDrawableTintList
    DeprecationCondition(
        MethodLocation(
            "android.widget.TextView", "setCompoundDrawableTintList",
            "android.content.res.ColorStateList"
        ),
        "Use `TextViewCompat.setCompoundDrawableTintList()`"
    ),
    // Suggest using TextViewCompat.setCompoundDrawableTintMode instead of
    // TextView.setCompoundDrawableTintMode
    DeprecationCondition(
        MethodLocation(
            "android.widget.TextView", "setCompoundDrawableTintMode",
            "android.graphics.PorterDuff.Mode"
        ),
        "Use `TextViewCompat.setCompoundDrawableTintMode()`"
    )
) {
    companion object {
        internal val NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_APIS: Issue = Issue.create(
            "UseCompatTextViewDrawableApis",
            "Should not call `TextView.setCompoundDrawableTintList` or" +
                " `TextView.setCompoundDrawableTintMode` directly",
            "Use Compat loading of compound text view drawables",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(TextViewCompoundDrawablesApiDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
