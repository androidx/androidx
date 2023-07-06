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

import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LayoutDetector
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class ImageViewTintDetector : LayoutDetector() {
    companion object {
        internal val USING_ANDROID_TINT: Issue = Issue.create(
            "UseAppTint",
            "`app:tint` attribute should be used on `ImageView` and `ImageButton`",
            "`ImageView` or `ImageButton` uses `android:tint` instead of `app:tint`",
            Category.CORRECTNESS,
            1,
            Severity.ERROR,
            Implementation(ImageViewTintDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
        )
    }

    override fun getApplicableElements(): Collection<String>? = listOf("ImageView", "ImageButton")

    override fun visitElement(context: XmlContext, element: Element) {
        // Only look at items that have android:tint
        if (!element.hasAttributeNS(SdkConstants.ANDROID_URI, "tint")) {
            return
        }
        // The suggested fix is a composite of setting app:tint to the existing value
        // of android:tint and unsetting android:tint
        context.report(
            USING_ANDROID_TINT,
            element,
            context.getLocation(element.getAttributeNodeNS(SdkConstants.ANDROID_URI, "tint")),
            "Must use `app:tint` instead of `android:tint`",
            LintFix.create().composite(
                LintFix.create().set(
                    SdkConstants.AUTO_URI, "tint",
                    element.getAttributeNS(SdkConstants.ANDROID_URI, "tint")
                ).build(),
                LintFix.create().unset(SdkConstants.ANDROID_URI, "tint").build()
            )
        )
    }
}
