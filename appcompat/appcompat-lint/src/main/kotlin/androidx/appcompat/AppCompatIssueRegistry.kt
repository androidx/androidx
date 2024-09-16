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

package androidx.appcompat

import androidx.appcompat.app.SetActionBarDetector
import androidx.appcompat.res.ColorStateListAlphaDetector
import androidx.appcompat.res.ColorStateListLoadingDetector
import androidx.appcompat.res.DrawableLoadingDetector
import androidx.appcompat.res.ImageViewTintDetector
import androidx.appcompat.view.OnClickXmlDetector
import androidx.appcompat.widget.SwitchUsageCodeDetector
import androidx.appcompat.widget.SwitchUsageXmlDetector
import androidx.appcompat.widget.TextViewCompoundDrawablesApiDetector
import androidx.appcompat.widget.TextViewCompoundDrawablesXmlDetector
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

@Suppress("UnstableApiUsage")
class AppCompatIssueRegistry : IssueRegistry() {
    override val minApi = CURRENT_API
    override val api = 16
    override val issues
        get() =
            listOf(
                SetActionBarDetector.USING_CORE_ACTION_BAR,
                ColorStateListAlphaDetector.NOT_USING_ANDROID_ALPHA,
                ColorStateListLoadingDetector.NOT_USING_COMPAT_LOADING,
                DrawableLoadingDetector.NOT_USING_COMPAT_LOADING,
                ImageViewTintDetector.USING_ANDROID_TINT,
                SwitchUsageCodeDetector.USING_CORE_SWITCH_CODE,
                SwitchUsageXmlDetector.USING_CORE_SWITCH_XML,
                TextViewCompoundDrawablesApiDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_APIS,
                TextViewCompoundDrawablesXmlDetector.NOT_USING_COMPAT_TEXT_VIEW_DRAWABLE_ATTRS,
                OnClickXmlDetector.USING_ON_CLICK_IN_XML
            )

    override val vendor =
        Vendor(
            feedbackUrl = "https://issuetracker.google.com/issues/new?component=460343",
            identifier = "androidx.appcompat",
            vendorName = "Android Open Source Project",
        )
}
